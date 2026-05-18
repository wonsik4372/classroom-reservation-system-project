package com.crsystem.systemserver.service;

import com.crsystem.common.model.Classroom;
import com.crsystem.common.model.ClassroomInfo;
import com.crsystem.common.model.Reservation;
import com.crsystem.common.model.ScheduleData;
import com.crsystem.common.model.DayReservation;
import com.crsystem.common.model.DayReservation.SingleBooking;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

public class ClassroomService {
    private final String masterFilePath;
    private final String dbDirectoryPath = "src/main/resources/reservations/"; // 날짜별 파일 저장 폴더
    private final ObjectMapper mapper;
    private ScheduleData masterData;

    public ClassroomService(String masterFilePath) {
        this.masterFilePath = masterFilePath;
        this.mapper = new ObjectMapper();
        
        // 날짜별 예약 파일 보관 폴더 자동 생성
        File dir = new File(dbDirectoryPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        loadMasterData();
    }

    // 1. 마스터 템플릿(정규 시간표 및 강의실 정보) 로드
    private void loadMasterData() {
        try {
            File file = new File(masterFilePath);
            if (file.exists() && file.length() > 0) {
                this.masterData = mapper.readValue(file, ScheduleData.class);
            } else {
                this.masterData = new ScheduleData();
            }
        } catch (IOException e) {
            System.err.println("❌ 마스터 데이터 로드 실패: " + e.getMessage());
            this.masterData = new ScheduleData();
        }
    }

    // 2. 마스터 데이터 저장 (강의실 정보 수정 시에만 호출됨)
    private synchronized void saveMasterData() {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(masterFilePath), "UTF-8"))) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, this.masterData);
            System.out.println("💾 마스터 데이터베이스(JSON) 업데이트 성공.");
        } catch (IOException e) {
            System.err.println("❌ 마스터 데이터 저장 실패: " + e.getMessage());
        }
    }

    // 3. 특정 날짜의 예약 파일 로드 헬퍼 (예: reservation_2026-05-24.json)
    private DayReservation loadDayReservation(String dateStr) {
        File file = new File(dbDirectoryPath + "reservation_" + dateStr + ".json");
        if (file.exists() && file.length() > 0) {
            try {
                return mapper.readValue(file, DayReservation.class);
            } catch (IOException e) {
                System.err.println("⚠️ 날짜 예약 파일 로드 실패, 새로 생성합니다.");
            }
        }
        return new DayReservation(dateStr); // 파일이 없으면 새 데이터 구조 리턴
    }

    // 4. 특정 날짜의 예약 파일 저장 헬퍼
    private void saveDayReservation(DayReservation dayRes) {
        String dateStr = dayRes.getDate();
        File file = new File(dbDirectoryPath + "reservation_" + dateStr + ".json");
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, dayRes);
            System.out.println("💾 날짜별 예약 파일 저장 성공: " + file.getName());
        } catch (IOException e) {
            System.err.println("❌ 날짜별 예약 파일 저장 실패: " + e.getMessage());
        }
    }

    // [기능 A] 강의실 정적 정보 수정 (오직 마스터 파일에만 영향)
    public boolean updateClassroomInfo(String classroomName, int capacity, String features, int computerCount, String status) {
        Classroom classroom = masterData.getClassrooms().get(classroomName);
        if (classroom == null) return false;

        ClassroomInfo info = classroom.getInfo();
        info.setCapacity(capacity);
        info.setFeatures(features);
        info.setComputerCount(computerCount);
        info.setStatus(status);

        saveMasterData(); // 마스터 템플릿만 영구 저장
        return true;
    }

    // [기능 B] 날짜 지정 예약 등록 (2주 뒤 예약 완벽 방어)
    public boolean addReservation(String classroomName, String dateStr, String timeSlot, String purpose, String requesterName, String requesterType) {
        Classroom classroom = masterData.getClassrooms().get(classroomName);
        if (classroom == null) {
            System.out.println("⚠️ 존재하지 않는 강의실입니다: " + classroomName);
            return false;
        }

        // 1) 날짜 포맷 분석 및 요일 구하기 (예: "2026-05-24" -> "일")
        String dayOfWeek;
        try {
            LocalDate date = LocalDate.parse(dateStr);
            dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN); // "월", "화" 등
        } catch (Exception e) {
            System.out.println("⚠️ 날짜 포맷이 잘못되었습니다. (YYYY-MM-DD 형식을 지켜주세요)");
            return false;
        }

        // 2) [1차 검증] 마스터 템플릿 검사 (정규 수업 'status 1'과 겹치는지 체크)
        List<Reservation> regularSchedule = classroom.getSchedule().get(dayOfWeek);
        if (regularSchedule != null) {
            for (Reservation regular : regularSchedule) {
                if (regular.getStatus() == 1 && isTimeOverlapping(regular.getTime(), timeSlot)) {
                    System.out.println("❌ 예약 실패: 해당 시간에는 정규 수업 [" + regular.getSubject() + "] 이(가) 있습니다.");
                    return false;
                }
            }
        }

        // 3) [2차 검증] 날짜별 예약 전용 파일 로드 후 기존 예약(status 2, 3)과 겹치는지 검사
        DayReservation dayRes = loadDayReservation(dateStr);
        for (SingleBooking booking : dayRes.getBookings()) {
            if (booking.getClassroomName().equals(classroomName) && isTimeOverlapping(booking.getTime(), timeSlot)) {
                String typeStr = booking.getStatus() == 2 ? "학생 예약" : "교수 예약";
                System.out.println("❌ 예약 실패: 이미 다른 [" + typeStr + " - " + booking.getSubject() + " (" + booking.getTime() + ")]이 예약되어 있습니다.");
                return false;
            }
        }

        // 4) 검증 완료되면 예약 내역 추가
        int statusCode = "professor".equalsIgnoreCase(requesterType) ? 3 : 2;
        dayRes.getBookings().add(new SingleBooking(classroomName, timeSlot, purpose, requesterName, statusCode));
        
        // 시간순 정렬
        dayRes.getBookings().sort(Comparator.comparing(SingleBooking::getTime));

        // 날짜 파일에 물리적 저장 (마스터 파일은 오염되지 않음!)
        saveDayReservation(dayRes);
        System.out.println("🎉 예약 성공: [" + dateStr + " (" + dayOfWeek + ") " + classroomName + "] " + timeSlot);
        return true;
    }

    // 시간 충돌 판정 알고리즘
    private boolean isTimeOverlapping(String timeRange1, String timeRange2) {
        try {
            String[] t1 = timeRange1.split("-");
            String[] t2 = timeRange2.split("-");

            int start1 = convertTimeToMinutes(t1[0]);
            int end1 = convertTimeToMinutes(t1[1]);
            int start2 = convertTimeToMinutes(t2[0]);
            int end2 = convertTimeToMinutes(t2[1]);

            return start1 < end2 && start2 < end1;
        } catch (Exception e) {
            return true; 
        }
    }
    public List<Reservation> getMergedSchedule(String classroomName, String dateStr) {
    Classroom classroom = masterData.getClassrooms().get(classroomName);
    if (classroom == null) {
        return new ArrayList<>();
    }

    // 1. 해당 날짜의 요일 알아내기 (예: "2026-05-24" -> "일")
    String dayOfWeek;
    try {
        LocalDate date = LocalDate.parse(dateStr);
        dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
    } catch (Exception e) {
        System.err.println("⚠️ 날짜 포맷 분석 실패: " + e.getMessage());
        return new ArrayList<>();
    }

    // 2. 마스터 파일에서 해당 요일의 기본 시간표(수업 1, 빈시간 0) 복사본 생성 (깊은 복사)
    List<Reservation> masterDaySchedule = classroom.getSchedule().get(dayOfWeek);
    List<Reservation> mergedList = new ArrayList<>();
    
    if (masterDaySchedule != null) {
        for (Reservation r : masterDaySchedule) {
            mergedList.add(new Reservation(r.getTime(), r.getSubject(), r.getProfessor(), r.getStatus()));
        }
    }

    // 3. 해당 날짜의 예약 파일(reservation_YYYY-MM-DD.json) 로드
    DayReservation dayRes = loadDayReservation(dateStr);

    // 4. 마스터 시간표(mergedList)의 빈 시간(0번) 공간에 실제 예약 정보(2번, 3번) 덮어쓰기
    for (SingleBooking booking : dayRes.getBookings()) {
        if (booking.getClassroomName().equals(classroomName)) {
            for (Reservation res : mergedList) {
                // 동일 시간대이면서 기존 마스터가 '빈 시간(status 0)'인 자리에만 예약을 채워 넣음
                if (res.getTime().equals(booking.getTime()) && res.getStatus() == 0) {
                    res.setSubject(booking.getSubject());
                    res.setProfessor(booking.getProfessor());
                    res.setStatus(booking.getStatus()); // 2(학생) 또는 3(교수)으로 상태 갱신
                    break;
                }
            }
        }
    }

    return mergedList;
    }
    
    private int convertTimeToMinutes(String timeStr) {
        String[] parts = timeStr.trim().split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    public ScheduleData getScheduleData() {
        return this.masterData;
    }
}
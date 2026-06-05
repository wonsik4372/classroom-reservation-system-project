package com.crsystem.systemserver.service;

import com.crsystem.common.dto.Classroom;
import com.crsystem.common.dto.ClassroomInfo;
import com.crsystem.common.dto.Reservation;
import com.crsystem.common.dto.ScheduleData;
import com.crsystem.common.dto.DayReservation;
import com.crsystem.common.dto.DayReservation.SingleBooking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

public class ClassroomHandler {
    private final String masterDirPath;
    private final String dbDirectoryPath = "data/reservations/";
    private final ObjectMapper mapper;

    // 모든 건물의 강의실을 하나로 합친 마스터 데이터
    private ScheduleData masterData;
    // 강의실명 -> 해당 건물 JSON 파일 (저장 시 어느 파일에 써야 하는지 추적)
    private final Map<String, File> classroomToFile = new LinkedHashMap<>();
    // 강의실명 -> 층 이름 (저장 시 floors 구조 재구성에 필요)
    private final Map<String, String> classroomToFloor = new LinkedHashMap<>();

    public ClassroomHandler(String masterDirPath) {
        this.masterDirPath = masterDirPath;
        this.mapper = new ObjectMapper();

        File dir = new File(dbDirectoryPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        loadMasterData();
    }

    // masterfile 디렉토리 안의 모든 건물 JSON을 읽어서 하나의 ScheduleData로 병합
    // JSON 구조: { "buildingName": "...", "floors": { "9층": { "911호": { "info": ..., "schedule": ... } } } }
    private void loadMasterData() {
        masterData = new ScheduleData();
        classroomToFile.clear();
        classroomToFloor.clear();

        File dir = new File(masterDirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("❌ 마스터 디렉토리가 존재하지 않습니다: " + masterDirPath);
            return;
        }

        File[] jsonFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            System.out.println("⚠️ 건물 JSON 파일이 없습니다: " + masterDirPath);
            return;
        }

        for (File jsonFile : jsonFiles) {
            try {
                JsonNode root = mapper.readTree(jsonFile);
                JsonNode floorsNode = root.get("floors");
                if (floorsNode == null || !floorsNode.isObject()) {
                    System.out.println("⚠️ floors 필드 없음, 스킵: " + jsonFile.getName());
                    continue;
                }

                // floors -> 층명 -> 강의실명 -> { info, schedule }
                floorsNode.fields().forEachRemaining(floorEntry -> {
                    String floorName = floorEntry.getKey();
                    JsonNode classroomsNode = floorEntry.getValue();

                    classroomsNode.fields().forEachRemaining(roomEntry -> {
                        String roomName = roomEntry.getKey();
                        JsonNode roomNode = roomEntry.getValue();

                        try {
                            Classroom classroom = mapper.treeToValue(roomNode, Classroom.class);
                            masterData.getClassrooms().put(roomName, classroom);
                            classroomToFile.put(roomName, jsonFile);
                            classroomToFloor.put(roomName, floorName);
                        } catch (Exception e) {
                            System.err.println("❌ 강의실 파싱 실패: " + roomName + " - " + e.getMessage());
                        }
                    });
                });

                System.out.println("📂 건물 로드: " + jsonFile.getName());
            } catch (IOException e) {
                System.err.println("❌ JSON 로드 실패: " + jsonFile.getName() + " - " + e.getMessage());
            }
        }

        System.out.println("✅ 총 " + masterData.getClassrooms().size() + "개 강의실 로드 완료.");
    }

    // 수정된 강의실이 속한 건물 파일을 floors 구조로 재구성하여 저장
    private synchronized void saveBuildingFile(String classroomName) {
        File targetFile = classroomToFile.get(classroomName);
        if (targetFile == null) {
            System.err.println("❌ 저장 대상 파일을 찾을 수 없습니다: " + classroomName);
            return;
        }

        try {
            JsonNode existingRoot = mapper.readTree(targetFile);
            String buildingName = existingRoot.has("buildingName")
                    ? existingRoot.get("buildingName").asText()
                    : targetFile.getName().replace(".json", "");

            // 해당 파일에 속한 강의실들을 층별로 재분류
            Map<String, Map<String, Object>> floorsJsonMap = new LinkedHashMap<>();

            for (Map.Entry<String, File> entry : classroomToFile.entrySet()) {
                if (!entry.getValue().equals(targetFile)) continue;

                String roomName = entry.getKey();
                String floorName = classroomToFloor.get(roomName);
                Classroom classroom = masterData.getClassrooms().get(roomName);
                if (classroom == null || floorName == null) continue;

                Map<String, Object> classroomDetails = new LinkedHashMap<>();
                classroomDetails.put("info", classroom.getInfo());
                classroomDetails.put("schedule", classroom.getSchedule());

                floorsJsonMap.computeIfAbsent(floorName, k -> new LinkedHashMap<>())
                             .put(roomName, classroomDetails);
            }

            Map<String, Object> jsonWrapper = new LinkedHashMap<>();
            jsonWrapper.put("buildingName", buildingName);
            jsonWrapper.put("floors", floorsJsonMap);

            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFile), "UTF-8"))) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(writer, jsonWrapper);
            }
            System.out.println("💾 건물 데이터 저장 완료: " + targetFile.getName());
        } catch (IOException e) {
            System.err.println("❌ 건물 데이터 저장 실패: " + e.getMessage());
        }
    }

    private DayReservation loadDayReservation(String dateStr) {
        File file = new File(dbDirectoryPath + "reservation_" + dateStr + ".json");
        if (file.exists() && file.length() > 0) {
            try {
                return mapper.readValue(file, DayReservation.class);
            } catch (IOException e) {
                System.err.println("⚠️ 날짜 예약 파일 로드 실패, 새로 생성합니다.");
            }
        }
        return new DayReservation(dateStr);
    }

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

    // ====================
    // [SFR-301] 강의실 현황 관리 - 강의실 정보 수정 (수용 인원·시설·상태)
    // [SFR-302] 강의실 현황 관리 - 학년도/학기별 강의실 정보 관리
    // [SFR-303] 강의실 현황 관리 - 강의실별 상태 변경 (사용가능/불가)
    // [SFR-304] 강의실 현황 관리 - 변경 사항 즉시 반영
    // ====================
    public boolean updateClassroomInfo(String classroomName, int capacity, String features, int computerCount, String status) {
        Classroom classroom = masterData.getClassrooms().get(classroomName);
        if (classroom == null) return false;

        ClassroomInfo info = classroom.getInfo();
        info.setCapacity(capacity);
        info.setFeatures(features);
        info.setComputerCount(computerCount);
        info.setStatus(status);

        saveBuildingFile(classroomName);
        return true;
    }

    // ====================
    // [SFR-401] 예약 등록 공통 - 정규 수업·기존 예약과 시간 충돌 검증
    // [SFR-402] 교수 예약 - 보강/세미나 목적으로 빈 강의 시간 예약
    // [SFR-403] 교수 예약 - 사용 목적·참석 인원 등록
    // ====================
    public boolean addReservation(String classroomName, String dateStr, String timeSlot, String purpose, String requesterName, String requesterType) {
        Classroom classroom = masterData.getClassrooms().get(classroomName);
        if (classroom == null) {
            System.out.println("⚠️ 존재하지 않는 강의실입니다: " + classroomName);
            return false;
        }

        String dayOfWeek;
        try {
            LocalDate date = LocalDate.parse(dateStr);
            dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
        } catch (Exception e) {
            System.out.println("⚠️ 날짜 포맷이 잘못되었습니다. (YYYY-MM-DD 형식을 지켜주세요)");
            return false;
        }

        List<Reservation> regularSchedule = classroom.getSchedule().get(dayOfWeek);
        if (regularSchedule != null) {
            for (Reservation regular : regularSchedule) {
                if (regular.getStatus() == 1 && isTimeOverlapping(regular.getTime(), timeSlot)) {
                    System.out.println("❌ 예약 실패: 해당 시간에는 정규 수업 [" + regular.getSubject() + "] 이(가) 있습니다.");
                    return false;
                }
            }
        }

        DayReservation dayRes = loadDayReservation(dateStr);
        for (SingleBooking booking : dayRes.getBookings()) {
            if (booking.getClassroomName().equals(classroomName) && isTimeOverlapping(booking.getTime(), timeSlot)) {
                String typeStr = booking.getStatus() == 2 ? "학생 예약" : "교수 예약";
                System.out.println("❌ 예약 실패: 이미 다른 [" + typeStr + " - " + booking.getSubject() + " (" + booking.getTime() + ")]이 예약되어 있습니다.");
                return false;
            }
        }

        int statusCode = "professor".equalsIgnoreCase(requesterType) ? 3 : 2;
        dayRes.getBookings().add(new SingleBooking(classroomName, timeSlot, purpose, requesterName, statusCode));
        dayRes.getBookings().sort(Comparator.comparing(SingleBooking::getTime));

        saveDayReservation(dayRes);
        System.out.println("🎉 예약 성공: [" + dateStr + " (" + dayOfWeek + ") " + classroomName + "] " + timeSlot);
        return true;
    }

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

    // ====================
    // [SFR-201] 강의실 현황 조회 - 특정 날짜의 정규 수업 + 예약을 병합하여 반환
    // [SFR-202] 강의실 현황 조회 - 일별 강의 현황 제공
    // ====================
    public List<Reservation> getMergedSchedule(String classroomName, String dateStr) {
        Classroom classroom = masterData.getClassrooms().get(classroomName);
        if (classroom == null) {
            return new ArrayList<>();
        }

        String dayOfWeek;
        try {
            LocalDate date = LocalDate.parse(dateStr);
            dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
        } catch (Exception e) {
            System.err.println("⚠️ 날짜 포맷 분석 실패: " + e.getMessage());
            return new ArrayList<>();
        }

        List<Reservation> masterDaySchedule = classroom.getSchedule().get(dayOfWeek);
        List<Reservation> mergedList = new ArrayList<>();

        if (masterDaySchedule != null) {
            for (Reservation r : masterDaySchedule) {
                mergedList.add(new Reservation(r.getTime(), r.getSubject(), r.getProfessor(), r.getStatus()));
            }
        }

        DayReservation dayRes = loadDayReservation(dateStr);

        for (SingleBooking booking : dayRes.getBookings()) {
            if (booking.getClassroomName().equals(classroomName)) {
                for (Reservation res : mergedList) {
                    if (res.getTime().equals(booking.getTime()) && res.getStatus() == 0) {
                        res.setSubject(booking.getSubject());
                        res.setProfessor(booking.getProfessor());
                        res.setStatus(booking.getStatus());
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

    // ====================
    // [SFR-201] 강의실 현황 조회 - 전체 강의실 스케줄 데이터 반환
    // [SFR-203] 강의실 현황 조회 - 주별 현황 제공
    // [SFR-204] 강의실 현황 조회 - 월별 현황 제공
    // ====================
    public ScheduleData getScheduleData() {
        return this.masterData;
    }
}

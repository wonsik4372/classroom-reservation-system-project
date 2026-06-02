package com.crsystem.systemserver.main;

import com.crsystem.common.dto.Reservation;
import com.crsystem.systemserver.service.ClassroomService;
import com.crsystem.systemserver.service.ScheduleInitializer;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ServerApplication {
    private static final Scanner scanner = new Scanner(System.in);
    private static ClassroomService service;

    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {}

        String buildingRootFolderPath = "data/timetable";
        String jsonOutputDirPath = "data/masterfile";

        System.out.println("🔄 각 건물별 PDF 파일로부터 마스터 시간표 데이터를 동기화하는 중...");
        ScheduleInitializer initializer = new ScheduleInitializer();
        initializer.convertAllTimetableToJsons(buildingRootFolderPath, jsonOutputDirPath);

        service = new ClassroomService(jsonOutputDirPath);

        System.out.println("=========================================");
        System.out.println("🏫 CRS (강의실 예약 시스템) 서버 제어 콘솔");
        System.out.println("=========================================");

        while (true) {
            System.out.println("\n[메뉴를 선택해 주세요]");
            System.out.println("1: 강의실 정보 수정");
            System.out.println("2: 강의실 예약 신청");
            System.out.println("3: 강의실 시간표 조회 (예약 반영) 📅");
            System.out.println("0: 프로그램 종료");
            System.out.print("👉 선택: ");

            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                handleUpdateClassroom();
            } else if (choice.equals("2")) {
                handleReservation();
            } else if (choice.equals("3")) {
                handleShowSchedule();
            } else if (choice.equals("0")) {
                System.out.println("👋 프로그램을 종료합니다. 이용해 주셔서 감사합니다.");
                break;
            } else {
                System.out.println("⚠️ 올바르지 않은 입력입니다. 1, 2, 3, 0 중에서만 선택해 주세요.");
            }
        }
    }

    // 1. 강의실 정보 수정 제어 기능 (마스터 파일 수정)
    private static void handleUpdateClassroom() {
        System.out.println("\n--- [강의실 정보 수정] ---");

        System.out.print("🔎 수정할 강의실 번호 입력 (정확히 입력, 예: 911호, 918호): ");
        String classroomName = scanner.nextLine().trim();

        int capacity = getValidIntegerInput("👥 최대 수용 인원 입력 (숫자만, 예: 40): ", 1, 60);

        System.out.print("📝 특이사항 입력 (예: 인터넷 사용 불가, 빔 사용 불가 / 없으면 엔터): ");
        String features = scanner.nextLine().trim();

        int computerCount = getValidIntegerInput("💻 사용 가능한 컴퓨터 개수 입력 (실습실이 아니면 0): ", 0, 60);

        String status = getValidStatusInput();

        boolean success = service.updateClassroomInfo(classroomName, capacity, features, computerCount, status);
        if (success) {
            System.out.println("✨ 강의실 정보가 정상적으로 마스터 데이터에 업데이트되었습니다!");
        } else {
            System.out.println("❌ 정보 수정에 실패했습니다. 강의실 이름(예: '912호')을 정확히 입력했는지 확인해 주세요.");
        }
    }

    // 2. 강의실 예약 제어 기능 (날짜별 전용 파일에 저장)
    private static void handleReservation() {
        System.out.println("\n--- [강의실 예약 신청] ---");

        System.out.print("🔎 예약할 강의실 번호 입력 (정확히 입력, 예: 911호, 918호): ");
        String classroomName = scanner.nextLine().trim();

        String dateStr = getValidDateInput();
        String timeSlot = getValidTimeSlotInput();

        System.out.print("📋 예약 목적/과목명 입력: ");
        String purpose = scanner.nextLine().trim();
        while (purpose.isEmpty()) {
            System.out.print("⚠️ 예약 목적은 비워둘 수 없습니다. 다시 입력: ");
            purpose = scanner.nextLine().trim();
        }

        System.out.print("👤 예약자 이름 입력: ");
        String requesterName = scanner.nextLine().trim();
        while (requesterName.isEmpty()) {
            System.out.print("⚠️ 예약자 이름은 비워둘 수 없습니다. 다시 입력: ");
            requesterName = scanner.nextLine().trim();
        }

        String requesterType = getValidRequesterTypeInput();

        boolean success = service.addReservation(classroomName, dateStr, timeSlot, purpose, requesterName, requesterType);
        if (success) {
            System.out.println("🎉 예약 등록이 성공적으로 처리되었습니다!");
        } else {
            System.out.println("❌ 예약 신청이 반려되었습니다. 날짜, 이미 채워져 있는 시간인지 다시 확인해 주세요.");
        }
    }

    // 3. 특정 날짜의 특정 강의실 시간표 시각화 출력
    private static void handleShowSchedule() {
        System.out.println("\n--- [강의실 시간표 조회] ---");
        System.out.print("🔎 조회할 강의실 번호 입력 (예: 911호, 918호): ");
        String classroomName = scanner.nextLine().trim();

        String dateStr = getValidDateInput();

        List<Reservation> mergedSchedule = service.getMergedSchedule(classroomName, dateStr);

        if (mergedSchedule.isEmpty()) {
            System.out.println("❌ 해당 강의실 정보가 존재하지 않거나 시간표를 불러올 수 없습니다.");
            return;
        }

        LocalDate date = LocalDate.parse(dateStr);
        String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);

        printClassroomScheduleTable(classroomName, dateStr, dayOfWeek, mergedSchedule);
    }

    // 콘솔 전용 시간표 그리드 테이블 렌더링 헬퍼
    private static void printClassroomScheduleTable(String classroom, String date, String day, List<Reservation> schedule) {
        System.out.println("\n==========================================================================");
        System.out.printf("  🏫 [%s] - 📅 날짜: %s (%s요일) 예약 및 수업 전체 현황\n", classroom, date, day);
        System.out.println("==========================================================================");
        System.out.printf("| %-6s | %-11s | %-25s | %-12s | %-8s |\n", "교시", " 시간대", "과목명 / 예약 목적", "예약자/교수", "상태");
        System.out.println("--------------------------------------------------------------------------");

        int index = 1;
        for (Reservation res : schedule) {
            String statusStr;
            switch (res.getStatus()) {
                case 1:
                    statusStr = "🔴 [수업]";
                    break;
                case 2:
                    statusStr = "🔵 [학생예약]";
                    break;
                case 3:
                    statusStr = "🟢 [교수예약]";
                    break;
                default:
                    statusStr = "⚪ [가능]";
                    break;
            }

            String subject = res.getSubject();
            if (subject.length() > 12) {
                subject = subject.substring(0, 10) + "..";
            }

            String professor = res.getProfessor();
            if (professor.length() > 6) {
                professor = professor.substring(0, 5) + "..";
            }

            System.out.printf("| %-4d교시 | %-11s | %-18s | %-9s | %-9s |\n",
                    index++,
                    res.getTime(),
                    padRightForKorean(subject, 18),
                    padRightForKorean(professor, 9),
                    statusStr);
        }
        System.out.println("==========================================================================");
    }

    // 한글 정렬 깨짐 보정 유틸리티
    private static String padRightForKorean(String str, int length) {
        if (str == null) return " ".repeat(length);
        int characterLength = 0;
        for (char c : str.toCharArray()) {
            if (c >= '가' && c <= '힣') {
                characterLength += 2;
            } else {
                characterLength += 1;
            }
        }
        int paddingSize = length - characterLength;
        if (paddingSize <= 0) return str;
        return str + " ".repeat(paddingSize);
    }

    // 정수 범위 체크 헬퍼
    private static int getValidIntegerInput(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(input);
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.printf("⚠️ 입력 범위를 벗어났습니다. (%d ~ %d 사이로 입력)\n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("⚠️ 유효한 숫자가 아닙니다. 숫자만 입력해 주세요.");
            }
        }
    }

    // 예약 날짜 포맷 검증 헬퍼 (YYYY-MM-DD)
    private static String getValidDateInput() {
        Pattern datePattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
        while (true) {
            System.out.print("📅 예약 날짜 입력 (형식: YYYY-MM-DD, 예: 2026-05-24): ");
            String dateInput = scanner.nextLine().trim();
            if (datePattern.matcher(dateInput).matches()) {
                return dateInput;
            }
            System.out.println("⚠️ 날짜 형식이 잘못되었습니다. 반드시 'YYYY-MM-DD' 형식으로 입력해 주세요.");
        }
    }

    // 시간 포맷(HH:mm-HH:mm) 정밀 검증 헬퍼
    private static String getValidTimeSlotInput() {
        Pattern timePattern = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-3]):[0-5]\\d$");
        while (true) {
            System.out.print("⏰ 예약 시간대 입력 (포맷: HH:mm-HH:mm, 예: 11:00-11:50): ");
            String timeSlot = scanner.nextLine().trim().replace(" ", "");
            if (timePattern.matcher(timeSlot).matches()) {
                String[] parts = timeSlot.split("-");
                if (parts[0].compareTo(parts[1]) < 0) {
                    return timeSlot;
                }
                System.out.println("⚠️ 시작 시간이 종료 시간보다 늦거나 같을 수 없습니다.");
            } else {
                System.out.println("⚠️ 포맷이 잘못되었습니다. 반드시 '13:00-14:30' 형태로 입력해 주세요.");
            }
        }
    }

    // 강의실 운영 상태 유효성 검증 헬퍼
    private static String getValidStatusInput() {
        while (true) {
            System.out.print("⚙️ 강의실 운영 상태 입력 (사용 가능 / 사용 불가): ");
            String status = scanner.nextLine().trim();
            if (status.equals("사용 가능") || status.equals("사용 불가")) {
                return status;
            }
            System.out.println("⚠️ '사용 가능' 또는 '사용 불가' 중 하나만 입력해 주세요.");
        }
    }

    // 신청자 구분 검증 헬퍼
    private static String getValidRequesterTypeInput() {
        while (true) {
            System.out.print("🏷️ 예약 주체 선택 (1: 학생, 2: 교수): ");
            String typeChoice = scanner.nextLine().trim();
            if (typeChoice.equals("1")) {
                return "student";
            } else if (typeChoice.equals("2")) {
                return "professor";
            }
            System.out.println("⚠️ 올바른 번호가 아닙니다. 1(학생) 또는 2(교수)를 선택해 주세요.");
        }
    }
}

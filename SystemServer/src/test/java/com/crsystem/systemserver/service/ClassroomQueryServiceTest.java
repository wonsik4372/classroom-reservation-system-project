package com.crsystem.systemserver.service;

import com.crsystem.common.dto.Classroom;
import com.crsystem.common.dto.Reservation;
import com.crsystem.common.dto.ScheduleData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.dto.ReservationDTO;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ClassroomQueryServiceTest extends BaseUserFileTest {

    private ClassroomService classroomHandler;
    private final String MASTERFILE_DIR = "data/masterfile/";

    @BeforeEach
    void setUpHandlerAndData() throws Exception {
        // [격리 환경 세팅] 테스트 실행 전 가상의 건물 JSON 마스터 파일 생성
        Path path = Path.of(MASTERFILE_DIR);
        Files.createDirectories(path);
        
        // ClassroomHandler의 loadMasterData()가 읽을 수 있도록 실제 구조에 맞춘 JSON 파일 작성
        Files.writeString(path.resolve("2027.json"), """
            {
              "buildingName": "상경관",
              "floors": {
                "4층": {
                  "411호": {
                    "info": {
                      "status": "사용 가능",
                      "capacity": 60,
                      "features": "빔프로젝터",
                      "computerCount": 2
                    },
                    "schedule": {
                      "월": [
                        { "time": "09:00-09:50", "subject": "빈 시간", "professor": "없음", "status": 0 },
                        { "time": "10:00-10:50", "subject": "창업재무회계", "professor": "강영진", "status": 1 }
                      ],
                      "화": [
                        { "time": "11:00-11:50", "subject": "빈 시간", "professor": "없음", "status": 0 }
                      ]
                    }
                  }
                }
              }
            }
            """);

        // 소스 코드에 명시된 public ClassroomHandler(String masterDirPath) 생성자 호출
        classroomHandler = new ClassroomService(MASTERFILE_DIR);
    }

    // ====================
    // [TC-12] SFR-201 강의실 현황 조회 - 특정 강의실 기본 정보 및 잔여 스케줄 마스터 데이터 연동 검증
    // ====================
    @Test
    public void getScheduleData_returnsClassroomBasicInfoAndMasterData() {
        // When: 소스 코드의 [SFR-201]에 명시된 getScheduleData() 호출
        ScheduleData scheduleData = classroomHandler.getScheduleData();
        
        // Then
        assertNotNull(scheduleData);
        assertTrue(scheduleData.getClassrooms().containsKey("411호"));
        Classroom classroom = scheduleData.getClassrooms().get("411호");
        
        assertNotNull(classroom.getInfo());
        assertEquals("사용 가능", classroom.getInfo().getStatus());
        assertEquals(60, classroom.getInfo().getCapacity());
        assertEquals(2, classroom.getInfo().getComputerCount());
        assertEquals("빔프로젝터", classroom.getInfo().getFeatures());
   
        assertNotNull(classroom.getSchedule(), "시간표 맵 데이터가 null이면 안 됩니다.");
        assertTrue(classroom.getSchedule().containsKey("월"), "시간표에 '월'요일 데이터가 포함되어야 합니다.");
        assertTrue(classroom.getSchedule().containsKey("화"), "시간표에 '화'요일 데이터가 포함되어야 합니다.");

        List<Reservation> mondaySchedule = classroom.getSchedule().get("월");
        assertEquals(2, mondaySchedule.size(), "SetUp에서 넣은 월요일 정규 수업/빈 시간 개수는 총 2개여야 합니다.");

        Reservation slot1 = mondaySchedule.get(0);
        assertEquals("09:00-09:50", slot1.getTime());
        assertEquals("빈 시간", slot1.getSubject());
        assertEquals("없음", slot1.getProfessor());
        assertEquals(0, slot1.getStatus());

        Reservation slot2 = mondaySchedule.get(1);
        assertEquals("10:00-10:50", slot2.getTime());
        assertEquals("창업재무회계", slot2.getSubject());
        assertEquals("강영진", slot2.getProfessor());
        assertEquals(1, slot2.getStatus());
    }

    // ====================
    // [TC-13] SFR-202 강의실 현황 조회 - 조교 기준 일별/주별/월별 시간표 데이터 및 상세 예약 정보 조회 무결성 검증
    // ====================
    @Test
    public void getReservationList_asAdmin_loadsAllDataWithFullDetails() {
        // [Given] 기준 날짜 설정 (예: 2026년 6월 8일)
        java.time.LocalDate baseDate = java.time.LocalDate.of(2026, 6, 8);

        // [When] 서버로부터 전체 예약 데이터 원본 로드
        ResponseDTO response = ReservationService.getInstance().getReservationList();

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getPayload(), "응답 페이로드가 null이 아니어야 합니다.");

        List<ReservationDTO.Response> allReservations = (List<ReservationDTO.Response>) response.getPayload();

        // 1. [일별 조회 필터링 검증]
        List<ReservationDTO.Response> dailyReservations = allReservations.stream()
                .filter(res -> res.getDate() != null && res.getDate().equals(baseDate))
                .collect(Collectors.toList());
        
        assertNotNull(dailyReservations, "조교 일별 조회 결과 리스트는 null일 수 없습니다.");
        for (ReservationDTO.Response res : dailyReservations) {
            assertEquals(baseDate, res.getDate());
        }
        
        // 2. [주별 조회 필터링 검증]
        java.time.temporal.WeekFields wf = java.time.temporal.WeekFields.ISO;
        int baseWeek = baseDate.get(wf.weekOfWeekBasedYear());
        int baseYear = baseDate.getYear();

        List<ReservationDTO.Response> weeklyReservations = allReservations.stream()
                .filter(res -> res.getDate() != null 
                        && res.getDate().getYear() == baseYear
                        && res.getDate().get(wf.weekOfWeekBasedYear()) == baseWeek)
                .collect(Collectors.toList());

        assertNotNull(weeklyReservations, "조교 주별 조회 결과 리스트는 null일 수 없습니다.");
        for (ReservationDTO.Response res : weeklyReservations) {
            assertEquals(baseYear, res.getDate().getYear());
            assertEquals(baseWeek, res.getDate().get(wf.weekOfWeekBasedYear()));
        }

        // 3. [월별 조회 필터링 검증] -> 명세서 요구사항 반영 완료!
        java.time.Month baseMonth = baseDate.getMonth();
        
        List<ReservationDTO.Response> monthlyReservations = allReservations.stream()
                .filter(res -> res.getDate() != null 
                        && res.getDate().getYear() == baseYear
                        && res.getDate().getMonth() == baseMonth)
                .collect(Collectors.toList());

        assertNotNull(monthlyReservations, "조교 월별 조회 결과 리스트는 null일 수 없습니다.");
        for (ReservationDTO.Response res : monthlyReservations) {
            assertEquals(baseYear, res.getDate().getYear(), "연도가 일치해야 합니다.");
            assertEquals(baseMonth, res.getDate().getMonth(), "해당 월이 일치해야 합니다.");
        }

        // 4. [SFR-202 핵심: 조교용 상세 정보 무결성 단언]
        for (ReservationDTO.Response res : dailyReservations) {
            assertNotNull(res.getRoomName());
            assertNotNull(res.getUserId(), "조교 조회 시 예약자의 고유 ID(학번/교원번호)가 반드시 노출되어야 합니다.");
            assertNotNull(res.getUserName());
            assertNotNull(res.getPurpose());
            assertNotNull(res.getStatus());
        }
    }
   // ====================
    // [TC-14] SFR-203 강의실 현황 조회 - 학생, 교수 기준 일별/주별/월별 시간표 데이터 및 예약 조회
    // ====================
    @Test
    public void getReservationList_loadsAllDataAndFiltersByUserId() {
        String loggedInUserId = "20260001";
        java.time.LocalDate baseDate = java.time.LocalDate.of(2026, 6, 8);

        ResponseDTO response = ReservationService.getInstance().getReservationList();

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getPayload(), "응답 페이로드가 null이 아니어야 합니다.");

        List<ReservationDTO.Response> allReservations = (List<ReservationDTO.Response>) response.getPayload();

        // 1. 내 예약 목록 필터링 구조 검증
        List<ReservationDTO.Response> myFilteredList = allReservations.stream()
                .filter(res -> loggedInUserId.equals(res.getUserId()))
                .collect(Collectors.toList());

        assertNotNull(myFilteredList);
        for (ReservationDTO.Response myRes : myFilteredList) {
            assertEquals(loggedInUserId, myRes.getUserId());
            assertNotNull(myRes.getRoomName());
            assertNotNull(myRes.getDate());
            assertNotNull(myRes.getPeriodInfo());
        }

        // 2. 일별(Daily) 필터링 조건 만족 여부 검증
        List<ReservationDTO.Response> dailyReservations = allReservations.stream()
                .filter(res -> res.getDate() != null && res.getDate().equals(baseDate))
                .collect(Collectors.toList());
        
        assertNotNull(dailyReservations);
        for (ReservationDTO.Response res : dailyReservations) {
            assertEquals(baseDate, res.getDate());
        }
        
        // 3. 주별(Weekly) 필터링 조건 만족 여부 검증
        java.time.temporal.WeekFields wf = java.time.temporal.WeekFields.ISO;
        int baseWeek = baseDate.get(wf.weekOfWeekBasedYear());
        int baseYear = baseDate.getYear();

        List<ReservationDTO.Response> weeklyReservations = allReservations.stream()
                .filter(res -> res.getDate() != null 
                        && res.getDate().getYear() == baseYear
                        && res.getDate().get(wf.weekOfWeekBasedYear()) == baseWeek)
                .collect(Collectors.toList());

        assertNotNull(weeklyReservations);
        for (ReservationDTO.Response res : weeklyReservations) {
            assertEquals(baseYear, res.getDate().getYear());
            assertEquals(baseWeek, res.getDate().get(wf.weekOfWeekBasedYear()));
        }

        // 4. 월별(Monthly) 필터링 조건 만족 여부 검증 -> 학생/교수 기준 요구사항 추가 반영 완료!
        java.time.Month baseMonth = baseDate.getMonth();

        List<ReservationDTO.Response> monthlyReservations = allReservations.stream()
                .filter(res -> res.getDate() != null 
                        && res.getDate().getYear() == baseYear
                        && res.getDate().getMonth() == baseMonth)
                .collect(Collectors.toList());

        assertNotNull(monthlyReservations, "학생/교수 월별 조회 결과 리스트는 null일 수 없습니다.");
        for (ReservationDTO.Response res : monthlyReservations) {
            assertEquals(baseYear, res.getDate().getYear());
            assertEquals(baseMonth, res.getDate().getMonth());
        }

        // 5. 보안 및 기본 출력 데이터 무결성 검증
        for (ReservationDTO.Response res : dailyReservations) {
            assertNotNull(res.getRoomName());
            assertNotNull(res.getUserName());
            assertNotNull(res.getRoleType());
        }
    }

    // ====================
    // [TC-15] SFR-204 강의실 현황 조회 - 특정 강의실 조회 시 수업/예약이 없는 "빈 시간" 타임슬롯만 정상 필터링(조회)되는지 검증
    // ====================
    @Test
    public void getMergedSchedule_filtersAndReturnsEmptyTimeSlots() {
        // Given
        String classroomName = "411호";
        String targetDate = "2026-06-08"; // 2026년 6월 8일 (월요일)

        // When: 해당 날짜의 전체 병합 스케줄 조회
        List<Reservation> mergedSchedule = classroomHandler.getMergedSchedule(classroomName, targetDate);

        // Then: 전체 스케줄 중 "빈 시간" (status == 0)인 타임슬롯만 추출하여 검증
        List<Reservation> emptyTimes = mergedSchedule.stream()
                .filter(r -> r.getStatus() == 0)
                .toList();

        // 1. 빈 시간 목록이 널이 아니고 데이터가 존재하는지 검증
        assertNotNull(emptyTimes, "빈 시간 목록은 null일 수 없습니다.");
        assertFalse(emptyTimes.isEmpty(), "테스트 데이터 상에 빈 시간이 존재해야 합니다.");

        // 2. 필터링된 모든 타임슬롯이 실제로 "빈 시간" 상태(status: 0, subject: "빈 시간", professor: "없음")인지 검증
        for (Reservation emptySlot : emptyTimes) {
            assertEquals(0, emptySlot.getStatus(), "빈 시간의 status는 0이어야 합니다.");
            assertEquals("빈 시간", emptySlot.getSubject(), "빈 시간의 subject는 '빈 시간'이어야 합니다.");
            assertEquals("없음", emptySlot.getProfessor(), "빈 시간의 professor는 '없음'이어야 합니다.");
        }

        // 3. setUp에서 넣은 가상 데이터 기준(09:00~09:50이 빈 시간) 첫 번째 빈 시간의 타임 규격 확인
        assertEquals("09:00-09:50", emptyTimes.get(0).getTime());
    }
}
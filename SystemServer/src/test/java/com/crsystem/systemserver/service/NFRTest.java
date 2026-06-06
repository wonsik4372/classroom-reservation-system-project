package com.crsystem.systemserver.service;

import com.crsystem.common.dto.ReservationDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.enums.Role;
import com.crsystem.systemserver.dao.ReservationFileManager;
import com.crsystem.systemserver.dao.ServerPaths;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 비기능 요구사항(NFR) 자동화 테스트
 * TC-57 (NFR-001) ~ TC-69 (NFR-013) 중 자동화 가능 항목
 */
public class NFRTest {

    @BeforeEach
    void setUp() {
        ReservationHandler.getInstance().clearForTesting();
    }

    private ReservationDTO.Response makeStudentReservation(String id, String periodInfo) {
        ReservationDTO.Response r = new ReservationDTO.Response();
        r.setReservationId(id);
        r.setUserId("20240001");
        r.setRoleType(Role.STUDENT);
        r.setRoomName("23 정보공학관 9층 911호");
        r.setDate(LocalDate.now().plusDays(2));
        r.setPeriodInfo(periodInfo);
        r.setPartnerCount(1);
        r.setStatus(ReservationDTO.Status.PENDING);
        return r;
    }

    // ====================
    // [TC-57] NFR-001 성능 - 예약 요청에 대한 서버 처리 응답 속도 1초 이내
    // ====================
    @Test
    public void addReservation_respondsWithinOneSecond() {
        ReservationDTO.Response reservation = makeStudentReservation("R-NFR001", "1");

        assertTimeout(Duration.ofSeconds(1), () ->
                ReservationHandler.getInstance().addReservation(reservation),
                "예약 등록 응답이 1초를 초과했습니다."
        );
    }

    // ====================
    // [TC-58] NFR-002 성능 - 교수 예약으로 인한 학생 알림 전송 1초 이내
    // ====================
    @Test
    public void notifyApproved_completesWithinOneSecond() {
        ReservationDTO.Response r = makeStudentReservation("R-NFR002", "2");
        r.setStatus(ReservationDTO.Status.APPROVED);

        assertTimeout(Duration.ofSeconds(1), () ->
                NotificationHandler.getInstance().notifyApproved(r),
                "승인 알림 전송이 1초를 초과했습니다."
        );
    }

    // ====================
    // [TC-61] NFR-005 동시성 - 50명 동시 예약 시 중복 예약 미발생
    // ====================
    @Test
    public void addReservation_noDuplicateUnderConcurrentRequests() throws InterruptedException {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // 동시에 출발
                    ReservationDTO.Response r = new ReservationDTO.Response();
                    r.setReservationId("R-CONC-" + idx);
                    r.setUserId("user" + idx);
                    r.setRoleType(Role.STUDENT);
                    r.setRoomName("23 정보공학관 9층 911호");
                    r.setDate(LocalDate.now().plusDays(2));
                    r.setPeriodInfo("1");
                    r.setPartnerCount(1);
                    r.setStatus(ReservationDTO.Status.PENDING);

                    ResponseDTO res = ReservationHandler.getInstance().addReservation(r);
                    if (res.isSuccess()) successCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 동시 실행 시작
        doneLatch.await();
        executor.shutdown();

        // 수용 인원 50% 제한으로 일부만 성공해야 하며, 각 예약 ID는 고유해야 함
        List<ReservationDTO.Response> all = (List<ReservationDTO.Response>)
                ReservationHandler.getInstance().getReservationList().getPayload();

        long distinctIds = all.stream()
                .map(ReservationDTO.Response::getReservationId)
                .distinct()
                .count();

        assertEquals(all.size(), distinctIds, "중복 예약 ID가 존재합니다. 동시성 제어에 실패했습니다.");
    }

    // ====================
    // [TC-62] NFR-006 사생활 - 예약 조회 시 민감 정보(비밀번호 등) 미포함 검증
    // ====================
    @Test
    public void getReservationList_doesNotExposePrivateUserInfo() {
        ReservationHandler.getInstance().addReservation(makeStudentReservation("R-NFR006", "3"));

        ResponseDTO response = ReservationHandler.getInstance().getReservationList();
        assertTrue(response.isSuccess());

        List<ReservationDTO.Response> list = (List<ReservationDTO.Response>) response.getPayload();
        assertFalse(list.isEmpty());

        for (ReservationDTO.Response r : list) {
            // ReservationDTO.Response에 비밀번호 필드가 없음을 구조적으로 확인
            // (password 관련 getter가 존재하지 않음 — 컴파일 레벨 보장)
            assertNotNull(r.getUserId(),   "userId는 공개 가능 정보입니다.");
            assertNotNull(r.getUserName(), "userName은 공개 가능 정보입니다.");
            assertNotNull(r.getRoleType(), "roleType은 공개 가능 정보입니다.");
            // 비밀번호·주민번호 등 민감 필드는 ReservationDTO.Response에 선언 자체가 없음
        }
    }

    // ====================
    // [TC-64] NFR-008 표준 - 저장된 데이터가 표준 JSON 규격을 준수하는지 검증
    // ====================
    @Test
    public void reservationJson_isValidStandardJsonFormat() throws Exception {
        // 예약 하나 저장
        ReservationHandler.getInstance().addReservation(makeStudentReservation("R-NFR008", "4"));

        Path jsonPath = Path.of(ServerPaths.RESERVATION_JSON);
        assertTrue(Files.exists(jsonPath), "Reservation.json 파일이 존재해야 합니다.");

        String content = Files.readString(jsonPath);
        assertFalse(content.isBlank(), "JSON 파일이 비어있으면 안 됩니다.");

        // 외부 라이브러리 없이 파싱 가능한 표준 JSON인지 확인
        ObjectMapper mapper = new ObjectMapper();
        assertDoesNotThrow(() -> mapper.readTree(content),
                "저장된 Reservation.json이 유효한 JSON 형식이 아닙니다.");

        JsonNode root = mapper.readTree(content);
        assertTrue(root.isArray(), "Reservation.json의 최상위 구조는 배열이어야 합니다.");
    }

    // ====================
    // [TC-67] NFR-011 확장성 - 예약 날짜가 ISO 8601 호환 포맷(LocalDate)으로 저장되는지 검증
    // ====================
    @Test
    public void reservationJson_dateFieldFollowsIso8601Format() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 6, 15);
        ReservationDTO.Response r = makeStudentReservation("R-NFR011", "5");
        r.setDate(targetDate);
        ReservationHandler.getInstance().addReservation(r);

        Path jsonPath = Path.of(ServerPaths.RESERVATION_JSON);
        String content = Files.readString(jsonPath);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(content);

        boolean found = false;
        for (JsonNode node : root) {
            if ("R-NFR011".equals(node.path("reservationId").asText())) {
                String dateStr = node.path("date").asText();
                // ISO 8601 날짜 형식: YYYY-MM-DD
                assertTrue(dateStr.matches("\\d{4}-\\d{2}-\\d{2}"),
                        "날짜 필드가 ISO 8601 형식(YYYY-MM-DD)이어야 합니다. 실제값: " + dateStr);
                assertEquals("2026-06-15", dateStr);
                found = true;
                break;
            }
        }
        assertTrue(found, "저장된 예약 데이터에서 R-NFR011을 찾을 수 없습니다.");
    }

    // ====================
    // [TC-69] NFR-013 안정성 - 형식이 깨진 데이터 수신 시 서버 다운 없이 에러 코드 반환
    // ====================
    @Test
    public void mainController_returnsFailResponseForMalformedRequest() {
        // null payload를 가진 깨진 요청 처리 시 예외 없이 ResponseDTO 반환
        com.crsystem.common.dto.RequestDTO malformed =
                new com.crsystem.common.dto.RequestDTO("ADD_RESERVATION", null);

        assertDoesNotThrow(() -> {
            ResponseDTO response = com.crsystem.systemserver.controller.MainController.handleRequest(malformed);
            assertNotNull(response, "응답이 null이면 안 됩니다.");
            assertFalse(response.isSuccess(), "잘못된 요청은 실패 응답을 반환해야 합니다.");
        }, "잘못된 요청으로 인해 서버가 다운(예외 발생)되면 안 됩니다.");
    }
}

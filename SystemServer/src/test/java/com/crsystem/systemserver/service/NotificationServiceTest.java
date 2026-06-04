package com.crsystem.systemserver.service;

import com.crsystem.common.dto.NotificationDTO;
import com.crsystem.common.dto.ReservationDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.enums.Role;
import com.crsystem.systemserver.model.NotificationCatalog;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationServiceTest {

    @BeforeEach
    void clearStore() throws Exception {
        Field storeField = NotificationCatalog.class.getDeclaredField("store");
        storeField.setAccessible(true);
        Map<?, ?> store = (Map<?, ?>) storeField.get(NotificationCatalog.getInstance());
        store.clear();
    }

    private ReservationDTO.Response makeReservation(String reservationId, String userId,
                                                     String roomName, String periodInfo,
                                                     Role role) {
        ReservationDTO.Response r = new ReservationDTO.Response();
        r.setReservationId(reservationId);
        r.setUserId(userId);
        r.setRoomName(roomName);
        r.setDate(LocalDate.now().plusDays(1));
        r.setPeriodInfo(periodInfo);
        r.setRoleType(role);
        r.setStatus(ReservationDTO.Status.PENDING);
        return r;
    }

    // ====================
    // getInstance
    // ====================

    @Test
    public void getInstance_returnsSameInstance() {
        assertSame(NotificationHandler.getInstance(), NotificationHandler.getInstance());
    }

    // ====================
    // [TC-33] [TC-34] 예약상태관리-승인 - 승인 알림 생성 및 저장
    // ====================
    @Test
    public void notifyApproved_createsApprovedNotificationInStore() {
        ReservationDTO.Response reservation = makeReservation(
                "R-001", "20240001", "911", "1,2", Role.STUDENT);

        NotificationHandler.getInstance().notifyApproved(reservation);

        List<NotificationDTO> notifications =
                NotificationCatalog.getInstance().getUnreadNotifications("20240001");
        assertEquals(1, notifications.size());
        assertEquals(NotificationDTO.Type.APPROVED, notifications.get(0).getType());
        assertEquals("R-001", notifications.get(0).getReservationId());
        assertEquals("20240001", notifications.get(0).getUserId());
    }

    // ====================
    // [TC-33] 예약상태관리-승인 - 승인 알림 메시지에 강의실명과 날짜 포함
    // ====================
    @Test
    public void notifyApproved_messageContainsRoomNameAndDate() {
        ReservationDTO.Response reservation = makeReservation(
                "R-001", "20240001", "911", "1,2", Role.STUDENT);

        NotificationHandler.getInstance().notifyApproved(reservation);

        NotificationDTO notification =
                NotificationCatalog.getInstance().getUnreadNotifications("20240001").get(0);
        assertTrue(notification.getMessage().contains("911"));
        assertTrue(notification.getMessage().contains("승인"));
    }

    // ====================
    // [TC-34] 예약상태관리-승인 - 생성된 알림은 기본적으로 미읽음 상태
    // ====================
    @Test
    public void notifyApproved_notificationIsUnreadByDefault() {
        ReservationDTO.Response reservation = makeReservation(
                "R-001", "20240001", "911", "1", Role.STUDENT);

        NotificationHandler.getInstance().notifyApproved(reservation);

        NotificationDTO notification =
                NotificationCatalog.getInstance().getUnreadNotifications("20240001").get(0);
        assertFalse(notification.isRead());
    }

    // ====================
    // [TC-36] SFR-505 예약상태관리-거부 - 거부 알림 생성 및 거부 사유 저장
    // ====================
    @Test
    public void notifyRejected_createsRejectedNotificationWithReasonInStore() {
        ReservationDTO.Response reservation = makeReservation(
                "R-002", "20240001", "911", "3", Role.STUDENT);
        String reason = "수업 일정과 중복";

        NotificationHandler.getInstance().notifyRejected(reservation, reason);

        List<NotificationDTO> notifications =
                NotificationCatalog.getInstance().getUnreadNotifications("20240001");
        assertEquals(1, notifications.size());
        assertEquals(NotificationDTO.Type.REJECTED, notifications.get(0).getType());
        assertEquals(reason, notifications.get(0).getRejectReason());
    }

    // ====================
    // [TC-36] SFR-505 예약상태관리-거부 - 거부 알림 메시지에 강의실 정보 포함
    // ====================
    @Test
    public void notifyRejected_messageContainsRoomInfo() {
        ReservationDTO.Response reservation = makeReservation(
                "R-002", "20240001", "915", "2", Role.STUDENT);

        NotificationHandler.getInstance().notifyRejected(reservation, "강의실 사용 불가");

        NotificationDTO notification =
                NotificationCatalog.getInstance().getUnreadNotifications("20240001").get(0);
        assertTrue(notification.getMessage().contains("915"));
        assertTrue(notification.getMessage().contains("거부"));
    }

    // ====================
    // [TC-37] SFR-506 예약상태관리-거부 - 거부 사유가 알림에 정확히 저장
    // ====================
    @Test
    public void notifyRejected_rejectReasonIsStoredInNotification() {
        ReservationDTO.Response reservation = makeReservation(
                "R-003", "20240001", "911", "1", Role.STUDENT);
        String reason = "교수 보강";

        NotificationHandler.getInstance().notifyRejected(reservation, reason);

        NotificationDTO notification =
                NotificationCatalog.getInstance().getUnreadNotifications("20240001").get(0);
        assertEquals(reason, notification.getRejectReason());
        assertEquals("R-003", notification.getReservationId());
    }

    // ====================
    // [TC-35] SFR-503 / [TC-38] SFR-507 예약상태관리 - 미읽음 알림 조회 후 읽음 처리
    // ====================
    @Test
    public void getAndMarkNotifications_returnsUnreadNotificationsAndMarksThem() {
        NotificationHandler service = NotificationHandler.getInstance();
        ReservationDTO.Response res1 = makeReservation("R-001", "20240001", "911", "1", Role.STUDENT);
        ReservationDTO.Response res2 = makeReservation("R-002", "20240001", "911", "2", Role.STUDENT);
        service.notifyApproved(res1);
        service.notifyRejected(res2, "일정 중복");

        ResponseDTO response = service.getAndMarkNotifications("20240001");

        assertTrue(response.isSuccess());
        @SuppressWarnings("unchecked")
        List<NotificationDTO> returned = (List<NotificationDTO>) response.getPayload();
        assertEquals(2, returned.size());

        // 읽음 처리 확인 — 재조회 시 빈 목록
        List<NotificationDTO> afterRead =
                NotificationCatalog.getInstance().getUnreadNotifications("20240001");
        assertTrue(afterRead.isEmpty());
    }

    // ====================
    // [TC-41] SFR-602 알림처리-거부 - 미읽음 알림 없을 때 빈 목록 반환
    // ====================
    @Test
    public void getAndMarkNotifications_returnsEmptyListWhenNoUnreadNotifications() {
        ResponseDTO response = NotificationHandler.getInstance()
                .getAndMarkNotifications("20240001");

        assertTrue(response.isSuccess());
        @SuppressWarnings("unchecked")
        List<NotificationDTO> returned = (List<NotificationDTO>) response.getPayload();
        assertNotNull(returned);
        assertTrue(returned.isEmpty());
    }

    // ====================
    // [TC-35] SFR-503 / [TC-38] SFR-507 예약상태관리 - 이미 읽은 알림은 재조회 시 반환하지 않음
    // ====================
    @Test
    public void getAndMarkNotifications_doesNotReturnAlreadyReadNotifications() {
        NotificationHandler service = NotificationHandler.getInstance();
        ReservationDTO.Response reservation = makeReservation(
                "R-001", "20240001", "911", "1", Role.STUDENT);
        service.notifyApproved(reservation);
        service.getAndMarkNotifications("20240001"); // 첫 번째 조회 — 읽음 처리

        ResponseDTO response = service.getAndMarkNotifications("20240001"); // 두 번째 조회

        @SuppressWarnings("unchecked")
        List<NotificationDTO> returned = (List<NotificationDTO>) response.getPayload();
        assertTrue(returned.isEmpty());
    }
}

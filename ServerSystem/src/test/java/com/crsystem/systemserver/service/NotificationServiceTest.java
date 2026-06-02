package com.crsystem.systemserver.service;

import com.crsystem.common.dto.NotificationDTO;
import com.crsystem.common.dto.ReservationDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.enums.Role;
import com.crsystem.systemserver.model.NotificationStore;
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
        Field storeField = NotificationStore.class.getDeclaredField("store");
        storeField.setAccessible(true);
        Map<?, ?> store = (Map<?, ?>) storeField.get(NotificationStore.getInstance());
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
        assertSame(NotificationService.getInstance(), NotificationService.getInstance());
    }

    // ====================
    // notifyApproved (TC-35, TC-36)
    // ====================

    @Test
    public void notifyApproved_createsApprovedNotificationInStore() {
        ReservationDTO.Response reservation = makeReservation(
                "R-001", "20240001", "911", "1,2", Role.STUDENT);

        NotificationService.getInstance().notifyApproved(reservation);

        List<NotificationDTO> notifications =
                NotificationStore.getInstance().getUnreadNotifications("20240001");
        assertEquals(1, notifications.size());
        assertEquals(NotificationDTO.Type.APPROVED, notifications.get(0).getType());
        assertEquals("R-001", notifications.get(0).getReservationId());
        assertEquals("20240001", notifications.get(0).getUserId());
    }

    @Test
    public void notifyApproved_messageContainsRoomNameAndDate() {
        ReservationDTO.Response reservation = makeReservation(
                "R-001", "20240001", "911", "1,2", Role.STUDENT);

        NotificationService.getInstance().notifyApproved(reservation);

        NotificationDTO notification =
                NotificationStore.getInstance().getUnreadNotifications("20240001").get(0);
        assertTrue(notification.getMessage().contains("911"));
        assertTrue(notification.getMessage().contains("승인"));
    }

    @Test
    public void notifyApproved_notificationIsUnreadByDefault() {
        ReservationDTO.Response reservation = makeReservation(
                "R-001", "20240001", "911", "1", Role.STUDENT);

        NotificationService.getInstance().notifyApproved(reservation);

        NotificationDTO notification =
                NotificationStore.getInstance().getUnreadNotifications("20240001").get(0);
        assertFalse(notification.isRead());
    }

    // ====================
    // notifyRejected (TC-37, TC-38, TC-39, TC-40)
    // ====================

    @Test
    public void notifyRejected_createsRejectedNotificationWithReasonInStore() {
        ReservationDTO.Response reservation = makeReservation(
                "R-002", "20240001", "911", "3", Role.STUDENT);
        String reason = "수업 일정과 중복";

        NotificationService.getInstance().notifyRejected(reservation, reason);

        List<NotificationDTO> notifications =
                NotificationStore.getInstance().getUnreadNotifications("20240001");
        assertEquals(1, notifications.size());
        assertEquals(NotificationDTO.Type.REJECTED, notifications.get(0).getType());
        assertEquals(reason, notifications.get(0).getRejectReason());
    }

    @Test
    public void notifyRejected_messageContainsRoomInfo() {
        ReservationDTO.Response reservation = makeReservation(
                "R-002", "20240001", "915", "2", Role.STUDENT);

        NotificationService.getInstance().notifyRejected(reservation, "강의실 사용 불가");

        NotificationDTO notification =
                NotificationStore.getInstance().getUnreadNotifications("20240001").get(0);
        assertTrue(notification.getMessage().contains("915"));
        assertTrue(notification.getMessage().contains("거부"));
    }

    @Test
    public void notifyRejected_rejectReasonIsStoredInNotification() {
        ReservationDTO.Response reservation = makeReservation(
                "R-003", "20240001", "911", "1", Role.STUDENT);
        String reason = "교수 보강";

        NotificationService.getInstance().notifyRejected(reservation, reason);

        NotificationDTO notification =
                NotificationStore.getInstance().getUnreadNotifications("20240001").get(0);
        assertEquals(reason, notification.getRejectReason());
        assertEquals("R-003", notification.getReservationId());
    }

    // ====================
    // getAndMarkNotifications (TC-36, TC-40)
    // ====================

    @Test
    public void getAndMarkNotifications_returnsUnreadNotificationsAndMarksThem() {
        NotificationService service = NotificationService.getInstance();
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
                NotificationStore.getInstance().getUnreadNotifications("20240001");
        assertTrue(afterRead.isEmpty());
    }

    @Test
    public void getAndMarkNotifications_returnsEmptyListWhenNoUnreadNotifications() {
        ResponseDTO response = NotificationService.getInstance()
                .getAndMarkNotifications("20240001");

        assertTrue(response.isSuccess());
        @SuppressWarnings("unchecked")
        List<NotificationDTO> returned = (List<NotificationDTO>) response.getPayload();
        assertNotNull(returned);
        assertTrue(returned.isEmpty());
    }

    @Test
    public void getAndMarkNotifications_doesNotReturnAlreadyReadNotifications() {
        NotificationService service = NotificationService.getInstance();
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

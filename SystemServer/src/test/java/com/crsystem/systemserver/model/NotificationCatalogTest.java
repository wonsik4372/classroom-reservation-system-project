package com.crsystem.systemserver.model;

import com.crsystem.common.dto.NotificationDTO;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationCatalogTest {

    @BeforeEach
    void clearStore() throws Exception {
        Field storeField = NotificationCatalog.class.getDeclaredField("store");
        storeField.setAccessible(true);
        Map<?, ?> store = (Map<?, ?>) storeField.get(NotificationCatalog.getInstance());
        store.clear();
    }

    private NotificationDTO makeNotification(String id, String userId, String reservationId,
                                             NotificationDTO.Type type, String rejectReason) {
        return new NotificationDTO(id, userId, reservationId, type,
                "[" + type + "] " + reservationId + " 알림", rejectReason);
    }

    // ====================
    // getInstance
    // ====================

    @Test
    public void getInstance_returnsSameInstance() {
        NotificationCatalog a = NotificationCatalog.getInstance();
        NotificationCatalog b = NotificationCatalog.getInstance();

        assertSame(a, b);
    }

    // ====================
    // addNotification
    // ====================

    @Test
    public void addNotification_storesNotificationForUser() {
        NotificationCatalog store = NotificationCatalog.getInstance();
        NotificationDTO notification = makeNotification("N-001", "20240001", "R-001",
                NotificationDTO.Type.APPROVED, null);

        store.addNotification(notification);

        List<NotificationDTO> result = store.getUnreadNotifications("20240001");
        assertEquals(1, result.size());
        assertEquals("N-001", result.get(0).getNotificationId());
    }

    @Test
    public void addNotification_storesMultipleNotificationsForSameUser() {
        NotificationCatalog store = NotificationCatalog.getInstance();
        store.addNotification(makeNotification("N-001", "20240001", "R-001",
                NotificationDTO.Type.APPROVED, null));
        store.addNotification(makeNotification("N-002", "20240001", "R-002",
                NotificationDTO.Type.REJECTED, "수업 일정과 중복"));

        List<NotificationDTO> result = store.getUnreadNotifications("20240001");
        assertEquals(2, result.size());
    }

    @Test
    public void addNotification_isolatesNotificationsByUser() {
        NotificationCatalog store = NotificationCatalog.getInstance();
        store.addNotification(makeNotification("N-001", "20240001", "R-001",
                NotificationDTO.Type.APPROVED, null));
        store.addNotification(makeNotification("N-002", "20240002", "R-002",
                NotificationDTO.Type.REJECTED, "강의실 사용 불가"));

        assertEquals(1, store.getUnreadNotifications("20240001").size());
        assertEquals(1, store.getUnreadNotifications("20240002").size());
    }

    // ====================
    // getUnreadNotifications
    // ====================

    @Test
    public void getUnreadNotifications_returnsEmptyListWhenNoNotificationsExist() {
        List<NotificationDTO> result = NotificationCatalog.getInstance()
                .getUnreadNotifications("20240001");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getUnreadNotifications_excludesAlreadyReadNotifications() {
        NotificationCatalog store = NotificationCatalog.getInstance();
        store.addNotification(makeNotification("N-001", "20240001", "R-001",
                NotificationDTO.Type.APPROVED, null));
        store.addNotification(makeNotification("N-002", "20240001", "R-002",
                NotificationDTO.Type.REJECTED, "사유"));
        store.markAsRead("20240001", List.of("N-001"));

        List<NotificationDTO> unread = store.getUnreadNotifications("20240001");

        assertEquals(1, unread.size());
        assertEquals("N-002", unread.get(0).getNotificationId());
    }

    @Test
    public void getUnreadNotifications_returnsEmptyListAfterAllMarkedAsRead() {
        NotificationCatalog store = NotificationCatalog.getInstance();
        store.addNotification(makeNotification("N-001", "20240001", "R-001",
                NotificationDTO.Type.APPROVED, null));
        store.markAsRead("20240001", List.of("N-001"));

        List<NotificationDTO> unread = store.getUnreadNotifications("20240001");

        assertTrue(unread.isEmpty());
    }

    // ====================
    // [TC-41] SFR-601 알림처리 - 지정 ID만 읽음 처리, 나머지 유지
    // ====================

    @Test
    public void markAsRead_setsReadFlagToTrue() {
        NotificationCatalog store = NotificationCatalog.getInstance();
        store.addNotification(makeNotification("N-001", "20240001", "R-001",
                NotificationDTO.Type.APPROVED, null));

        store.markAsRead("20240001", List.of("N-001"));

        List<NotificationDTO> unread = store.getUnreadNotifications("20240001");
        assertTrue(unread.isEmpty());
    }

    @Test
    public void markAsRead_doesNothingForUnknownUser() {
        NotificationCatalog store = NotificationCatalog.getInstance();
        store.addNotification(makeNotification("N-001", "20240001", "R-001",
                NotificationDTO.Type.APPROVED, null));

        assertDoesNotThrow(() -> store.markAsRead("99999", List.of("N-001")));
        assertEquals(1, store.getUnreadNotifications("20240001").size());
    }

    @Test
    public void markAsRead_onlyMarksSpecifiedIds() {
        NotificationCatalog store = NotificationCatalog.getInstance();
        store.addNotification(makeNotification("N-001", "20240001", "R-001",
                NotificationDTO.Type.APPROVED, null));
        store.addNotification(makeNotification("N-002", "20240001", "R-002",
                NotificationDTO.Type.REJECTED, "사유"));

        store.markAsRead("20240001", List.of("N-001"));

        List<NotificationDTO> unread = store.getUnreadNotifications("20240001");
        assertEquals(1, unread.size());
        assertEquals("N-002", unread.get(0).getNotificationId());
        assertFalse(unread.get(0).isRead());
    }
}

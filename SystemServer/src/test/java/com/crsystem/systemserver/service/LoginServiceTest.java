package com.crsystem.systemserver.service;

import com.crsystem.common.dto.NotificationDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.dto.UserDTO;
import com.crsystem.common.enums.Role;
import com.crsystem.systemserver.model.NotificationCatalog;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LoginServiceTest extends BaseUserFileTest {

    @BeforeEach
    void clearNotificationStore() throws Exception {
        Field storeField = NotificationCatalog.class.getDeclaredField("store");
        storeField.setAccessible(true);
        Map<?, ?> store = (Map<?, ?>) storeField.get(NotificationCatalog.getInstance());
        store.clear();
    }

    // ====================
    // [TC-01] 로그인 기본 흐름 - 올바른 ID/역할/비밀번호로 로그인 성공
    // ====================
    @Test
    public void processLogin_succeedsWhenIdRoleAndPasswordMatch() {
        LoginHandler service = LoginHandler.getInstance();
        UserDTO.Request request = new UserDTO.Request("LOGIN", "34567", "34567");
        request.setRole(Role.PROFESSOR);

        ResponseDTO response = service.processLogin(request);

        assertTrue(response.isSuccess());
        assertEquals("로그인 성공!", response.getMessage());
        assertInstanceOf(UserDTO.Response.class, response.getPayload());

        UserDTO.Response userInfo = (UserDTO.Response) response.getPayload();
        assertEquals(Role.PROFESSOR, userInfo.getRole());
        assertEquals("34567", userInfo.getId());
        assertEquals("Professor User", userInfo.getName());
    }

    // ====================
    // [TC-02] 로그인 권한정보검증 - 조교가 교수 화면으로 로그인 허용 (특수 허용 규칙)
    // ====================
    @Test
    public void processLogin_allowsAssistantToLoginAsProfessorScreen() {
        LoginHandler service = LoginHandler.getInstance();
        UserDTO.Request request = new UserDTO.Request("LOGIN", "23456", "23456");
        request.setRole(Role.PROFESSOR);

        ResponseDTO response = service.processLogin(request);

        assertTrue(response.isSuccess());
        assertEquals("로그인 성공!", response.getMessage());
        assertInstanceOf(UserDTO.Response.class, response.getPayload());

        UserDTO.Response userInfo = (UserDTO.Response) response.getPayload();
        assertEquals(Role.ASSISTANT, userInfo.getRole());
        assertEquals("23456", userInfo.getId());
        assertEquals("Assistant User", userInfo.getName());
    }

    // ====================
    // [SRS 미등재] 로그인 결과알림 - 로그인 성공 시 메시지 반환 (TC-03 보완, GUI 표시는 수동 테스트)
    // ====================
    @Test
    public void processLogin_returnsSuccessMessageOnSuccess() {
        LoginHandler service = LoginHandler.getInstance();
        UserDTO.Request request = new UserDTO.Request("LOGIN", "34567", "34567");
        request.setRole(Role.PROFESSOR);

        ResponseDTO response = service.processLogin(request);

        assertTrue(response.isSuccess());
        assertNotNull(response.getMessage());
        assertFalse(response.getMessage().isBlank());
        assertEquals("로그인 성공!", response.getMessage());
    }

    // ====================
    // [TC-03] [TC-04] 로그인 결과알림 / 실패흐름 - 존재하지 않는 계정으로 로그인 실패
    // ====================
    @Test
    public void processLogin_failsWhenUserDoesNotExist() {
        LoginHandler service = LoginHandler.getInstance();
        UserDTO.Request request = new UserDTO.Request("LOGIN", "99999", "99999");
        request.setRole(Role.PROFESSOR);

        ResponseDTO response = service.processLogin(request);

        assertFalse(response.isSuccess());
        assertEquals("로그인 실패: 존재하지 않는 계정입니다.", response.getMessage());
        assertNull(response.getPayload());
    }

    // ====================
    // [TC-02] [TC-04] 로그인 권한정보검증 / 실패흐름 - 권한 불일치로 로그인 실패
    // ====================
    @Test
    public void processLogin_failsWhenRoleDoesNotMatch() {
        LoginHandler service = LoginHandler.getInstance();
        UserDTO.Request request = new UserDTO.Request("LOGIN", "34567", "34567");
        request.setRole(Role.STUDENT);

        ResponseDTO response = service.processLogin(request);

        assertFalse(response.isSuccess());
        assertEquals("로그인 실패: 해당 권한으로 로그인할 수 없습니다.", response.getMessage());
        assertNull(response.getPayload());
    }

    // ====================
    // [TC-03] [TC-04] 로그인 결과알림 / 실패흐름 - 비밀번호 불일치로 로그인 실패
    // ====================
    @Test
    public void processLogin_failsWhenPasswordDoesNotMatch() {
        LoginHandler service = LoginHandler.getInstance();
        UserDTO.Request request = new UserDTO.Request("LOGIN", "34567", "wrong-password");
        request.setRole(Role.PROFESSOR);

        ResponseDTO response = service.processLogin(request);

        assertFalse(response.isSuccess());
        assertEquals("로그인 실패: 비밀번호가 일치하지 않습니다.", response.getMessage());
        assertNull(response.getPayload());
    }

    // ====================
    // [TC-40] SFR-601 알림처리 - 로그인 시 미읽음 알림 즉각 전달
    // ====================
    @Test
    public void processLogin_includesPendingNotificationsForStudentWithUnreadAlerts() {
        // 학생 계정을 User.json에 추가
        try {
            java.nio.file.Files.writeString(USER_FILE, """
                    [
                      { "role": "ADMIN",     "id": "admin",    "pw": "admin",    "name": "Admin User" },
                      { "role": "ASSISTANT", "id": "23456",    "pw": "23456",    "name": "Assistant User" },
                      { "role": "PROFESSOR", "id": "34567",    "pw": "34567",    "name": "Professor User" },
                      { "role": "STUDENT",   "id": "20240001", "pw": "20240001", "name": "Student User" }
                    ]
                    """);
            UserHandler.getInstance().reloadForTesting();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        // 미읽음 알림 사전 등록 (승인 + 거부)
        NotificationDTO approved = new NotificationDTO(
                "N-001", "20240001", "R-001",
                NotificationDTO.Type.APPROVED, "[예약 승인] 911 알림", null);
        NotificationDTO rejected = new NotificationDTO(
                "N-002", "20240001", "R-002",
                NotificationDTO.Type.REJECTED, "[예약 거부] 911 알림", "수업 일정과 중복");
        NotificationCatalog.getInstance().addNotification(approved);
        NotificationCatalog.getInstance().addNotification(rejected);

        UserDTO.Request request = new UserDTO.Request("LOGIN", "20240001", "20240001");
        request.setRole(Role.STUDENT);
        ResponseDTO response = LoginHandler.getInstance().processLogin(request);

        assertTrue(response.isSuccess());
        UserDTO.Response userInfo = (UserDTO.Response) response.getPayload();
        List<NotificationDTO> pending = userInfo.getPendingNotifications();
        assertNotNull(pending);
        assertEquals(2, pending.size());

        // 로그인 후 알림은 읽음 처리됨
        List<NotificationDTO> unread =
                NotificationCatalog.getInstance().getUnreadNotifications("20240001");
        assertTrue(unread.isEmpty());
    }

    // ====================
    // [TC-40] SFR-601 알림처리 - 미읽음 알림 없을 때 빈 목록 반환
    // ====================
    @Test
    public void processLogin_returnsNoPendingNotificationsWhenNoneExist() {
        try {
            java.nio.file.Files.writeString(USER_FILE, """
                    [
                      { "role": "ADMIN",     "id": "admin",    "pw": "admin",    "name": "Admin User" },
                      { "role": "ASSISTANT", "id": "23456",    "pw": "23456",    "name": "Assistant User" },
                      { "role": "PROFESSOR", "id": "34567",    "pw": "34567",    "name": "Professor User" },
                      { "role": "STUDENT",   "id": "20240001", "pw": "20240001", "name": "Student User" }
                    ]
                    """);
            UserHandler.getInstance().reloadForTesting();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        UserDTO.Request request = new UserDTO.Request("LOGIN", "20240001", "20240001");
        request.setRole(Role.STUDENT);
        ResponseDTO response = LoginHandler.getInstance().processLogin(request);

        assertTrue(response.isSuccess());
        UserDTO.Response userInfo = (UserDTO.Response) response.getPayload();
        List<NotificationDTO> pending = userInfo.getPendingNotifications();
        // 미읽음 알림 없으면 null 또는 빈 리스트
        assertTrue(pending == null || pending.isEmpty());
    }
}

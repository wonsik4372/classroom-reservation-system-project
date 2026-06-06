package com.crsystem.systemserver.service;

import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.dto.UserDTO;
import com.crsystem.common.enums.Role;
import com.crsystem.systemserver.model.User;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserHandlerTest extends BaseUserFileTest {
    
    // ====================
    // [SRS 미등재] 사용자관리-사용자목록조회 - 사용자 목록 조회 정상 반환 (SFR-104 보완)
    // ====================
    @Test
    @SuppressWarnings("unchecked")
    public void getUserList_returnsUsersLoadedFromFile() {
        UserHandler service = UserHandler.getInstance();

        ResponseDTO response = service.getUserList();

        assertTrue(response.isSuccess());
        assertEquals("조회 성공", response.getMessage());
        assertInstanceOf(List.class, response.getPayload());

        List<UserDTO.Response> users = (List<UserDTO.Response>) response.getPayload();
        assertEquals(3, users.size());
        assertEquals("admin", users.get(0).getId());
        assertEquals(Role.ADMIN, users.get(0).getRole());
        assertEquals("Admin User", users.get(0).getName());
    }

    // ====================
    // [TC-07] 사용자관리-사용자추가 - 초기 비밀번호는 ID와 동일하게 설정
    // ====================
    @Test
    public void addUser_addsStudentWithInitialPasswordEqualToId() {
        UserHandler service = UserHandler.getInstance();
        UserDTO.Request request = new UserDTO.Request("ADD_USER", "20240001", Role.STUDENT, "Student One");

        ResponseDTO response = service.addUser(request);
        User savedUser = service.getUserById("20240001");
        int userCount = getUserCount(service);

        assertTrue(response.isSuccess());
        assertEquals("가입 성공!", response.getMessage());
        assertEquals(4, userCount);
        assertNotNull(savedUser);
        assertEquals(Role.STUDENT, savedUser.getRole());
        assertEquals("Student One", savedUser.getName());
        assertTrue(savedUser.verifyPassword("20240001"));
    }

    // ====================
    // [TC-08] 사용자관리-사용자추가 - 중복 ID로 추가 시 실패
    // ====================
    @Test
    public void addUser_failsWhenIdAlreadyExists() {
        UserHandler service = UserHandler.getInstance();
        UserDTO.Request request = new UserDTO.Request("ADD_USER", "23456", Role.ASSISTANT, "Duplicate User");

        ResponseDTO response = service.addUser(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("이미 존재하는 아이디"));
        assertEquals(3, getUserCount(service));
    }

    
    // ====================
    // [TC-09] 사용자관리-사용자추가 - ID 형식이 권한과 불일치 시 실패
    // ====================
    @Test
    public void addUser_failsWhenIdFormatDoesNotMatchRole() {
        UserHandler service = UserHandler.getInstance();
        UserDTO.Request request = new UserDTO.Request("ADD_USER", "1234", Role.PROFESSOR, "Invalid Professor");

        ResponseDTO response = service.addUser(request);

        assertFalse(response.isSuccess());
        assertEquals("가입 실패: 교직원 ID는 5자리 숫자여야 합니다.", response.getMessage());
        assertEquals(3, getUserCount(service));
        assertNull(service.getUserById("1234"));
    }

    // ====================
    // [TC-10] 사용자관리-사용자삭제 - 일반 사용자 정상 삭제
    // ====================
    @Test
    public void deleteUser_deletesExistingNonAdminUser() {
        UserHandler service = UserHandler.getInstance();
        UserDTO.Request request = new UserDTO.Request();
        request.setId("23456");

        ResponseDTO response = service.deleteUser(request);
        int userCount = getUserCount(service);

        assertTrue(response.isSuccess());
        assertEquals("삭제 성공!", response.getMessage());
        assertEquals(2, userCount);
        assertNull(service.getUserById("23456"));
    }

    // ====================
    // [TC-11] 사용자관리-사용자삭제 - 존재하지 않는 사용자 삭제 시 실패
    // ====================
    @Test
    public void deleteUser_failsWhenUserDoesNotExist() {
        UserHandler service = UserHandler.getInstance();
        UserDTO.Request request = new UserDTO.Request();
        request.setId("99999");

        ResponseDTO response = service.deleteUser(request);

        assertFalse(response.isSuccess());
        assertEquals("삭제 실패: 존재하지 않는 계정입니다.", response.getMessage());
        assertEquals(3, getUserCount(service));
    }

    // ====================
    // [TC-11] 사용자관리-사용자삭제 - 관리자 계정 삭제 시도 시 실패
    // ====================
    @Test
    public void deleteUser_failsWhenUserIsAdmin() {
        UserHandler service = UserHandler.getInstance();
        UserDTO.Request request = new UserDTO.Request();
        request.setId("admin");

        ResponseDTO response = service.deleteUser(request);

        assertFalse(response.isSuccess());
        assertEquals("삭제 실패: 최고 관리자 계정은 삭제할 수 없습니다.", response.getMessage());
        assertEquals(3, getUserCount(service));
        assertNotNull(service.getUserById("admin"));
    }

    @Test
    public void getUserById_returnsUserWhenIdExists() {
        UserHandler service = UserHandler.getInstance();

        User user = service.getUserById("34567");

        assertNotNull(user);
        assertEquals(Role.PROFESSOR, user.getRole());
        assertEquals("Professor User", user.getName());
    }

    @Test
    public void getUserById_returnsNullWhenIdDoesNotExist() {
        UserHandler service = UserHandler.getInstance();

        User user = service.getUserById("00000");

        assertNull(user);
    }

    @SuppressWarnings("unchecked")
    private static int getUserCount(UserHandler service) {
        return ((List<UserDTO.Response>) service.getUserList().getPayload()).size();
    }
}

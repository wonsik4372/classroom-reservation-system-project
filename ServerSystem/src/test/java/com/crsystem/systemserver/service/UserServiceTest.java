package com.crsystem.systemserver.service;

import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.dto.UserDTO;
import com.crsystem.common.enums.Role;
import com.crsystem.systemserver.model.User;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest extends BaseUserFileTest {

    @Test
    @SuppressWarnings("unchecked")
    public void getUserList_returnsUsersLoadedFromFile() {
        UserService service = UserService.getInstance();

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

    @Test
    public void addUser_addsStudentWithInitialPasswordEqualToId() {
        UserService service = UserService.getInstance();
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

    @Test
    public void addUser_failsWhenIdAlreadyExists() {
        UserService service = UserService.getInstance();
        UserDTO.Request request = new UserDTO.Request("ADD_USER", "23456", Role.ASSISTANT, "Duplicate User");

        ResponseDTO response = service.addUser(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("이미 존재하는 아이디"));
        assertEquals(3, getUserCount(service));
    }

    @Test
    public void addUser_failsWhenIdFormatDoesNotMatchRole() {
        UserService service = UserService.getInstance();
        UserDTO.Request request = new UserDTO.Request("ADD_USER", "1234", Role.PROFESSOR, "Invalid Professor");

        ResponseDTO response = service.addUser(request);

        assertFalse(response.isSuccess());
        assertEquals("가입 실패: 교직원 ID는 5자리 숫자여야 합니다.", response.getMessage());
        assertEquals(3, getUserCount(service));
        assertNull(service.getUserById("1234"));
    }

    @Test
    public void deleteUser_deletesExistingNonAdminUser() {
        UserService service = UserService.getInstance();
        UserDTO.Request request = new UserDTO.Request();
        request.setId("23456");

        ResponseDTO response = service.deleteUser(request);
        int userCount = getUserCount(service);

        assertTrue(response.isSuccess());
        assertEquals("삭제 성공!", response.getMessage());
        assertEquals(2, userCount);
        assertNull(service.getUserById("23456"));
    }

    @Test
    public void deleteUser_failsWhenUserDoesNotExist() {
        UserService service = UserService.getInstance();
        UserDTO.Request request = new UserDTO.Request();
        request.setId("99999");

        ResponseDTO response = service.deleteUser(request);

        assertFalse(response.isSuccess());
        assertEquals("삭제 실패: 존재하지 않는 계정입니다.", response.getMessage());
        assertEquals(3, getUserCount(service));
    }

    @Test
    public void deleteUser_failsWhenUserIsAdmin() {
        UserService service = UserService.getInstance();
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
        UserService service = UserService.getInstance();

        User user = service.getUserById("34567");

        assertNotNull(user);
        assertEquals(Role.PROFESSOR, user.getRole());
        assertEquals("Professor User", user.getName());
    }

    @Test
    public void getUserById_returnsNullWhenIdDoesNotExist() {
        UserService service = UserService.getInstance();

        User user = service.getUserById("00000");

        assertNull(user);
    }

    @SuppressWarnings("unchecked")
    private static int getUserCount(UserService service) {
        return ((List<UserDTO.Response>) service.getUserList().getPayload()).size();
    }
}

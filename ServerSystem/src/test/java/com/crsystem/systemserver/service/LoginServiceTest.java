package com.crsystem.systemserver.service;

import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.dto.UserDTO;
import com.crsystem.common.enums.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LoginServiceTest extends BaseUserFileTest {

    @Test
    public void processLogin_succeedsWhenIdRoleAndPasswordMatch() {
        LoginService service = LoginService.getInstance();
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

    @Test
    public void processLogin_allowsAssistantToLoginAsProfessorScreen() {
        LoginService service = LoginService.getInstance();
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

    @Test
    public void processLogin_failsWhenUserDoesNotExist() {
        LoginService service = LoginService.getInstance();
        UserDTO.Request request = new UserDTO.Request("LOGIN", "99999", "99999");
        request.setRole(Role.PROFESSOR);

        ResponseDTO response = service.processLogin(request);

        assertFalse(response.isSuccess());
        assertEquals("로그인 실패: 존재하지 않는 계정입니다.", response.getMessage());
        assertNull(response.getPayload());
    }

    @Test
    public void processLogin_failsWhenRoleDoesNotMatch() {
        LoginService service = LoginService.getInstance();
        UserDTO.Request request = new UserDTO.Request("LOGIN", "34567", "34567");
        request.setRole(Role.STUDENT);

        ResponseDTO response = service.processLogin(request);

        assertFalse(response.isSuccess());
        assertEquals("로그인 실패: 해당 권한으로 로그인할 수 없습니다.", response.getMessage());
        assertNull(response.getPayload());
    }

    @Test
    public void processLogin_failsWhenPasswordDoesNotMatch() {
        LoginService service = LoginService.getInstance();
        UserDTO.Request request = new UserDTO.Request("LOGIN", "34567", "wrong-password");
        request.setRole(Role.PROFESSOR);

        ResponseDTO response = service.processLogin(request);

        assertFalse(response.isSuccess());
        assertEquals("로그인 실패: 비밀번호가 일치하지 않습니다.", response.getMessage());
        assertNull(response.getPayload());
    }
}

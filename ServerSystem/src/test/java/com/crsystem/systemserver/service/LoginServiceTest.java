package com.crsystem.systemserver.service;

import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.dto.UserDTO;
import com.crsystem.common.enums.Role;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LoginServiceTest {

    private static final Path USER_FILE = Path.of("src/main/resources/masterfile/User.json");
    private static Path backupFile;
    private static boolean originalFileExists;

    @BeforeAll
    public static void backUpUserFile() throws IOException {
        originalFileExists = Files.exists(USER_FILE);

        if (originalFileExists) {
            backupFile = Files.createTempFile("User", ".json");
            Files.copy(USER_FILE, backupFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @AfterAll
    public static void restoreUserFileAfterAllTests() throws IOException {
        restoreUserFile();

        if (backupFile != null) {
            Files.deleteIfExists(backupFile);
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        writeTestUsers();
        resetSingleton(UserService.class);
        resetSingleton(LoginService.class);
    }

    @AfterEach
    public void tearDown() throws Exception {
        restoreUserFile();
        resetSingleton(UserService.class);
        resetSingleton(LoginService.class);
    }

    @Test
    public void getInstance_returnsSingletonInstance() {
        // LoginService가 싱글톤으로 같은 인스턴스를 반환하는지 검증
        LoginService first = LoginService.getInstance();
        LoginService second = LoginService.getInstance();

        assertNotNull(first);
        assertSame(first, second);
    }

    @Test
    public void processLogin_succeedsWhenIdRoleAndPasswordMatch() {
        // ID, 권한, 비밀번호가 모두 일치하면 로그인 성공 응답과 사용자 정보가 반환되는지 검증
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
        // 조교는 교수 화면 권한으로 로그인할 수 있는 예외 규칙이 적용되는지 검증
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
        // 존재하지 않는 ID로 로그인하면 실패 응답이 반환되는지 검증
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
        // 사용자 권한과 요청 권한이 맞지 않으면 로그인이 거부되는지 검증
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
        // 비밀번호가 일치하지 않으면 로그인 실패 응답이 반환되는지 검증
        LoginService service = LoginService.getInstance();
        UserDTO.Request request = new UserDTO.Request("LOGIN", "34567", "wrong-password");
        request.setRole(Role.PROFESSOR);

        ResponseDTO response = service.processLogin(request);

        assertFalse(response.isSuccess());
        assertEquals("로그인 실패: 비밀번호가 일치하지 않습니다.", response.getMessage());
        assertNull(response.getPayload());
    }

    private static void writeTestUsers() throws IOException {
        Files.createDirectories(USER_FILE.getParent());
        Files.writeString(USER_FILE, """
                [
                  {
                    "role": "ADMIN",
                    "id": "admin",
                    "pw": "admin",
                    "name": "Admin User"
                  },
                  {
                    "role": "ASSISTANT",
                    "id": "23456",
                    "pw": "23456",
                    "name": "Assistant User"
                  },
                  {
                    "role": "PROFESSOR",
                    "id": "34567",
                    "pw": "34567",
                    "name": "Professor User"
                  }
                ]
                """);
    }

    private static void restoreUserFile() throws IOException {
        if (originalFileExists) {
            Files.copy(backupFile, USER_FILE, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        Files.deleteIfExists(USER_FILE);
    }

    private static void resetSingleton(Class<?> serviceClass) throws Exception {
        Field instanceField = serviceClass.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }
}

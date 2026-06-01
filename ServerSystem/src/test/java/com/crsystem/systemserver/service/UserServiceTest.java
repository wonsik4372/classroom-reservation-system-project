package com.crsystem.systemserver.service;

import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.dto.UserDTO;
import com.crsystem.common.enums.Role;
import com.crsystem.systemserver.model.User;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

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
        resetUserServiceSingleton();
    }

    @AfterEach
    public void tearDown() throws Exception {
        restoreUserFile();
        resetUserServiceSingleton();
    }

    @Test
    public void getInstance_returnsSingletonInstance() {
        // UserService가 싱글톤으로 같은 인스턴스를 반환하는지 검증
        UserService first = UserService.getInstance();
        UserService second = UserService.getInstance();

        assertNotNull(first);
        assertSame(first, second);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getUserList_returnsUsersLoadedFromFile() {
        // 파일에서 불러온 사용자 목록이 응답 DTO에 정상적으로 담기는지 검증
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
        // 신규 학생 사용자를 추가하면 목록에 반영되고 초기 비밀번호가 ID로 설정되는지 검증
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
        // 이미 존재하는 ID로 사용자를 추가하려고 하면 실패하고 기존 목록이 유지되는지 검증
        UserService service = UserService.getInstance();
        UserDTO.Request request = new UserDTO.Request("ADD_USER", "23456", Role.ASSISTANT, "Duplicate User");

        ResponseDTO response = service.addUser(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("이미 존재하는 아이디"));
        assertEquals(3, getUserCount(service));
    }

    @Test
    public void addUser_failsWhenIdFormatDoesNotMatchRole() {
        // 역할에 맞지 않는 ID 형식이면 사용자 추가가 실패하는지 검증
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
        // 존재하는 일반 사용자를 삭제하면 목록에서 제거되는지 검증
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
        // 존재하지 않는 사용자를 삭제하려고 하면 실패하고 기존 목록이 유지되는지 검증
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
        // 관리자 계정은 삭제할 수 없고 기존 목록에 남아 있는지 검증
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
        // 존재하는 ID로 조회하면 해당 사용자 정보를 반환하는지 검증
        UserService service = UserService.getInstance();

        User user = service.getUserById("34567");

        assertNotNull(user);
        assertEquals(Role.PROFESSOR, user.getRole());
        assertEquals("Professor User", user.getName());
    }

    @Test
    public void getUserById_returnsNullWhenIdDoesNotExist() {
        // 존재하지 않는 ID로 조회하면 null을 반환하는지 검증
        UserService service = UserService.getInstance();

        User user = service.getUserById("00000");

        assertNull(user);
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

    @SuppressWarnings("unchecked")
    private static int getUserCount(UserService service) {
        return ((List<UserDTO.Response>) service.getUserList().getPayload()).size();
    }

    private static void restoreUserFile() throws IOException {
        if (originalFileExists) {
            Files.copy(backupFile, USER_FILE, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        Files.deleteIfExists(USER_FILE);
    }

    private static void resetUserServiceSingleton() throws Exception {
        Field instanceField = UserService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }
}

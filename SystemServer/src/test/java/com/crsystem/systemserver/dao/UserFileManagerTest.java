package com.crsystem.systemserver.dao;

import com.crsystem.common.enums.Role;
import com.crsystem.systemserver.model.User;
import java.io.IOException;
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

public class UserFileManagerTest {

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
    public void setUp() throws IOException {
        writeTestUsers();
    }

    @AfterEach
    public void tearDown() throws IOException {
        restoreUserFile();
    }

    // ====================
    // [TC-42] 데이터관리 - 복구 시 JSON 파일로부터 사용자 목록 정상 로드
    // ====================
    @Test
    public void loadAll_returnsUsersFromJsonFile() {
        // User.json에 저장된 사용자 목록을 User 객체 목록으로 읽어오는지 검증
        UserFileManager fileManager = new UserFileManager();

        List<User> users = fileManager.loadAll();

        assertEquals(2, users.size());
        assertEquals("admin", users.get(0).getId());
        assertEquals(Role.ADMIN, users.get(0).getRole());
        assertEquals("Admin User", users.get(0).getName());
        assertEquals("23456", users.get(1).getId());
        assertEquals(Role.ASSISTANT, users.get(1).getRole());
        assertEquals("Assistant User", users.get(1).getName());
    }

    // ====================
    // [TC-42] 데이터관리 - 파일 없을 때 예외 없이 빈 목록 반환
    // ====================
    @Test
    public void loadAll_returnsEmptyListWhenFileDoesNotExist() throws IOException {
        // User.json 파일이 없을 때 예외 없이 빈 목록을 반환하는지 검증
        Files.deleteIfExists(USER_FILE);
        UserFileManager fileManager = new UserFileManager();

        List<User> users = fileManager.loadAll();

        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    // ====================
    // [TC-43] 데이터관리 - 데이터 복구 요청 시 경고 후 파일 덮어쓰기
    // ====================
    @Test
    public void saveAll_overwritesJsonFileWithGivenUsers() {
        // saveAll 호출 시 기존 파일 내용이 전달한 사용자 목록으로 덮어써지는지 검증
        UserFileManager fileManager = new UserFileManager();
        List<User> newUsers = List.of(
                new User("20240001", Role.STUDENT, "Student One"),
                new User("34567", Role.PROFESSOR, "Professor User")
        );

        fileManager.saveAll(newUsers);
        List<User> savedUsers = fileManager.loadAll();

        assertEquals(2, savedUsers.size());
        assertEquals("20240001", savedUsers.get(0).getId());
        assertEquals(Role.STUDENT, savedUsers.get(0).getRole());
        assertEquals("Student One", savedUsers.get(0).getName());
        assertEquals("34567", savedUsers.get(1).getId());
        assertEquals(Role.PROFESSOR, savedUsers.get(1).getRole());
        assertEquals("Professor User", savedUsers.get(1).getName());
    }

    // ====================
    // [TC-44] 데이터관리 - 저장 시 백업 파일 생성 (신규 사용자 추가 후 반영)
    // ====================
    @Test
    public void add_appendsUserToJsonFile() {
        // add 호출 시 기존 사용자 목록 뒤에 신규 사용자가 추가 저장되는지 검증
        UserFileManager fileManager = new UserFileManager();
        User newUser = new User("20240001", Role.STUDENT, "Student One");

        fileManager.add(newUser);
        List<User> users = fileManager.loadAll();

        assertEquals(3, users.size());
        assertTrue(containsUserId(users, "20240001"));
    }

    // ====================
    // [TC-45] 데이터관리 - 프로그램 종료 후 재시작 시 데이터 유지
    // ====================
    @Test
    public void delete_removesUserWithMatchingIdFromJsonFile() {
        // delete 호출 시 전달한 ID와 일치하는 사용자가 파일에서 제거되는지 검증
        UserFileManager fileManager = new UserFileManager();

        fileManager.delete("23456");
        List<User> users = fileManager.loadAll();

        assertEquals(1, users.size());
        assertFalse(containsUserId(users, "23456"));
        assertTrue(containsUserId(users, "admin"));
    }

    // ====================
    // [TC-46] 데이터관리 - 초기 실행 시 기본 데이터 생성 (존재하지 않는 ID 삭제 시 목록 유지)
    // ====================
    @Test
    public void delete_keepsListUnchangedWhenIdDoesNotExist() {
        // 존재하지 않는 ID를 삭제하려고 하면 기존 사용자 목록이 유지되는지 검증
        UserFileManager fileManager = new UserFileManager();

        fileManager.delete("99999");
        List<User> users = fileManager.loadAll();

        assertEquals(2, users.size());
        assertTrue(containsUserId(users, "admin"));
        assertTrue(containsUserId(users, "23456"));
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
                  }
                ]
                """);
    }

    private static boolean containsUserId(List<User> users, String id) {
        return users.stream().anyMatch(user -> user.getId().equals(id));
    }

    private static void restoreUserFile() throws IOException {
        if (originalFileExists) {
            Files.copy(backupFile, USER_FILE, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        Files.deleteIfExists(USER_FILE);
    }
}

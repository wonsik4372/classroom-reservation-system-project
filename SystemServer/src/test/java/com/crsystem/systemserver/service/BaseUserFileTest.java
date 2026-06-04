package com.crsystem.systemserver.service;

import com.crsystem.systemserver.dao.ServerPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

abstract class BaseUserFileTest {

    static final Path USER_FILE = Path.of(ServerPaths.USER_JSON);
    private static Path backupFile;
    private static boolean originalFileExists;

    @BeforeAll
    static void backUpUserFile() throws IOException {
        originalFileExists = Files.exists(USER_FILE);
        if (originalFileExists) {
            backupFile = Files.createTempFile("User", ".json");
            Files.copy(USER_FILE, backupFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @AfterAll
    static void restoreUserFileAfterAllTests() throws IOException {
        restoreUserFile();
        if (backupFile != null) {
            Files.deleteIfExists(backupFile);
        }
    }

    @BeforeEach
    void setUpFile() throws IOException {
        writeTestUsers();
        UserHandler.getInstance().reloadForTesting();
    }

    @AfterEach
    void tearDownFile() throws IOException {
        restoreUserFile();
        UserHandler.getInstance().reloadForTesting();
    }

    static void writeTestUsers() throws IOException {
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
}

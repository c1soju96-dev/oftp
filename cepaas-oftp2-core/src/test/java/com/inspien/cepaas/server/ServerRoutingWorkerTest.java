package com.inspien.cepaas.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.neociclo.odetteftp.protocol.OdetteFtpObject;
import org.neociclo.odetteftp.util.IoUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ServerRoutingWorkerTest {

    @TempDir
    File tempDir;

    @Test
    void testIoUtilMove() throws IOException {
        File testFile = new File("src/test/resources/test.vfile");

        File sourceDir = new File(tempDir, "user123/work");
        sourceDir.mkdirs();
        File destDir = new File(tempDir, "recipient123/mailbox");
        destDir.mkdirs();
        
        // 직접 파일 이동 테스트
        File sourceFile = new File(sourceDir, "test.vfile");
        Files.copy(testFile.toPath(), sourceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        File destFile = new File(destDir, "test.vfile");
        IoUtil.move(sourceFile, destFile);

        // 파일이 이동되었는지 확인
        assertTrue(destFile.exists(), "The file should be moved to the recipient's mailbox");
    }

    @Test
    void testEnsureDirectoryExistsCreatesDirectoryIfNotExist() {
        // Given
        File dir = new File(tempDir, "newDir");
        assertFalse(dir.exists(), "Directory should not exist before the test");

        // When
        ServerRoutingWorker.MakeDeliveryTask task = new ServerRoutingWorker.MakeDeliveryTask(tempDir, "user123", mock(OdetteFtpObject.class));
        task.ensureDirectoryExists(dir);

        // Then
        assertTrue(dir.exists(), "Directory should be created");
    }

    @Test
    void testEnsureDirectoryExistsDoesNothingIfDirectoryExists() {
        // Given
        File dir = new File(tempDir, "existingDir");
        dir.mkdirs();
        assertTrue(dir.exists(), "Directory should exist before the test");

        // When
        ServerRoutingWorker.MakeDeliveryTask task = new ServerRoutingWorker.MakeDeliveryTask(tempDir, "user123", mock(OdetteFtpObject.class));
        task.ensureDirectoryExists(dir);

        // Then
        assertTrue(dir.exists(), "Directory should still exist");
    }

}

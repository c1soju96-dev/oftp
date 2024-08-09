package com.inspien.cepaas.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.neociclo.odetteftp.protocol.OdetteFtpObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerRoutingWorkerTest {

    private ServerRoutingWorker serverRoutingWorker;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        serverRoutingWorker = new ServerRoutingWorker();
    }

    @Test
    void testDeliverSuccessful() throws IOException, InterruptedException {
        // Given
        String userCode = "user123";
        String recipientOid = "recipient123";
        OdetteFtpObject obj = mock(OdetteFtpObject.class);
        when(obj.getDestination()).thenReturn(recipientOid);

        File sourceDir = new File(tempDir, "user123/work");
        sourceDir.mkdirs();
        File destDir = new File(tempDir, "recipient123/mailbox");
        destDir.mkdirs();

        // Copy test.vfile from the test resources to the sourceDir
        File testFile = new File("src/test/resources/test.vfile");
        File sourceFile = new File(sourceDir, "mockedFile.vfile");
        Files.copy(testFile.toPath(), sourceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        try (MockedStatic<OftpServerHelper> mockedHelper = Mockito.mockStatic(OftpServerHelper.class)) {
            // Mock the createFileName method to avoid issues during test execution
            //mockedHelper.when(() -> OftpServerHelper.createFileName(obj)).thenReturn("mockedFile.vfile");
            mockedHelper.when(() -> OftpServerHelper.createFileName(Mockito.any(OdetteFtpObject.class)))
                        .thenReturn("mockedFile.vfile");

            // Mock other necessary static methods
            mockedHelper.when(() -> OftpServerHelper.getUserWorkDir(tempDir, userCode)).thenReturn(sourceDir);
            mockedHelper.when(() -> OftpServerHelper.getUserMailboxDir(tempDir, recipientOid)).thenReturn(destDir);

            // When
            serverRoutingWorker.deliver(tempDir, userCode, obj);
            Thread.sleep(1000); // 비동기 작업이 완료되기 위해 잠시 대기

            // Then
            File destFile = new File(destDir, "mockedFile.vfile");
            assertTrue(destFile.exists(), "The file should be moved to the recipient's mailbox");
        }
    }

}

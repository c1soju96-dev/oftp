package com.inspien.cepaas.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.neociclo.odetteftp.protocol.OdetteFtpObject;
import org.neociclo.odetteftp.protocol.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OftpServerUtilTest {

    @TempDir
    File tempDir;

    private OdetteFtpObject odetteFtpObject;
    private VirtualFile virtualFile;

    @BeforeEach
    void setUp() {
        odetteFtpObject = mock(OdetteFtpObject.class);
        virtualFile = mock(VirtualFile.class);
    }

    @Test
    void testCreateUserDirStructureIfNotExist() {
        // Given
        String userCode = "user1234";
        File dataDir = new File(tempDir, "data");
        File mailboxDir = new File(tempDir, userCode + "/mailbox");
        File workDir = new File(tempDir, userCode + "/work");

        // When
        OftpServerUtil.createUserDirStructureIfNotExist(userCode, tempDir);

        // Then
        assertTrue(dataDir.exists(), "Data directory should exist");
        assertTrue(mailboxDir.exists(), "Mailbox directory should exist");
        assertTrue(workDir.exists(), "Work directory should exist");
    }

    @Test
    void testCreateFileName() {
        // Given
        UUID testUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        when(odetteFtpObject.getOriginator()).thenReturn("originator");
        when(odetteFtpObject.getDestination()).thenReturn("destination");
        when(odetteFtpObject.getDatasetName()).thenReturn("dataset");
        when(odetteFtpObject.getDateTime()).thenReturn(new Date());

        // When
        String fileName = OftpServerUtil.createFileName(odetteFtpObject, () -> testUUID);

        // Then
        assertTrue(fileName.contains("123e4567-e89b-12d3-a456-426614174000"), "UUID should be present in the filename");
        assertTrue(fileName.contains("originator"), "Originator should be present in the filename");
        assertTrue(fileName.contains("destination"), "Destination should be present in the filename");
    }

    @Test
    void testCreateDataFile() throws IOException {
        // Given
        UUID testUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        when(virtualFile.getOriginator()).thenReturn("originator");
        when(virtualFile.getDestination()).thenReturn("destination");
        when(virtualFile.getDatasetName()).thenReturn("dataset");
        when(virtualFile.getDateTime()).thenReturn(new Date());
        OftpServerUtil.createUserDirStructureIfNotExist("user1234", tempDir);

        // When
        File dataFile = OftpServerUtil.createDataFile(virtualFile, tempDir, () -> testUUID);

        // Then
        assertTrue(dataFile.exists(), "Data file should be created");
        assertTrue(dataFile.getName().contains("123e4567-e89b-12d3-a456-426614174000"), "UUID should be part of the filename");
    }

    @Test
    void testStoreAndLoadObject_WithSerializableMock() throws IOException {
        // Given
        File outputFile = new File(tempDir, "test.vfile");
        odetteFtpObject = mock(OdetteFtpObject.class, withSettings().serializable());
        when(odetteFtpObject.getOriginator()).thenReturn("originator");
        when(odetteFtpObject.getDestination()).thenReturn("destination");
        when(odetteFtpObject.getDatasetName()).thenReturn("dataset");
        when(odetteFtpObject.getDateTime()).thenReturn(new Date());

        // When
        OftpServerUtil.storeObject(outputFile, odetteFtpObject);
        OdetteFtpObject loadedObject = OftpServerUtil.loadObject(outputFile);

        // Then
        assertNotNull(loadedObject, "Loaded object should not be null");
    }

    @Test
    void testListExchanges() throws IOException {
        // Given
        String userCode = "user123";
        File mailboxDir = new File(tempDir, "user123/mailbox");
        mailboxDir.mkdirs();

        // Create test files
        File file1 = new File(mailboxDir, "file1.vfile");
        File file2 = new File(mailboxDir, "file2.notif");
        file1.createNewFile();
        file2.createNewFile();

        // When
        File[] exchanges = OftpServerUtil.listExchanges(userCode, tempDir);

        // Then
        assertNotNull(exchanges);
        assertEquals(2, exchanges.length, "Should list both vfile and notif files");
    }

    @Test
    void testDeleteExchange() throws IOException {
        // Given
        String userCode = "user123";
        File mailboxDir = new File(tempDir, "user123/mailbox");
        mailboxDir.mkdirs();

        // Create a test file
        File testFile = new File(mailboxDir, "test.vfile");
        testFile.createNewFile();
        when(virtualFile.getFile()).thenReturn(testFile);

        // When
        OftpServerUtil.deleteExchange(userCode, virtualFile, tempDir);

        // Then
        assertFalse(testFile.exists(), "The file should be deleted");
    }

    @Test
    void testHasExchange() throws IOException {
        // Given
        String userCode = "user123";
        File mailboxDir = new File(tempDir, "user123/mailbox");
        mailboxDir.mkdirs();

        // Create a test file
        File testFile = new File(mailboxDir, "test.vfile");
        testFile.createNewFile();

        // When
        boolean hasExchange = OftpServerUtil.hasExchange(userCode, tempDir);

        // Then
        assertTrue(hasExchange, "Should return true when there is an exchange");
    }
}

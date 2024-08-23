package com.inspien.cepaas.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neociclo.odetteftp.service.TcpServer;

import com.inspien.cepaas.exception.InvalidPasswordException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class OftpServerManagerTest {

    private OftpServerManager serverManager;

    @BeforeEach
    void setUp() {
        serverManager = new OftpServerManager(
                "serverDir", false, 8080,
                "keystorePath", "keystorePassword",
                "ssid", "password"
        );
    }

    @Test
    void testStartServerWithInvalidPassword() {
        serverManager = new OftpServerManager(
                "serverDir", true, 8080,
                "keystorePath", "keystorePassword",
                "ssid", ""
        );

        assertThrows(InvalidPasswordException.class, serverManager::startServer);
    }

    @Test
    void testStopServer() throws Exception {
        // Mock the server to simulate stopping it
        TcpServer mockServer = mock(TcpServer.class);
        
        // Create a spy of the serverManager to mock the isRunning method
        OftpServerManager spyServerManager = spy(serverManager);
        
        // Mock the isRunning method to return true
        doReturn(true).when(spyServerManager).isRunning();

        // Use reflection to set the private 'server' field
        Field serverField = OftpServerManager.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(spyServerManager, mockServer);

        // Call the method under test
        spyServerManager.stopServer();

        // Verify that the stop method was called on the mocked server
        verify(mockServer, times(1)).stop();
    }

}

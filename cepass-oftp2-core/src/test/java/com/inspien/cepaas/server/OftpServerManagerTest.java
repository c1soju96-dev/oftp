package com.inspien.cepaas.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.neociclo.odetteftp.service.TcpServer;
import org.neociclo.odetteftp.support.OdetteFtpConfiguration;
import org.neociclo.odetteftp.support.PropertiesBasedConfiguration;

import com.inspien.cepaas.exception.InvalidPasswordException;
import com.inspien.cepaas.util.OftpServerUtil;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OftpServerManagerTest {

    private OftpServerManager serverManager;

    @BeforeEach
    void setUp() {
        serverManager = new OftpServerManager(
                "serverDir", false, 8080,
                "keystorePath", "keystorePassword",
                9090, "ssid", "password"
        );
    }

    // @Test
    // void testStartServerWithValidConfig() throws Exception {
    //     try (MockedStatic<SSLContext> sslContextMock = mockStatic(SSLContext.class);
    //          MockedStatic<OftpServerUtil> oftpHelperMock = mockStatic(OftpServerUtil.class)) {

    //         SSLContext sslContext = mock(SSLContext.class);
    //         sslContextMock.when(() -> SSLContext.getInstance("TLS")).thenReturn(sslContext);

    //         serverManager.startServer();

    //         sslContextMock.verify(() -> SSLContext.getInstance("TLS"), times(1));
    //         oftpHelperMock.verify(() -> OftpServerUtil.createFileName(any(), UUID::randomUUID), times(0));
    //     }
    // }

    @Test
    void testStartServerWithInvalidPassword() {
        serverManager = new OftpServerManager(
                "serverDir", true, 8080,
                "keystorePath", "keystorePassword",
                9090, "ssid", ""
        );

        assertThrows(InvalidPasswordException.class, serverManager::startServer);
    }

    // @Test
    // void testNonTlsServerSetup() throws Exception {
    //     serverManager = new OftpServerManager(
    //             "serverDir", false, 0,
    //             null, null,
    //             9090, "ssid", "sfid", "password"
    //     );

    //     try (MockedStatic<PropertiesBasedConfiguration> configMock = mockStatic(PropertiesBasedConfiguration.class)) {
    //         PropertiesBasedConfiguration mockConfig = mock(PropertiesBasedConfiguration.class);
    //         configMock.when(PropertiesBasedConfiguration::new).thenReturn(mockConfig);

    //         serverManager.startServer();

    //         verify(mockConfig, times(1)).load(any(FileInputStream.class));
    //     }
    // }

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

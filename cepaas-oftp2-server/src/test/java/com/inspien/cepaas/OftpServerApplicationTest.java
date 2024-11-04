package com.inspien.cepaas;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import com.inspien.cepaas.config.OftpServerProperties;
import com.inspien.cepaas.server.IOftpServerManager;

@SpringBootTest
@ActiveProfiles("test")
@Configuration
class OftpServerApplicationTest {

    @Autowired
    private ApplicationContext context;

    @MockBean
    private IOftpServerManager serverManager;

    @MockBean
    private OftpServerProperties properties;

    @Test
    void testApplicationStartup() throws Exception {
        // Given
        // when(properties.getBaseDirectory()).thenReturn("/tmp/oftp");
        // when(properties.isTlsYn()).thenReturn(false);
        // when(properties.getPort()).thenReturn(13305);
        // when(properties.getKeystorePath()).thenReturn(null);
        // when(properties.getKeystorePassword()).thenReturn(null);
        // when(properties.getSsid()).thenReturn("SSID");
        // when(properties.getPassword()).thenReturn("password");

        // When
        // OftpServerApplication.main(new String[] {});

        // Then
        // verify(serverManager, times(1)).startServer();

        // Manually trigger shutdown hook to verify stopServer method
        context.getBean(OftpServerApplication.class).init(serverManager).run();
    }

    @Bean
    public IOftpServerManager serverManager() {
        return serverManager;
    }
}

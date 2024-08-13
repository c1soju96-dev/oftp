package com.inspien.cepaas.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class OftpServerPropertiesTest {

    @Mock
    private OftpServerProperties.TlsConfig tlsConfig;

    @Mock
    private OftpServerProperties.NonTlsConfig nonTlsConfig;

    @Mock
    private OftpServerProperties.AuthConfig authConfig;

    @InjectMocks
    private OftpServerProperties oftpServerProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mocking TlsConfig
        when(tlsConfig.isActive()).thenReturn(true);
        when(tlsConfig.getPort()).thenReturn(1234);
        when(tlsConfig.getKeystorePath()).thenReturn("/path/to/keystore");
        when(tlsConfig.getKeystorePassword()).thenReturn("keystorePassword");

        // Mocking NonTlsConfig
        when(nonTlsConfig.isActive()).thenReturn(true);
        when(nonTlsConfig.getPort()).thenReturn(5678);

        // Mocking AuthConfig
        when(authConfig.getSsid()).thenReturn("ssidValue");
        when(authConfig.getPassword()).thenReturn("passwordValue");

        // Assign mocked configs to oftpServerProperties
        oftpServerProperties.setTls(tlsConfig);
        oftpServerProperties.setNonTls(nonTlsConfig);
        oftpServerProperties.setAuth(authConfig);
        oftpServerProperties.setServerDir("/path/to/serverDir");
    }

    @Test
    void testOftpServerPropertiesLoaded() {
        assertNotNull(oftpServerProperties, "OftpServerProperties should be loaded");
        assertNotNull(oftpServerProperties.getServerDir(), "ServerDir should not be null");
        assertNotNull(oftpServerProperties.getTls(), "TlsConfig should not be null");
        assertNotNull(oftpServerProperties.getNonTls(), "NonTlsConfig should not be null");
        assertNotNull(oftpServerProperties.getAuth(), "AuthConfig should not be null");

        // Additional assertions to verify specific properties
        assertEquals("/path/to/serverDir", oftpServerProperties.getServerDir());
        assertEquals(true, oftpServerProperties.getTls().isActive());
        assertEquals(1234, oftpServerProperties.getTls().getPort());
        assertEquals("/path/to/keystore", oftpServerProperties.getTls().getKeystorePath());
        assertEquals("keystorePassword", oftpServerProperties.getTls().getKeystorePassword());

        assertEquals(true, oftpServerProperties.getNonTls().isActive());
        assertEquals(5678, oftpServerProperties.getNonTls().getPort());

        assertEquals("ssidValue", oftpServerProperties.getAuth().getSsid());
        assertEquals("passwordValue", oftpServerProperties.getAuth().getPassword());
    }
}

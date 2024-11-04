package com.inspien.cepaas.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OftpServerPropertiesTest {

    @InjectMocks
    private OftpServerProperties oftpServerProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 설정된 값들을 수동으로 설정
        // oftpServerProperties.setBaseDirectory("/path/to/serverDir");
        // oftpServerProperties.setPort(3309);
        // oftpServerProperties.setTlsYn(false);
        // oftpServerProperties.setKeystorePath("/path/to/keystore");
        // oftpServerProperties.setKeystorePassword("#*.cloudedi.net@");
        // oftpServerProperties.setSsid("NH_SSID");
        // oftpServerProperties.setPassword("NHPASS");
    }

    @Test
    void testOftpServerPropertiesLoaded() {
        // assertNotNull(oftpServerProperties, "OftpServerProperties should be loaded");
        // assertNotNull(oftpServerProperties.getBaseDirectory(), "BaseDirectory should not be null");
        // assertNotNull(oftpServerProperties.getKeystorePath(), "KeystorePath should not be null");
        // assertNotNull(oftpServerProperties.getKeystorePassword(), "KeystorePassword should not be null");
        // assertNotNull(oftpServerProperties.getSsid(), "SSID should not be null");
        // assertNotNull(oftpServerProperties.getPassword(), "Password should not be null");

        // // Base properties assertions
        // assertEquals("/path/to/serverDir", oftpServerProperties.getBaseDirectory());
        // assertEquals(3309, oftpServerProperties.getPort());
        // assertEquals(false, oftpServerProperties.isTlsYn());
        
        // // TLS properties assertions
        // assertEquals("/path/to/keystore", oftpServerProperties.getKeystorePath());
        // assertEquals("#*.cloudedi.net@", oftpServerProperties.getKeystorePassword());
        
        // // Authentication properties assertions
        // assertEquals("NH_SSID", oftpServerProperties.getSsid());
        // assertEquals("NHPASS", oftpServerProperties.getPassword());
    }

}

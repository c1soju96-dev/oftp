package com.inspien.cepaas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "oftpserver")
@Getter
@Setter
public class OftpServerProperties {

    private String serverDir;
    private TlsConfig tls;
    private NonTlsConfig nonTls;
    private AuthConfig auth;

    @Getter
    @Setter
    public static class TlsConfig {
        private boolean active;
        private int port;
        private String keystorePath;
        private String keystorePassword;
    }

    @Getter
    @Setter
    public static class NonTlsConfig {
        private boolean active;
        private int port;
    }

    @Getter
    @Setter
    public static class AuthConfig {
        private String ssid;
        private String password;
    }
}
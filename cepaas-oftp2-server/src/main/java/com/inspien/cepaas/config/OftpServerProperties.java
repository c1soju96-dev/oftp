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

    private String baseDirectory;
    private int port;
    private boolean tlsYn;
    private String keystorePath;
    private String keystorePassword;
    private String ssid;
    private String password;
}
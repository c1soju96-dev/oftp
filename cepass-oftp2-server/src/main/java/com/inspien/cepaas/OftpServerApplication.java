package com.inspien.cepaas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.inspien.cepaas.config.OftpServerProperties;
import com.inspien.cepaas.server.OftpServerManager;

@SpringBootApplication
public class OftpServerApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(OftpServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(OftpServerApplication.class, args);
    }

    @Bean
    CommandLineRunner init(OftpServerManager serverManager) {
        return args -> {
            try {
                serverManager.startServer();
            } catch (Exception e) {
                LOGGER.error("Failed to start OFTP2 server.", e);
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    serverManager.stopServer();
                } catch (Exception e) {
                    LOGGER.error("Failed to stop OFTP2 server", e);
                }
            }));
        };
    }

    @Bean
    public OftpServerManager serverManager(OftpServerProperties properties) {
        String serverDir = properties.getServerDir();
        boolean tlsActive = properties.getTls().isActive();
        int tlsPort = properties.getTls().getPort();
        String keystorePath = properties.getTls().getKeystorePath();
        String keystorePassword = properties.getTls().getKeystorePassword();
        int nonTlsPort = properties.getNonTls().getPort();
        String ssid = properties.getAuth().getSsid();
        String sfid = properties.getAuth().getSfid();
        String password = properties.getAuth().getPassword();

        return new OftpServerManager(serverDir, tlsActive, tlsPort, keystorePath, keystorePassword, nonTlsPort, ssid, sfid, password);
    }
}
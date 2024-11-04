package com.inspien.cepaas;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.inspien.cepaas.config.OftpServerProperties;
import com.inspien.cepaas.config.OftpServerProperties.PhysicalPartner;
import com.inspien.cepaas.enums.ErrorCode;
import com.inspien.cepaas.exception.OftpException;
import com.inspien.cepaas.server.IOftpServerManager;
import com.inspien.cepaas.server.OftpServerManager;

@SpringBootApplication
@EnableScheduling
@EnableAspectJAutoProxy
public class OftpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OftpServerApplication.class, args);
    }

    @Bean
    CommandLineRunner init(IOftpServerManager serverManager) {
        return args -> {
            try {
                serverManager.startServer();
            } catch (Exception e) {
                throw new OftpException(ErrorCode.INVALID_SERVER_SETTING);
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    serverManager.stopServer();
                } catch (Exception e) {
                    throw new OftpException(ErrorCode.INVALID_SERVER_SETTING);
                }
            }));
        };
    }

    @Bean
    public IOftpServerManager serverManager(OftpServerProperties properties) {
        PhysicalPartner physicalPartner = properties.findHomePartner();
        return OftpServerManager.builder()
                .baseDirectory(properties.getBaseDirectory())
                .tlsYn(physicalPartner.isUseTls())
                .port(physicalPartner.getPort())
                .keystorePath(physicalPartner.getKeystorePath())
                .keystorePassword(physicalPartner.getKeystorePassword())
                .ssid(physicalPartner.getSsId())
                .password(physicalPartner.getPassword())
                .build();
    }
}
package com.inspien.cepaas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

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
    private List<PhysicalPartner> physicalPartners;
    private List<LogicalPartner> logicalPartners;

    @Getter
    @Setter
    public static class PhysicalPartner {
        private String partnerSsId;
        private String hostName;
        private int partnerIpPort;
        private boolean partnerSslYn;
        private String partnerSslCertPath;
    }

    @Getter
    @Setter
    public static class LogicalPartner {
        private String partnerSsId;
        private String partnerSfId;
        private String partnerFileEncryptCertPath;
        private String partnerFileSigningCertPath;
        private String partnerEerpSigningCertPath;
        private String messageBoxId;
        private String messageBoxEndPoint;
        private String outBoundSlotId;
        private String inBoundSlotId;
        private String charEncoding;
        private boolean signedEerpRequestYn;
        private boolean fileCompressionYn;
        private boolean fileEncryptionYn;
        private boolean fileSignYn;
    }
}

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
    private List<Partner> partners;

    @Getter
    @Setter
    public static class Partner {
        private String partnerId;
        private PartnerKey partnerKey;
        private Payload payload;
        private Outbound outbound;
        private Inbound inbound;
    }

    @Getter
    @Setter
    public static class PartnerKey {
        private String partnerSignCertPath;
        private String partnerKeystorePath;
        private String partnerKeystorePassword;
    }

    @Getter
    @Setter
    public static class Payload {
        private boolean encrytYn;
        private boolean signYn;
        private boolean compressYn;
    }

    @Getter
    @Setter
    public static class Outbound {
        private String messageBoxId;
        private String messageSlotId;
        private String endPoint;
    }

    @Getter
    @Setter
    public static class Inbound {
        private String messageBoxId;
        private String messageSlotId;
        private String endPoint;
    }
}

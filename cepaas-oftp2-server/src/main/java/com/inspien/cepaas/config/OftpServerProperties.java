package com.inspien.cepaas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@ConfigurationProperties(prefix = "oftpserver")
@Getter
@Setter
public class OftpServerProperties {

    private String baseDirectory;
    private String messageBoxId;
    private String messageBoxEndPoint;
    private List<PhysicalPartner> physicalPartners;

    @Getter
    @Setter
    public static class PhysicalPartner {
        private String ssId;
        private String type;
        private String hostName;
        private int port;
        private boolean useTls;
        private String password;
        private String certPath;
        private String keystorePath;
        private String keystorePassword;
        private List<LogicalPartner> logicalPartners;

        @Getter
        @Setter
        public static class LogicalPartner {
            private String ssId;
            private String sfId;
            private String fileEncryptCertPath;
            private String fileSigningCertPath;
            private String eerpSigningCertPath;
            private String slotId;
            private String charEncoding;
            private boolean signedEerpRequestYn;
            private boolean fileCompressionYn;
            private boolean fileEncryptionYn;
            private boolean fileSignYn;
        }
    }

    public PhysicalPartner findHomePartner() {
        return physicalPartners.stream()
                .filter(partner -> "HOME".equals(partner.getType()))
                .findFirst()
                .orElse(null);
    }
    
    // public PhysicalPartner.LogicalPartner findLogicalPartnerBySfId(String sfId) {
    //     for (PhysicalPartner physicalPartner : physicalPartners) {
    //         Optional<PhysicalPartner.LogicalPartner> logicalPartner = physicalPartner.getLogicalPartners().stream()
    //                 .filter(lp -> sfId.equals(lp.getSfId()))
    //                 .findFirst();
    //         if (logicalPartner.isPresent()) {
    //             return logicalPartner.get();
    //         }
    //     }
    //     return null;
    // }

    public PhysicalPartner.LogicalPartner findLogicalPartnerBySfId(String sfId) {
        return physicalPartners.stream()
                .flatMap(partner -> partner.getLogicalPartners().stream())
                .filter(logicalPartner -> sfId.equals(logicalPartner.getSfId()))
                .findFirst()
                .orElse(null);
    }

}

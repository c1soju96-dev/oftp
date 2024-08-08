package com.inspien.cepaas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "oftpserver")
public class OftpServerProperties {

    private String serverDir;
    private TlsConfig tls;
    private NonTlsConfig nonTls;
    private AuthConfig auth;

    public static class TlsConfig {
        private boolean active;
        private int port;
        private String keystorePath;
        private String keystorePassword;

        // Getters and Setters
        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getKeystorePath() {
            return keystorePath;
        }

        public void setKeystorePath(String keystorePath) {
            this.keystorePath = keystorePath;
        }

        public String getKeystorePassword() {
            return keystorePassword;
        }

        public void setKeystorePassword(String keystorePassword) {
            this.keystorePassword = keystorePassword;
        }
    }

    public static class NonTlsConfig {
        private boolean active;
        private int port;

        // Getters and Setters
        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class AuthConfig {
        private String ssid;
        private String sfid;
        private String password;

        // Getters and Setters
        public String getSsid() {
            return ssid;
        }

        public String getSfid() {
            return sfid;
        }

        public void setSsid(String ssid) {
            this.ssid = ssid;
        }

        public void setSfid(String sfid) {
            this.sfid = sfid;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    // Getters and Setters for serverDir, tls, nonTls, and auth
    public String getServerDir() {
        return serverDir;
    }

    public void setServerDir(String serverDir) {
        this.serverDir = serverDir;
    }

    public TlsConfig getTls() {
        return tls;
    }

    public void setTls(TlsConfig tls) {
        this.tls = tls;
    }

    public NonTlsConfig getNonTls() {
        return nonTls;
    }

    public void setNonTls(NonTlsConfig nonTls) {
        this.nonTls = nonTls;
    }

    public AuthConfig getAuth() {
        return auth;
    }

    public void setAuth(AuthConfig auth) {
        this.auth = auth;
    }
}

package com.inspien.cepaas.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;

import org.neociclo.odetteftp.OdetteFtpSession;
import org.neociclo.odetteftp.OdetteFtpVersion;
import org.neociclo.odetteftp.TransferMode;
import org.neociclo.odetteftp.protocol.v20.CipherSuite;
import org.neociclo.odetteftp.security.MappedCallbackHandler;
import org.neociclo.odetteftp.security.OneToOneHandler;
import org.neociclo.odetteftp.security.PasswordAuthenticationCallback;
import org.neociclo.odetteftp.security.PasswordCallback;
import org.neociclo.odetteftp.service.TcpServer;
import org.neociclo.odetteftp.support.OdetteFtpConfiguration;
import org.neociclo.odetteftp.support.OftpletEventListenerAdapter;
import org.neociclo.odetteftp.support.PasswordHandler;
import org.neociclo.odetteftp.support.PropertiesBasedConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inspien.cepaas.auth.Keystore;
import com.inspien.cepaas.exception.InvalidPasswordException;
import com.inspien.cepaas.handler.OftpPasswordAuthenticationHandler;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.security.auth.callback.Callback;

public class OftpServerManager {

    private static final Logger logger = LoggerFactory.getLogger(OftpServerManager.class);
    private final MappedCallbackHandler serverSecurityHandler = new MappedCallbackHandler();
    private TcpServer server;
    private final OdetteFtpConfiguration config = createInitialServerConfig();
    private File serverFile;
    private OftpletFactoryWrapper factory;

    private final String serverDir;
    private final boolean tlsActive;
    private final int tlsPort;
    private final String keystorePath;
    private final String keystorePassword;
    private final int nonTlsPort;
    private final String ssid;
    private final String sfid;
    private final String password;

    public OftpServerManager(String serverDir, boolean tlsActive, int tlsPort, String keystorePath, String keystorePassword, int nonTlsPort, String ssid, String sfid, String password) {
        this.serverDir = serverDir;
        this.tlsActive = tlsActive;
        this.tlsPort = tlsPort;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.nonTlsPort = nonTlsPort;
        this.ssid = ssid;
        this.sfid = sfid;
        this.password = password;
    }

    public <T extends Callback> void addHandler(Class<T> type, OneToOneHandler<T> handler) {
        serverSecurityHandler.addHandler(type, handler);
    }

    public void startServer() throws Exception {
        if (isRunning()) {
            logger.info("Server is already running. Restarting the server");
            return;
        }

        validatePassword();

        addSecurityHandlers();
        setupServerFile();

        if (tlsActive) {
            setupTlsServer();
        } else {
            setupNonTlsServer();
        }

        server.start();
    }

    public void stopServer() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    public void restartServer() {
        try {
            stopServer();
            startServer();
        } catch (Exception e) {
            logger.error("Error while restarting server", e);
        }
    }

    private static OdetteFtpConfiguration createInitialServerConfig() {
        OdetteFtpConfiguration config = new OdetteFtpConfiguration();
        config.setTransferMode(TransferMode.BOTH);
        config.setVersion(OdetteFtpVersion.OFTP_V20);
        config.setDataExchangeBufferSize(4096);
        config.setWindowSize(64);
        config.setUseSecureAuthentication(false);
        config.setCipherSuiteSelection(CipherSuite.NO_CIPHER_SUITE_SELECTION);
        return config;
    }

    private void validatePassword() {
        if (password == null || password.isEmpty()) {
            throw new InvalidPasswordException();
        }
    }

    private void addSecurityHandlers() {
        addHandler(PasswordAuthenticationCallback.class, new OftpPasswordAuthenticationHandler(password));
        addHandler(PasswordCallback.class, new PasswordHandler(ssid, password));
    }

    private void setupServerFile() {
        serverFile = new File(".", serverDir);
    }

    private void setupTlsServer() throws Exception {
        factory = new OftpletFactoryWrapper(serverFile, config, serverSecurityHandler);
        KeyManager[] keyManagers = new Keystore(keystorePath, keystorePassword.toCharArray()).getKeyManagers();
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, null, null);
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(false);
        sslEngine.setNeedClientAuth(false);
        server = new TcpServer(new InetSocketAddress(tlsPort), sslEngine, factory);
    }

    private void setupNonTlsServer() {
        factory = new OftpletFactoryWrapper(serverFile, config, serverSecurityHandler, new OftpletEventListenerAdapter() {
            @Override
            public void configure(OdetteFtpSession session) {
                String userCode = session.getUserCode();
                PropertiesBasedConfiguration customConfig = new PropertiesBasedConfiguration();
                File configFile = saveConfigToFile(userCode, password, 728, 10, serverDir);
                try {
                    customConfig.load(new FileInputStream(configFile));
                    customConfig.setup(session);
                } catch (IOException e) {
                    logger.error("Failed to configure session", e);
                }
            }
        });
        server = new TcpServer(new InetSocketAddress(nonTlsPort), factory);
    }

    public static File saveConfigToFile(String userCode, String password, int dataExchangeBuffer, int window, String serverDir) {
        File dir = new File(serverDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File configFile = new File(dir, "config");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("userCode=" + userCode + System.lineSeparator());
            writer.write("password=" + password + System.lineSeparator());
            writer.write("dataExchangeBuffer=" + dataExchangeBuffer + System.lineSeparator());
            writer.write("window=" + window + System.lineSeparator());
        } catch (IOException e) {
            logger.error("Failed to save configuration to file", e);
        }
        return configFile;
    }

    public boolean isRunning() {
        return server != null && server.isStarted();
    }

    public static URL getResource(String name) {
        return Thread.currentThread().getContextClassLoader().getResource(name);
    }

    public static File getResourceFile(String name) throws URISyntaxException {
        return new File(getResource(name).toURI());
    }
}

package com.inspien.cepaas.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Objects;

import org.neociclo.odetteftp.OdetteFtpSession;
import org.neociclo.odetteftp.OdetteFtpVersion;
import org.neociclo.odetteftp.TransferMode;
import org.neociclo.odetteftp.protocol.v20.CipherSuite;
import org.neociclo.odetteftp.security.AuthenticationChallengeCallback;
import org.neociclo.odetteftp.security.MappedCallbackHandler;
import org.neociclo.odetteftp.security.OneToOneHandler;
import org.neociclo.odetteftp.security.PasswordAuthenticationCallback;
import org.neociclo.odetteftp.security.PasswordCallback;
import org.neociclo.odetteftp.service.TcpServer;
import org.neociclo.odetteftp.support.OdetteFtpConfiguration;
import org.neociclo.odetteftp.support.OftpletEventListenerAdapter;
import org.neociclo.odetteftp.support.PasswordHandler;
import org.neociclo.odetteftp.support.PropertiesBasedConfiguration;
import org.neociclo.odetteftp.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inspien.cepaas.auth.Keystore;
import com.inspien.cepaas.exception.InvalidPasswordException;
import com.inspien.cepaas.handler.OftpPasswordAuthenticationHandler;
import com.inspien.cepaas.handler.OftpServerSecureAuthHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.security.auth.callback.Callback;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

public class OftpServerManager {

    private static final Logger logger = LoggerFactory.getLogger(OftpServerManager.class);
    private final MappedCallbackHandler serverSecurityHandler = new MappedCallbackHandler();
    private TcpServer server;
    private final OdetteFtpConfiguration config = createInitialServerConfig();
    private File serverFile;
    private OftpletFactoryWrapper factory;

    private final String baseDirectory;
    private final int port;
    private final boolean tlsYn;
    private final String keystorePath;
    private final String keystorePassword;
    private final String ssid;
    private final String password;

    public OftpServerManager(String baseDirectory, boolean tlsYn, int port, String keystorePath, String keystorePassword, String ssid, String password) {
        this.baseDirectory = baseDirectory;
        this.tlsYn = tlsYn;
        this.port = port;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.ssid = ssid;
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

        if (tlsYn) {
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
        addHandler(AuthenticationChallengeCallback.class, new OftpServerSecureAuthHandler(ssid, keystorePath, keystorePassword));
    }

    private void setupServerFile() {
        serverFile = new File(".", baseDirectory);
    }

    private void setupTlsServer() throws Exception {
        factory = new OftpletFactoryWrapper(serverFile, config, serverSecurityHandler, new SessionFinalizationListener(1));
        KeyManager[] keyManagers;
        Keystore keystore = new Keystore(keystorePath, keystorePassword.toCharArray());
        keyManagers = Objects.requireNonNull(keystore).getKeyManagers();
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, null, null);
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(false);
        sslEngine.setNeedClientAuth(true);
        server = new TcpServer(new InetSocketAddress(port), sslContext, factory);


        // String algorithm = "SunX509";
        // KeyStore keyStore = SecurityUtil.openKeyStore(new File(keystorePath), keystorePassword.toCharArray());
        // KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
        // kmf.init(keyStore, keystorePassword.toCharArray());
        // SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		// sslContext.init(kmf.getKeyManagers(), null, null);
        
        // SSLEngine sslEngine = sslContext.createSSLEngine();
        // sslEngine.setUseClientMode(false);
        // sslEngine.setNeedClientAuth(true);

        // factory = new OftpletFactoryWrapper(serverFile, config, serverSecurityHandler);
        // server = new TcpServer(new InetSocketAddress(port), sslEngine, factory);

        // KeyStore keyStore = KeyStore.getInstance("JKS");
        // try (FileInputStream keyStoreStream = new FileInputStream(keystorePath)) {
        //     keyStore.load(keyStoreStream, keystorePassword.toCharArray());
        // }

        // KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        // keyManagerFactory.init(keyStore, keystorePassword.toCharArray());

        // SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        // sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

        // SSLEngine sslEngine = sslContext.createSSLEngine();
        // sslEngine.setUseClientMode(false); // 서버 모드로 설정
        // sslEngine.setNeedClientAuth(true);
        // factory = new OftpletFactoryWrapper(serverFile, config, serverSecurityHandler);
        // server = new TcpServer(new InetSocketAddress(port), sslEngine, factory);

    }

    private void setupNonTlsServer() {
        factory = new OftpletFactoryWrapper(serverFile, config, serverSecurityHandler, new OftpletEventListenerAdapter() {
            @Override
            public void configure(OdetteFtpSession session) {
                String userCode = session.getUserCode();
                PropertiesBasedConfiguration customConfig = new PropertiesBasedConfiguration();
                File configFile = saveConfigToFile(userCode, password, 728, 10, baseDirectory);
                try {
                    customConfig.load(new FileInputStream(configFile));
                    customConfig.setup(session);
                } catch (IOException e) {
                    logger.error("Failed to configure session", e);
                }
            }
        });
        server = new TcpServer(new InetSocketAddress(port), factory);
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

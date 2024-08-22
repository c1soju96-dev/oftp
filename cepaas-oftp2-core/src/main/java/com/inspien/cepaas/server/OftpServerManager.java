package com.inspien.cepaas.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inspien.cepaas.auth.Keystore;
import com.inspien.cepaas.enums.ErrorCode;
import com.inspien.cepaas.exception.InvalidPasswordException;
import com.inspien.cepaas.exception.InvalidTlsServerException;
import com.inspien.cepaas.exception.OftpException;
import com.inspien.cepaas.handler.OftpPasswordAuthenticationHandler;
import com.inspien.cepaas.handler.OftpServerSecureAuthHandler;

import lombok.Builder;
import lombok.Getter;

import javax.net.ssl.SSLContext;
import javax.security.auth.callback.Callback;
import javax.net.ssl.KeyManager;

public class OftpServerManager implements IOftpServerManager {

    private static final Logger logger = LoggerFactory.getLogger(OftpServerManager.class);
    private final MappedCallbackHandler serverSecurityHandler = new MappedCallbackHandler();
    private TcpServer server;
    private final OdetteFtpConfiguration config = createInitialServerConfig();
    private File serverFile;
    private OftpletFactoryWrapper factory;

    @Getter
    private final String baseDirectory;
    @Getter
    private final int port;
    @Getter
    private final boolean tlsYn;
    @Getter
    private final String keystorePath;
    @Getter
    private final String keystorePassword;
    @Getter
    private final String ssid;
    @Getter
    private final String password;

    @Builder
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

    @Override
    public void startServer() throws OftpException {
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

        try {
            server.start();
        } catch (Exception e) {
            throw new OftpException(ErrorCode.INVALID_SERVER_SETTING);
        }
    }

    @Override
    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Override
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

    private void setupTlsServer() {
        factory = new OftpletFactoryWrapper(serverFile, config, serverSecurityHandler, new SessionFinalizationListener(1));
        KeyManager[] keyManagers;
        Keystore keystore;
        try {
            keystore = new Keystore(keystorePath, keystorePassword.toCharArray());
            keyManagers = Objects.requireNonNull(keystore).getKeyManagers();
            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, null, null);
            
            server = new TcpServer(new InetSocketAddress(port), sslContext, factory);
        } catch (UnrecoverableKeyException | KeyStoreException | NoSuchProviderException | 
                 NoSuchAlgorithmException | CertificateException | IOException | KeyManagementException e) {
            throw new InvalidTlsServerException();
        }
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

    private static File saveConfigToFile(String userCode, String password, int dataExchangeBuffer, int window, String serverDir) {
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

    @Override
    public boolean isRunning() {
        return server != null && server.isStarted();
    }

}

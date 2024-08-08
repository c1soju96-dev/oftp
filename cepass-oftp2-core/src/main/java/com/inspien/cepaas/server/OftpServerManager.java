package com.inspien.cepaas.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

import org.neociclo.odetteftp.OdetteFtpSession;
import org.neociclo.odetteftp.OdetteFtpVersion;
import org.neociclo.odetteftp.TransferMode;
import org.neociclo.odetteftp.protocol.v20.CipherSuite;
import org.neociclo.odetteftp.security.AuthenticationChallengeCallback;
import org.neociclo.odetteftp.security.EncryptAuthenticationChallengeCallback;
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
import com.inspien.cepaas.handler.OftpPasswordAuthenticationHandler;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.security.auth.callback.Callback;
import static com.inspien.cepaas.server.OftpServerHelper.getUserConfigFile;

public class OftpServerManager {

    private static final Logger logger = LoggerFactory.getLogger(OftpServerManager.class);
    private final MappedCallbackHandler serverSecurityHandler = new MappedCallbackHandler();
    TcpServer server;
    OdetteFtpConfiguration config = createInitialServerConfig();
    private File serverFile;
    OftpletFactoryWrapper factory;

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

        if (server != null && server.isStarted()) {
            logger.info("Server is already running. Restarting the server");
            return;
        }

        addHandler(PasswordAuthenticationCallback.class, new OftpPasswordAuthenticationHandler(password));
        addHandler(PasswordCallback.class, new PasswordHandler(ssid, password));
        //addHandler(AuthenticationChallengeCallback.class, new OftpServerSecureAuthHandler(propertiesSupport));
        //addHandler(EncryptAuthenticationChallengeCallback.class, new OftpServerEncryptSecureAuthHandler(propertiesSupport));
        
        serverFile = new File(".", serverDir);
        //OftpletFactoryWrapper factory = new OftpletFactoryWrapper(serverFile, config, serverSecurityHandler);
        
        if (tlsActive){
            factory = new OftpletFactoryWrapper(serverFile, config, serverSecurityHandler);
            KeyManager[] km;
            Keystore ks = new Keystore(keystorePath, keystorePassword.toCharArray());
            km = Objects.requireNonNull(ks).getKeyManagers();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(km, null, null);
            SSLEngine sslEngine = sslContext.createSSLEngine();
            sslEngine.setUseClientMode(false); // 서버 모드로 설정
            sslEngine.setNeedClientAuth(false); // 클라이언트 인증 필요 여부 설정
            server = new TcpServer(new InetSocketAddress(tlsPort), sslEngine, factory);
        }
        else {
            factory = new OftpletFactoryWrapper(serverFile, config, serverSecurityHandler,
				new OftpletEventListenerAdapter() {

			@Override
			public void configure(OdetteFtpSession session) {
				// setup custom parameters specific to this user configuration
				String userCode = session.getUserCode();
				//File configFile = getUserConfigFile(serverFile, userCode);
				PropertiesBasedConfiguration customConfig = new PropertiesBasedConfiguration();
                File configFile = saveConfigToFile(userCode, password, 728, 10, serverDir);
				try {
					customConfig.load(new FileInputStream(configFile));
					customConfig.setup(session);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
            server = new TcpServer(new InetSocketAddress(nonTlsPort), factory);
        }
        server.start();
    }

    public void stopServer() throws Exception {
        server.stop();
    }

    public void restartServer(){
        try {
            stopServer();
            startServer();
        } catch (Exception e) {
            logger.error("Error while restarting server", e);
        }
    }

    private static OdetteFtpConfiguration createInitialServerConfig() {
		OdetteFtpConfiguration c = new OdetteFtpConfiguration();

		c.setTransferMode(TransferMode.BOTH);
		c.setVersion(OdetteFtpVersion.OFTP_V20);
		c.setDataExchangeBufferSize(4096);
		c.setWindowSize(64);
		c.setUseSecureAuthentication(false);
		c.setCipherSuiteSelection(CipherSuite.NO_CIPHER_SUITE_SELECTION);

		return c;
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
            e.printStackTrace(); // 예외 처리: 로그 기록 또는 오류 처리 로직 추가 가능
        }
        return configFile;
    }

    public static URL getResource(String name) {
        return Thread.currentThread().getContextClassLoader().getResource(name);
    }
    
    public static File getResourceFile(String name) throws URISyntaxException {
        return new File(getResource(name).toURI());
    }

    public boolean isRunning() {
        return server != null && server.isStarted();
    }
}

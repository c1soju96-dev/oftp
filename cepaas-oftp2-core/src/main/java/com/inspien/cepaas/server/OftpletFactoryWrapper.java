package com.inspien.cepaas.server;

import java.io.File;

import org.neociclo.odetteftp.oftplet.Oftplet;
import org.neociclo.odetteftp.oftplet.OftpletFactory;
import org.neociclo.odetteftp.security.MappedCallbackHandler;
import org.neociclo.odetteftp.support.OdetteFtpConfiguration;
import org.neociclo.odetteftp.support.OftpletEventListener;

public class OftpletFactoryWrapper implements OftpletFactory {

	private File serverBaseDir;
	private OdetteFtpConfiguration config;
	private OftpletEventListener listener;
	private MappedCallbackHandler securityCallbackHandler;
    private String keystorePath;
    private String keystorePassword;

	public OftpletFactoryWrapper(String keystorePath, String keystorePassword, File serverBaseDir, OdetteFtpConfiguration config, MappedCallbackHandler serverSecurityHandler) {
		this(keystorePath, keystorePassword, serverBaseDir, config, serverSecurityHandler, null);
	}

	public OftpletFactoryWrapper(String keystorePath, String keystorePassword, File serverBaseDir, OdetteFtpConfiguration config, MappedCallbackHandler serverSecurityHandler, OftpletEventListener listener) {
		super();
		this.serverBaseDir = serverBaseDir;
		this.config = config;
		this.securityCallbackHandler = serverSecurityHandler;
		this.listener = listener;
		this.keystorePath = keystorePath;
		this.keystorePassword = keystorePassword;

	}

	public Oftplet createProvider() {
		return new ServerOftpletWrapper(keystorePath, keystorePassword, serverBaseDir, config, securityCallbackHandler, listener);
	}

}

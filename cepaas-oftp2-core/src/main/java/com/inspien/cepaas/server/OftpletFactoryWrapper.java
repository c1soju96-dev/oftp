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

	public OftpletFactoryWrapper(File serverBaseDir, OdetteFtpConfiguration config, MappedCallbackHandler serverSecurityHandler) {
		this(serverBaseDir, config, serverSecurityHandler, null);
	}

	public OftpletFactoryWrapper(File serverBaseDir, OdetteFtpConfiguration config, MappedCallbackHandler serverSecurityHandler, OftpletEventListener listener) {
		super();
		this.serverBaseDir = serverBaseDir;
		this.config = config;
		this.securityCallbackHandler = serverSecurityHandler;
		this.listener = listener;
	}

	public Oftplet createProvider() {
		return new ServerOftpletWrapper(serverBaseDir, config, securityCallbackHandler, listener);
	}

}

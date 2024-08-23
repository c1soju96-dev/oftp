package com.inspien.cepaas.handler;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.neociclo.odetteftp.protocol.CommandExchangeBuffer;
import org.neociclo.odetteftp.protocol.EndSessionReason;
import org.neociclo.odetteftp.support.PasswordAuthenticationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OftpPasswordAuthenticationHandler extends PasswordAuthenticationHandler {
	private static final Logger logger = LoggerFactory.getLogger(OftpPasswordAuthenticationHandler.class);

	private boolean useMd5Digest;
	private EndSessionReason cause;
	private String serverPassword;

	public OftpPasswordAuthenticationHandler(String serverPassword) {
		this(serverPassword, false);
	}

	public OftpPasswordAuthenticationHandler(String serverPassword, boolean useMd5Digest) {
		super();
		this.serverPassword = serverPassword;
		this.useMd5Digest = useMd5Digest;
	}

	@Override
	public boolean authenticate(String remoteSSID, String remotePassword) throws IOException {
		logger.debug("Authenticating user: {}", remoteSSID);

		if (serverPassword == null) {
			logger.warn("No user password were set in config file: {}", remoteSSID);
			cause = EndSessionReason.INVALID_PASSWORD;
			return false;
		}

		boolean passwordMatch = false;
		if (useMd5Digest) {
			String passwordHash;
			try {
				passwordHash = hash(remotePassword);
			} catch (NoSuchAlgorithmException e) {
				throw new IOException("Failed to generate MD5 digest over the password.", e);
			}
			passwordMatch = (passwordHash.equals(serverPassword));
		} else {
			passwordMatch = (remotePassword.equalsIgnoreCase(serverPassword));
		}

		if (passwordMatch) {
			return true;
		} else {
			cause = EndSessionReason.INVALID_PASSWORD;
			return false;
		}
	}

	@Override
	public EndSessionReason getCause() {
		return cause;
	}

	private String hash(String text) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(text.getBytes(CommandExchangeBuffer.DEFAULT_PROTOCOL_CHARSET));
		byte[] digest = md.digest();
		return toHexString(digest);
	}

	private String toHexString(byte[] digest) {
		StringBuilder sb = new StringBuilder(); // Use StringBuilder instead of StringBuffer
		for (byte d : digest) {
			String hex = Integer.toHexString(d & 0xff);
			if (hex.length() == 1) {
				sb.append('0').append(hex);
			} else {
				sb.append(hex);
			}
		}
		return sb.toString();
	}
 
}
package com.inspien.cepaas.handler;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.bouncycastle.cms.CMSException;
import org.neociclo.odetteftp.security.AuthenticationChallengeCallback;
import org.neociclo.odetteftp.security.OneToOneHandler;
import org.neociclo.odetteftp.util.EnvelopingUtil;

import com.inspien.cepaas.auth.Keystore;
import com.inspien.cepaas.enums.ErrorCode;
import com.inspien.cepaas.exception.OftpException;

public class OftpServerSecureAuthHandler implements OneToOneHandler<AuthenticationChallengeCallback> {
    
    String remoteSSID = "";
    String keystorePath = "";
    String keystorePassword = "";

    public OftpServerSecureAuthHandler(String remoteSSID, String keystorePath, String keystorePassword) {
        this.remoteSSID = remoteSSID;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
    }

    public void authenticate(AuthenticationChallengeCallback cb)
            throws IOException, CertificateException, NoSuchAlgorithmException,
                   UnrecoverableKeyException, KeyStoreException, NoSuchProviderException
	{
        switch (cb.getSession().getState()) {
            case SPEAKER:
                remoteSSID = cb.getSession().getResponseUser();
                break;
            case LISTENER:
                remoteSSID = cb.getSession().getUserCode();
                break;
            default:
                throw new IllegalStateException("Invalid session state: " + cb.getSession().getState());
        }

        if (keystorePath == null || keystorePassword == null) {
            throw new IllegalArgumentException("Secure authentication keystore path or password is not set.");
        }

        Keystore userKeystore = new Keystore(keystorePath, keystorePassword.toCharArray());
        X509Certificate cert = userKeystore.getCertificate();
        PrivateKey key = userKeystore.getPrivateKey();

        byte[] challengeResponse;
		try {
			challengeResponse = EnvelopingUtil.parseEnvelopedData(cb.getEncodedChallenge(), cert, key);
			cb.setChallenge(challengeResponse);
		} catch (NoSuchProviderException | CMSException | IOException e) {
			throw new OftpException(ErrorCode.INVALID_AUTH, e.getMessage());
		}
    }

    @Override
    public void handle(AuthenticationChallengeCallback cb) throws IOException {
        try {
            authenticate(cb);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new IOException("Authentication failed due to invalid state or arguments.", e);
        } catch (CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException |
                 KeyStoreException | NoSuchProviderException e) {
            throw new IOException("Authentication failed due to security error.", e);
        }
    }
}

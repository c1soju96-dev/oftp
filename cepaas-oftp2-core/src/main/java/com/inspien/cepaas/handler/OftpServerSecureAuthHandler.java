package com.inspien.cepaas.handler;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.neociclo.odetteftp.security.AuthenticationChallengeCallback;
import org.neociclo.odetteftp.security.OneToOneHandler;
import org.neociclo.odetteftp.util.EnvelopingUtil;

import com.inspien.cepaas.auth.Keystore;

public class OftpServerSecureAuthHandler implements OneToOneHandler<AuthenticationChallengeCallback> {
    String remoteSSID = "";
    String keystorePath = "";
    String keystorePassword = "";

	public OftpServerSecureAuthHandler(String remoteSSID, String keystorePath, String keystorePassword) {
        this.remoteSSID = remoteSSID;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
	}

	public void authenticate(AuthenticationChallengeCallback cb) throws Exception {

		switch ( cb.getSession().getState()){
			case SPEAKER:
				remoteSSID =  cb.getSession().getResponseUser();
				break;
			case LISTENER:
				remoteSSID =  cb.getSession().getUserCode();
				break;
			default:
				throw new RuntimeException("Invalid session state: " + cb.getSession().getState());
		}
		
		if(keystorePath == null || keystorePassword == null) {
			throw new Exception("Secure authentication key path or password is not set.");
		}
		
		Keystore userKeystore = new Keystore(keystorePath, keystorePassword.toCharArray());
		X509Certificate cert = userKeystore.getCertificate();
		PrivateKey key = userKeystore.getPrivateKey();

		byte[] challengeResponse = EnvelopingUtil.parseEnvelopedData(cb.getEncodedChallenge(),
				cert, key);

		cb.setChallenge(challengeResponse);
	}

	public void handle(AuthenticationChallengeCallback cb) throws IOException {
        
		try {
			authenticate(cb);
		} catch (Exception e) {
			throw new IOException(e);
		}  
	 }

}
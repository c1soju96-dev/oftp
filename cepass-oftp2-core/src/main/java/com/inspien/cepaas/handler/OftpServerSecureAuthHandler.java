// package com.inspien.cepaas.handler;

// import java.io.IOException;
// import java.security.PrivateKey;
// import java.security.cert.X509Certificate;

// import org.neociclo.odetteftp.security.AuthenticationChallengeCallback;
// import org.neociclo.odetteftp.util.EnvelopingUtil;

// import com.inspien.cepaas.auth.Keystore;


// public class OftpServerSecureAuthHandler {
//     String remoteSSID = "";

// 	public OftpServerSecureAuthHandler(String remoteSSID) {
//         this.remoteSSID = remoteSSID;

// 	}

// 	public void authenticate(AuthenticationChallengeCallback cb) throws Exception {
// 		String remoteSSID = "";
// 		switch ( cb.getSession().getState()){
// 			case SPEAKER:
// 				remoteSSID =  cb.getSession().getResponseUser();
// 				break;
// 			case LISTENER:
// 				remoteSSID =  cb.getSession().getUserCode();
// 				break;
// 			default:
// 				throw new RuntimeException("Invalid session state: " + cb.getSession().getState());
// 		}
// 		RemoteGatewayProperties rp;
// 		rp = propertiesSupport.getRemoteGatewayConfig(remoteSSID);
		
// 		String authKeyPath = rp.getSecureAuth().getLocal().getPath();
// 		String authKeyPass = rp.getSecureAuth().getLocal().getPassword();
		
// 		if(authKeyPath == null || authKeyPass == null) {
// 			throw new Exception("Secure authentication key path or password is not set.");
// 		}
		
// 		Keystore userKeystore = new Keystore(authKeyPath, authKeyPass.toCharArray());
// 		X509Certificate cert = userKeystore.getCertificate();
// 		PrivateKey key = userKeystore.getPrivateKey();

// 		byte[] challengeResponse = EnvelopingUtil.parseEnvelopedData(cb.getEncodedChallenge(),
// 				cert, key);

// 		cb.setChallenge(challengeResponse);
// 	}

// 	public void handle(AuthenticationChallengeCallback cb) throws IOException {
// 		try {
// 			authenticate(cb);
// 		} catch (Exception e) {
// 			throw new IOException(e);
// 		}  
// 	 }

// }
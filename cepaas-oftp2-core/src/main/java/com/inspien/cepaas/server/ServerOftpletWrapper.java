package com.inspien.cepaas.server;

import static com.inspien.cepaas.util.OftpServerUtil.*;
import static org.neociclo.odetteftp.protocol.DefaultEndFileResponse.*;
import static org.neociclo.odetteftp.protocol.DefaultStartFileResponse.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bouncycastle.cms.CMSException;
import org.neociclo.odetteftp.OdetteFtpException;
import org.neociclo.odetteftp.OdetteFtpSession;
import org.neociclo.odetteftp.OdetteFtpVersion;
import org.neociclo.odetteftp.oftplet.AnswerReasonInfo;
import org.neociclo.odetteftp.oftplet.EndFileResponse;
import org.neociclo.odetteftp.oftplet.OftpletAdapter;
import org.neociclo.odetteftp.oftplet.OftpletListener;
import org.neociclo.odetteftp.oftplet.OftpletSpeaker;
import org.neociclo.odetteftp.oftplet.StartFileResponse;
import org.neociclo.odetteftp.protocol.AnswerReason;
import org.neociclo.odetteftp.protocol.DefaultDeliveryNotification;
import org.neociclo.odetteftp.protocol.DeliveryNotification;
import org.neociclo.odetteftp.protocol.DeliveryNotification.EndResponseType;
import org.neociclo.odetteftp.protocol.OdetteFtpObject;
import org.neociclo.odetteftp.protocol.VirtualFile;
import org.neociclo.odetteftp.protocol.v20.DefaultSignedDeliveryNotification;
import org.neociclo.odetteftp.protocol.v20.EnvelopedVirtualFile;
import org.neociclo.odetteftp.protocol.v20.SecurityLevel;
import org.neociclo.odetteftp.security.DefaultSecurityContext;
import org.neociclo.odetteftp.security.MappedCallbackHandler;
import org.neociclo.odetteftp.security.SecurityContext;
import org.neociclo.odetteftp.support.OdetteFtpConfiguration;
import org.neociclo.odetteftp.support.OftpletEventListener;
import org.neociclo.odetteftp.util.AttributeKey;
import org.neociclo.odetteftp.util.EnvelopingUtil;
import org.neociclo.odetteftp.util.SessionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inspien.cepaas.auth.Keystore;
import com.inspien.cepaas.enums.ErrorCode;
import com.inspien.cepaas.exception.OftpException;
import com.inspien.cepaas.util.OftpServerUtil;

class ServerOftpletWrapper extends OftpletAdapter implements org.neociclo.odetteftp.oftplet.ServerOftplet, OftpletSpeaker, OftpletListener {

	private static final Logger logger = LoggerFactory.getLogger(ServerOftpletWrapper.class);
	private static final AttributeKey CURRENT_REQUEST_ATTR = new AttributeKey(SessionHelper.class, "__obj_currentRequest");
	private static final ServerRoutingWorker ROUTING_WORKER = new ServerRoutingWorker();

	private File serverBaseDir;
	private OftpletEventListener listener;
	private SecurityContext securityContext;
	private OdetteFtpConfiguration config;
	private OdetteFtpSession session;
	private String keystorePath;
    private String keystorePassword;

	private Map<String, Iterator<File>> outFileIteratorMap = new HashMap<>();

	public ServerOftpletWrapper(String keystorePath, String keystorePassword, File serverBaseDir, OdetteFtpConfiguration config, MappedCallbackHandler securityCallbackHandler, OftpletEventListener listener) {
		super();
		this.serverBaseDir = serverBaseDir;
		this.config = config;
		this.securityContext = new DefaultSecurityContext(securityCallbackHandler);
		this.listener = listener;
		this.keystorePath = keystorePath;
		this.keystorePassword = keystorePassword;
	}

	// -------------------------------------------------------------------------
	//   Oftplet implementation
	// -------------------------------------------------------------------------

	@Override
	public boolean isProtocolVersionSupported(OdetteFtpVersion version) {
		return (config != null ? config.getVersion().isEqualOrOlder(version) : super.isProtocolVersionSupported(version));
	}

	@Override
	public SecurityContext getSecurityContext() {
		return securityContext;
	}

	@Override
	public void init(OdetteFtpSession session) throws OdetteFtpException {
		this.session = session;
		config.setup(session);

		if (listener != null) {
			listener.init(session);
		}
	}

	public void configure() {
		if (listener != null) {
			listener.configure(session);
		}
	}

	@Override
	public void destroy() {
		this.config = null;
		this.session = null;
		this.securityContext = null;
		if (listener != null) {
			listener.destroy();
		}
	}

	@Override
	public void onSessionStart() {

		String userCode = session.getUserCode();
		createUserDirStructureIfNotExist(userCode);

		if (listener != null) {
			listener.onSessionStart();
		}
	}

	@Override
	public void onExceptionCaught(Throwable cause) {
		logger.error("Exception Caught.", cause);
		if (listener != null) {
			listener.onExceptionCaught(cause);
		}
	}

	@Override
	public void onSessionEnd() {
		if (listener != null) {
			listener.onSessionEnd();
		}
	}

	@Override
	public OftpletSpeaker getSpeaker() {
		return this;
	}

	@Override
	public OftpletListener getListener() {
		return this;
	}

	// -------------------------------------------------------------------------
	//   OftpletSpeaker implementation
	// -------------------------------------------------------------------------

	public OdetteFtpObject nextOftpObjectToSend() {
		String userCode = session.getUserCode();
		Iterator<File> filesIt = getUserOutFileIterator(userCode);
	
		if (!filesIt.hasNext()) {
			outFileIteratorMap.remove(userCode);
			return null;
		}
	
		File cur = filesIt.next();
		try {
			return loadObject(cur);
		} catch (IOException e) {
			logger.error("Failed to load Odette FTP object file: " + cur, e);
			if (cur.exists()) {
				try {
					Files.delete(cur.toPath());
					logger.info("Successfully deleted the file:{}", cur.getName());
				} catch (IOException deleteException) {
					logger.error("Failed to delete the file: " + cur, deleteException);
				}
			}
			return null;
		}
	}

	private Iterator<File> getUserOutFileIterator(String userCode) {
		Iterator<File> it = outFileIteratorMap.get(userCode);
		if (it == null) {
			File[] a = listExchanges(userCode);
			List<File> files = Arrays.asList(a);
			it = files.iterator();
			outFileIteratorMap.put(userCode, it);
		}
		return it;
	}

	public void onSendFileStart(VirtualFile virtualFile, long answerCount) {
		logger.info("onSendFileStart:{}", virtualFile.getDestination());
	}

	public void onDataSent(VirtualFile virtualFile, long totalOctetsSent) {
		logger.info("onDataSent:{}", virtualFile.getDestination());
	}

	public void onSendFileEnd(VirtualFile virtualFile) {
		deleteExchange(virtualFile);
	}

	public void onSendFileError(VirtualFile virtualFile, AnswerReasonInfo reason, boolean retryLater) {
		logger.error("onSendFileError:{}",reason.getReasonText());
	}

	public void onNotificationSent(DeliveryNotification notification) {
		deleteExchange(notification);
	}

	// -------------------------------------------------------------------------
	//   OftpletListener implementation
	// -------------------------------------------------------------------------

	public StartFileResponse acceptStartFile(VirtualFile vf) {

		String recipientOid = vf.getDestination();

		if (!recipientExists(recipientOid)) {
			return negativeStartFileAnswer(AnswerReason.INVALID_DESTINATION,
					"Recipient [" + recipientOid + "] doesn't exist.", false);
		}
		createUserDirStructureIfNotExist(recipientOid);

		if (targetFileExists(recipientOid, vf)) {
			return negativeStartFileAnswer(AnswerReason.DUPLICATE_FILE,
					"File already exist in the recipient [" + recipientOid + "].", true);
		}

		File dataFile = null;
		try {
			dataFile = createDataFile(vf);
			logger.trace("Saving to: {}", dataFile);
		} catch (IOException e) {
			logger.error("Cannot create data file for object: " + vf, e);
			return negativeStartFileAnswer(AnswerReason.ACCESS_METHOD_FAILURE, "Couldn't store file in local system.",
					true);
		}
		return positiveStartFileAnswer(dataFile);
	}

	public void onReceiveFileStart(VirtualFile virtualFile, long answerCount) {

		if (virtualFile instanceof EnvelopedVirtualFile) {
			EnvelopedVirtualFile envelopedFile = (EnvelopedVirtualFile) virtualFile;
			// 복호화 및 서명 검증 로직 추가
			// 예를 들어, 서명 검증 및 복호화 상태 확인
			if (envelopedFile.getSecurityLevel() == SecurityLevel.SIGNED) {
				// 서명 검증 로직
			}
			if (envelopedFile.getSecurityLevel() == SecurityLevel.ENCRYPTED) {
				// 복호화 로직
			}
		}
		try {
			store(virtualFile);
		} catch (IOException e) {
			logger.error("Cannot store data file for object:{}", e.getMessage());
		}

	}

	public void onDataReceived(VirtualFile virtualFile, long totalOctetsReceived) {
		logger.info("onDataReceived:{}", virtualFile.getDestination());
	}

	public EndFileResponse onReceiveFileEnd(VirtualFile virtualFile, long recordCount, long unitCount) {
		String userCode = session.getUserCode();
		ROUTING_WORKER.deliver(serverBaseDir, userCode, virtualFile);

		try {
            storeNotification(virtualFile, keystorePath, keystorePassword);
        } catch (IOException e) {
            e.printStackTrace();
        }

		return positiveEndFileAnswer(hasExchange(userCode));
	}

	public static OdetteFtpObject getSessionCurrentRequest(OdetteFtpSession session) {
        return session.getTypedAttribute(OdetteFtpObject.class, CURRENT_REQUEST_ATTR);
    }

	public void onReceiveFileError(VirtualFile virtualFile, AnswerReasonInfo reason) {
		logger.error("onReceiveFileError:{}",reason.getReasonText());
	}

	@Override
	public void onNotificationReceived(DeliveryNotification notif){

		String userCode = session.getUserCode();

		try {
			store(notif);
		} catch (IOException e) {
			logger.error("Cannot store data file for object:{}", e.getMessage());
		}

		ROUTING_WORKER.deliver(serverBaseDir, userCode, notif);

	}

	// -------------------------------------------------------------------------
	//   Implementation specific methods
	// -------------------------------------------------------------------------

	private void store(OdetteFtpObject obj) throws IOException {
		String userCode = session.getUserCode();
		OftpServerUtil.storeInWork(userCode, obj, serverBaseDir, UUID::randomUUID);
	}

	private void storeNotification(VirtualFile virtualFile, String keystorePath, String keystorePassword) throws IOException {

		String userCode = session.getUserCode();
		EnvelopedVirtualFile vf = (EnvelopedVirtualFile) virtualFile;
		if(vf.isSignedNotificationRequest()){
			DeliveryNotification signedNotification = org.neociclo.odetteftp.util.OdetteFtpSupport.getReplyDeliveryNotification(vf);
			try {
				Keystore keystore = new Keystore(keystorePath, keystorePassword.toCharArray());
				EnvelopingUtil.addNotifSignature((DefaultSignedDeliveryNotification)signedNotification, vf.getCipherSuite(), keystore.getCertificate(), keystore.getPrivateKey());
			} catch (UnrecoverableKeyException | KeyStoreException | NoSuchProviderException | NoSuchAlgorithmException
					| CertificateException | IOException | CMSException e) {
				throw new OftpException(ErrorCode.INVALID_KEY);
			}
			OftpServerUtil.storeInMailbox(userCode, signedNotification, serverBaseDir, UUID::randomUUID);
		} else {
			DefaultDeliveryNotification notification = createNotification(virtualFile);
			OftpServerUtil.storeInMailbox(userCode, notification, serverBaseDir, UUID::randomUUID);
		}
	}

	private void createUserDirStructureIfNotExist(String userCode) {
		OftpServerUtil.createUserDirStructureIfNotExist(userCode, serverBaseDir);
	}

	private boolean recipientExists(String recipientOid) {
		File recipientDir = getUserDir(serverBaseDir, recipientOid);
		if (!recipientDir.exists()) {
			recipientDir.mkdirs();
		}
		return recipientDir.exists();
	}

	private boolean targetFileExists(String recipientOid, VirtualFile vf) {

		String filename = createFileName(vf, UUID::randomUUID);
		File mailboxDir = getUserMailboxDir(serverBaseDir, recipientOid);

		File target = new File(mailboxDir, filename);
		return target.exists();
	}

	private File createDataFile(VirtualFile vf) throws IOException {
		return OftpServerUtil.createDataFile(vf, serverBaseDir, UUID::randomUUID);
	}

	private boolean hasExchange(String userCode) {
		return OftpServerUtil.hasExchange(userCode, serverBaseDir);
	}

	private File[] listExchanges(String userCode) {
		return OftpServerUtil.listExchanges(userCode, serverBaseDir);
	}

	void deleteExchange(OdetteFtpObject obj) {
		String userCode = session.getUserCode();
		OftpServerUtil.deleteExchange(userCode, obj, serverBaseDir);
	}

	private DefaultDeliveryNotification createNotification(VirtualFile virtualFile) {
        DefaultDeliveryNotification notification = new DefaultDeliveryNotification(EndResponseType.END_TO_END_RESPONSE);
        notification.setDatasetName(virtualFile.getDatasetName());
        notification.setDateTime(new Date(virtualFile.getDateTime().getTime()));
        notification.setTicker(virtualFile.getTicker());
        notification.setDestination(virtualFile.getOriginator());
        notification.setOriginator(virtualFile.getDestination());
        notification.setUserData(virtualFile.getUserData());
        notification.setCreator(virtualFile.getOriginator());
        return notification;
    }
}
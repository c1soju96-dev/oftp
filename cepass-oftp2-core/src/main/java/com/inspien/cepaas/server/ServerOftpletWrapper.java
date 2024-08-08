package com.inspien.cepaas.server;

import static com.inspien.cepaas.server.OftpServerHelper.*;
import static org.neociclo.odetteftp.protocol.DefaultEndFileResponse.*;
import static org.neociclo.odetteftp.protocol.DefaultStartFileResponse.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.neociclo.odetteftp.protocol.CommandExchangeBuffer;
import org.neociclo.odetteftp.protocol.DefaultDeliveryNotification;
import org.neociclo.odetteftp.protocol.DeliveryNotification;
import org.neociclo.odetteftp.protocol.DeliveryNotification.EndResponseType;
import org.neociclo.odetteftp.protocol.NegativeResponseReason;
import org.neociclo.odetteftp.protocol.OdetteFtpObject;
import org.neociclo.odetteftp.protocol.VirtualFile;
import org.neociclo.odetteftp.protocol.v20.DefaultSignedDeliveryNotification;
import org.neociclo.odetteftp.protocol.v20.EnvelopedVirtualFile;
import org.neociclo.odetteftp.protocol.v20.FileEnveloping;
import org.neociclo.odetteftp.security.DefaultSecurityContext;
import org.neociclo.odetteftp.security.MappedCallbackHandler;
import org.neociclo.odetteftp.security.SecurityContext;
import org.neociclo.odetteftp.support.OdetteFtpConfiguration;
import org.neociclo.odetteftp.support.OftpletEventListener;
import org.neociclo.odetteftp.util.AttributeKey;
import org.neociclo.odetteftp.util.SessionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ServerOftpletWrapper extends OftpletAdapter implements org.neociclo.odetteftp.oftplet.ServerOftplet, OftpletSpeaker, OftpletListener {

	private static final Logger logger = LoggerFactory.getLogger(ServerOftpletWrapper.class);
	private static final AttributeKey CURRENT_REQUEST_ATTR = new AttributeKey(SessionHelper.class, "__obj_currentRequest");

	private static final ServerRoutingWorker ROUTING_WORKER = new ServerRoutingWorker();
	//private final OftpDataProvider oftpDataProvider;

	private File serverBaseDir;
	private OftpletEventListener listener;
	private SecurityContext securityContext;
	private OdetteFtpConfiguration config;
	private OdetteFtpSession session;

	private Map<String, Iterator<File>> outFileIteratorMap = new HashMap<String, Iterator<File>>();

	public ServerOftpletWrapper(File serverBaseDir, OdetteFtpConfiguration config, MappedCallbackHandler securityCallbackHandler, OftpletEventListener listener) {
		super();
		this.serverBaseDir = serverBaseDir;
		this.config = config;
		this.securityContext = new DefaultSecurityContext(securityCallbackHandler);
		//this.oftpDataProvider = new OftpDataProvider(serverBaseDir);
		this.listener = listener;
	}

	// -------------------------------------------------------------------------
	//   Oftplet implementation
	// -------------------------------------------------------------------------

	@Override
	public boolean isProtocolVersionSupported(OdetteFtpVersion version) {
		// server that accepts downgrading the version
		return (config != null ? config.getVersion().isEqualOrOlder(version) : super.isProtocolVersionSupported(version));
	};

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

		OdetteFtpObject next = null;

		String userCode = session.getUserCode();

		Iterator<File> filesIt = getUserOutFileIterator(userCode);
		if (filesIt.hasNext()) {
			File cur = filesIt.next();
			try {
				next = loadObject(cur);
			} catch (IOException e) {
				logger.error("Failed to load Odette FTP obejct file: " + cur, e);
				if (cur.exists()) {
					cur.delete();
				}
			}
		} else {
			outFileIteratorMap.remove(userCode);
		}

		return next;
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
	}

	public void onDataSent(VirtualFile virtualFile, long totalOctetsSent) {
	}

	public void onSendFileEnd(VirtualFile virtualFile) {
		deleteExchange(virtualFile);
	}

	public void onSendFileError(VirtualFile virtualFile, AnswerReasonInfo reason, boolean retryLater) {
	}

	public void onNotificationSent(DeliveryNotification notif) {
		deleteExchange(notif);
	}

	// -------------------------------------------------------------------------
	//   OftpletListener implementation
	// -------------------------------------------------------------------------

	// public StartFileResponse acceptStartFile(VirtualFile virtualFile) {
	// 	EnvelopedVirtualFile vf = (EnvelopedVirtualFile) virtualFile;
	// 	String homeSFID = virtualFile.getDestination();
	// 	String remoteSSID = session.getUserCode();

	// 	if (!(oftpDataProvider.existHomeSFID(remoteSSID, homeSFID))) {
	// 		logger.error("Recipient doesn't exist: remote SSID - {}, home SFID - {}", remoteSSID, homeSFID);
	// 		return negativeStartFileAnswer(AnswerReason.INVALID_DESTINATION,
	// 				"Recipient [" + homeSFID + "] doesn't exist.", false);
	// 	}

	// 	if(virtualFile.getRestartOffset()>0){
    //         try {
    //             OdetteFtpObject re_vf = oftpDataProvider.getRestartFile(remoteSSID, homeSFID, virtualFile);
	// 			File f = ((VirtualFile)re_vf).getFile();
	// 			return positiveStartFileAnswer(f);
    //         } catch (IOException e) {
	// 			logger.error("Failed to get restart file: {}{}{} ", remoteSSID, homeSFID , createVirtualFileName(vf), e);
	// 			return negativeStartFileAnswer(AnswerReason.ACCESS_METHOD_FAILURE, "Failed to get restart file.", false);
    //         }
    //     }

	// 	File tmpData;
	// 	if (vf.getEnvelopingFormat() == FileEnveloping.NO_ENVELOPE){
	// 		tmpData = oftpDataProvider.storeData(remoteSSID, virtualFile);
	// 	}else{
	// 		tmpData = oftpDataProvider.storeEnvelopedData(remoteSSID, virtualFile);
	// 	}

	// 	return positiveStartFileAnswer(tmpData);
	// }

	public StartFileResponse acceptStartFile(VirtualFile vf) {

		String userCode = session.getUserCode();
		String recipientOid = vf.getDestination();

		if (!recipientExists(userCode, recipientOid)) {
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

		try {
			store(virtualFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// public void onReceiveFileStart(VirtualFile virtualFile, long answerCount) {
	// 	String userCode = session.getUserCode();
	// 	logger.info("Receiving file: {}", virtualFile);
	// 	try {
	// 		oftpDataProvider.storeVirtualFile(userCode, virtualFile);
	// 	} catch (IOException e) {
	// 		logger.error("Failed to store virtual file: " + createVirtualFileName(virtualFile), e);
	// 	}

	// }


	public void onDataReceived(VirtualFile virtualFile, long totalOctetsReceived) {
	}

	public EndFileResponse onReceiveFileEnd(VirtualFile virtualFile, long recordCount, long unitCount) {

		String userCode = session.getUserCode();
		ROUTING_WORKER.deliver(serverBaseDir, userCode, virtualFile);

		//toDO
		DefaultDeliveryNotification notif = new DefaultDeliveryNotification(EndResponseType.END_TO_END_RESPONSE);
		notif.setDatasetName(virtualFile.getDatasetName());
        notif.setDateTime(new Date(virtualFile.getDateTime().getTime()));
		notif.setTicker(virtualFile.getTicker());
        notif.setDestination(virtualFile.getDestination());
        notif.setOriginator(virtualFile.getOriginator());
        notif.setUserData(virtualFile.getUserData());
		notif.setCreator(virtualFile.getOriginator());
      	notif.setReason((NegativeResponseReason)null);
     	notif.setReasonText(null);

		try {
			storeNoti(notif);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//ROUTING_WORKER.deliver(serverBaseDir, userCode, notif);

		return positiveEndFileAnswer(hasExchange(userCode));
	}

	public static OdetteFtpObject getSessionCurrentRequest(OdetteFtpSession session) {
        OdetteFtpObject exchange = session.getTypedAttribute(OdetteFtpObject.class, CURRENT_REQUEST_ATTR);
        return exchange;
    }

	// private void sendEERP(String userCode, VirtualFile virtualFile) throws OdetteFtpException {
	// 	// EERP 객체 생성
	// 	DeliveryNotification eerp = new DeliveryNotification();
	// 	eerp.setFileReference
	// 	eerp.setFileReference(virtualFile.getFileReference());
	// 	eerp.setOriginator(userCode);
	// 	eerp.setReasonCode(DeliveryNotification.SUCCESSFUL_DELIVERY);
	
	// 	// EERP 전송
	// 	session.write(eerp);
	// }

	// public EndFileResponse onReceiveFileEnd(VirtualFile virtualFile, long recordCount, long unitCount) {
	// 	String userCode = session.getUserCode(); // 사용자의 식별자 가져오기
	// 	File userDirectory = new File(serverBaseDir, userCode); // 사용자별 디렉토리 경로 설정

	// 	// 사용자 디렉토리가 없으면 생성
	// 	if (!userDirectory.exists()) {
	// 		userDirectory.mkdirs();
	// 	}

	// 	// virtualFile에서 실제 파일 객체 가져오기
	// 	File sourceFile = virtualFile.getFile();
	// 	String fileName = sourceFile.getName(); // 파일 이름 가져오기

	// 	// 저장할 파일 경로 설정
	// 	File destinationFile = new File(userDirectory, fileName);

	// 	// 파일 복사
	// 	try (FileInputStream fis = new FileInputStream(sourceFile);
	// 		FileOutputStream fos = new FileOutputStream(destinationFile)) {

	// 		byte[] buffer = new byte[1024];
	// 		int length;
	// 		while ((length = fis.read(buffer)) > 0) {
	// 			fos.write(buffer, 0, length);
	// 		}

	// 	} catch (IOException e) {
	// 		e.printStackTrace();
	// 		// 필요 시 예외 처리 추가
	// 	}

	// 	//ROUTING_WORKER.deliver(serverBaseDir, userCode, virtualFile); // 파일 전달 작업 수행

	// 	return positiveEndFileAnswer(true); // 응답 반환
	// }


	public void onReceiveFileError(VirtualFile virtualFile, AnswerReasonInfo reason) {
	}

	@Override
	public void onNotificationReceived(DeliveryNotification notif) {

		String userCode = session.getUserCode();

		try {
			store(notif);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ROUTING_WORKER.deliver(serverBaseDir, userCode, notif);

	}

	// -------------------------------------------------------------------------
	//   Implementation specific methods
	// -------------------------------------------------------------------------

	private void store(OdetteFtpObject obj) throws IOException {
		String userCode = session.getUserCode();
		OftpServerHelper.storeInWork(userCode, obj, serverBaseDir);
	}

	private void storeNoti(OdetteFtpObject obj) throws IOException {
		String userCode = session.getUserCode();
		OftpServerHelper.storeInMailbox(userCode, obj, serverBaseDir);
	}

	private void createUserDirStructureIfNotExist(String userCode) {
		OftpServerHelper.createUserDirStructureIfNotExist(userCode, serverBaseDir);
	}

	private boolean recipientExists(String userCode, String recipientOid) {
		File recipientDir = getUserDir(serverBaseDir, recipientOid);
		if (!recipientDir.exists()) {
			recipientDir.mkdirs();
		}
		return recipientDir.exists();
	}

	/**
	 * Check if the Virtual File already exist in the recipient mailbox.
	 * 
	 * @param recipientOid
	 * @param vf
	 * @return
	 */
	private boolean targetFileExists(String recipientOid, VirtualFile vf) {

		String filename = createFileName(vf);
		File mailboxDir = getUserMailboxDir(serverBaseDir, recipientOid);

		File target = new File(mailboxDir, filename);
		return target.exists();
	}

	private File createDataFile(VirtualFile vf) throws IOException {
		return OftpServerHelper.createDataFile(vf, serverBaseDir);
	}

	/**
	 * Check it has exchange in the user mailbox.
	 *
	 * @param userCode
	 * @return
	 */
	private boolean hasExchange(String userCode) {
		return OftpServerHelper.hasExchange(userCode, serverBaseDir);
	}

	private File[] listExchanges(String userCode) {
		return OftpServerHelper.listExchanges(userCode, serverBaseDir);
	}

	private void deleteExchange(OdetteFtpObject obj) {
		String userCode = session.getUserCode();
		OftpServerHelper.deleteExchange(userCode, obj, serverBaseDir);
	}

}
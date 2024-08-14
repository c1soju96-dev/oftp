package com.inspien.cepaas.server;

import static com.inspien.cepaas.util.OftpServerUtil.*;
import static org.neociclo.odetteftp.protocol.DefaultEndFileResponse.*;
import static org.neociclo.odetteftp.protocol.DefaultStartFileResponse.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.neociclo.odetteftp.security.DefaultSecurityContext;
import org.neociclo.odetteftp.security.MappedCallbackHandler;
import org.neociclo.odetteftp.security.SecurityContext;
import org.neociclo.odetteftp.support.OdetteFtpConfiguration;
import org.neociclo.odetteftp.support.OftpletEventListener;
import org.neociclo.odetteftp.util.AttributeKey;
import org.neociclo.odetteftp.util.SessionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private Map<String, Iterator<File>> outFileIteratorMap = new HashMap<String, Iterator<File>>();

	public ServerOftpletWrapper(File serverBaseDir, OdetteFtpConfiguration config, MappedCallbackHandler securityCallbackHandler, OftpletEventListener listener) {
		super();
		this.serverBaseDir = serverBaseDir;
		this.config = config;
		this.securityContext = new DefaultSecurityContext(securityCallbackHandler);
		this.listener = listener;
	}

	// -------------------------------------------------------------------------
	//   Oftplet implementation
	// -------------------------------------------------------------------------

	@Override
	public boolean isProtocolVersionSupported(OdetteFtpVersion version) {
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

	public void onNotificationSent(DeliveryNotification notification) {
		deleteExchange(notification);
	}

	// -------------------------------------------------------------------------
	//   OftpletListener implementation
	// -------------------------------------------------------------------------

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

	public void onDataReceived(VirtualFile virtualFile, long totalOctetsReceived) {
	}

	public EndFileResponse onReceiveFileEnd(VirtualFile virtualFile, long recordCount, long unitCount) {
		String userCode = session.getUserCode();
		ROUTING_WORKER.deliver(serverBaseDir, userCode, virtualFile);

		try {
            storeNotification(virtualFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

		return positiveEndFileAnswer(hasExchange(userCode));
	}

	public static OdetteFtpObject getSessionCurrentRequest(OdetteFtpSession session) {
        OdetteFtpObject exchange = session.getTypedAttribute(OdetteFtpObject.class, CURRENT_REQUEST_ATTR);
        return exchange;
    }

	public void onReceiveFileError(VirtualFile virtualFile, AnswerReasonInfo reason) {
	}

	@Override
	public void onNotificationReceived(DeliveryNotification notif){

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
		OftpServerUtil.storeInWork(userCode, obj, serverBaseDir, UUID::randomUUID);
	}

	private void storeNotification(VirtualFile virtualFile) throws IOException {
		DefaultDeliveryNotification notification = createNotification(virtualFile);
		String userCode = session.getUserCode();
		OftpServerUtil.storeInMailbox(userCode, notification, serverBaseDir, UUID::randomUUID);
	}

	private void createUserDirStructureIfNotExist(String userCode) {
		OftpServerUtil.createUserDirStructureIfNotExist(userCode, serverBaseDir);
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

		String filename = createFileName(vf, UUID::randomUUID);
		File mailboxDir = getUserMailboxDir(serverBaseDir, recipientOid);

		File target = new File(mailboxDir, filename);
		return target.exists();
	}

	private File createDataFile(VirtualFile vf) throws IOException {
		return OftpServerUtil.createDataFile(vf, serverBaseDir, UUID::randomUUID);
	}

	/**
	 * Check it has exchange in the user mailbox.
	 *
	 * @param userCode
	 * @return
	 */
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
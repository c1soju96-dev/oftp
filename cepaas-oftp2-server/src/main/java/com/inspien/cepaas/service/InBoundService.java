package com.inspien.cepaas.service;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.neociclo.odetteftp.OdetteFtpException;
import org.neociclo.odetteftp.protocol.DefaultVirtualFile;
import org.neociclo.odetteftp.protocol.OdetteFtpObject;
import org.neociclo.odetteftp.protocol.v20.CipherSuite;
import org.neociclo.odetteftp.protocol.v20.DefaultEnvelopedVirtualFile;
import org.neociclo.odetteftp.protocol.v20.EnvelopedVirtualFile;
import org.neociclo.odetteftp.protocol.v20.FileCompression;
import org.neociclo.odetteftp.protocol.v20.FileEnveloping;
import org.neociclo.odetteftp.protocol.v20.SecurityLevel;
import org.neociclo.odetteftp.security.MappedCallbackHandler;
import org.neociclo.odetteftp.service.TcpClient;
import org.neociclo.odetteftp.support.InOutSharedQueueOftpletFactory;
import org.neociclo.odetteftp.support.OdetteFtpConfiguration;
import org.neociclo.odetteftp.support.PasswordHandler;
import org.neociclo.odetteftp.util.ProtocolUtil;
import org.neociclo.odetteftp.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.inspien.cepaas.auth.Keystore;
import com.inspien.cepaas.client.MessageBoxClient;
import com.inspien.cepaas.config.OftpServerProperties;
import com.inspien.cepaas.config.OftpServerProperties.PhysicalPartner;
import com.inspien.cepaas.enums.ErrorCode;
import com.inspien.cepaas.exception.OftpException;
import com.inspien.cepaas.msgbox.api.MessageRecord;
import com.inspien.cepaas.msgbox.api.MessageRecordWithAttachment;
import com.inspien.cepaas.msgbox.api.MessageStatus;
import com.inspien.cepaas.msgbox.api.PendingMessageRecordQuery;
import com.inspien.cepaas.util.FileUtil;
import com.inspien.cepaas.util.OftpServerUtil;

@Service
public class InBoundService {

    private static final Logger logger = LoggerFactory.getLogger(InBoundService.class);
    private final OftpServerProperties oftpServerProperties;
    private final ConcurrentLinkedQueue<MessageRecordWithAttachment> messageQueue = new ConcurrentLinkedQueue<>();
    private static final String REMOTE_SFID = "MENDELSON_SFID";
    //PhysicalPartner physicalPartner = properties.findHomePartner();

    public InBoundService(OftpServerProperties oftpServerProperties) {
        this.oftpServerProperties = oftpServerProperties;
    }

    @Scheduled(fixedRate = 60000)
    public void scheduleMessageProcessing() {
        if (messageQueue.isEmpty()) {
            enqueueMessages();
        }

        if (!messageQueue.isEmpty()) {
            processInboundMessages();
        }
    }

    private void enqueueMessages() {
        try {
            // TODO : Remote SSID에 SFID들의 Inbound mbox들에 대해 로직을 수행해야 함.
            PhysicalPartner.LogicalPartner logicalPartner = oftpServerProperties.findLogicalPartnerBySfId(REMOTE_SFID);
            List<MessageRecordWithAttachment> messages = getWaitMessageList(logicalPartner.getSlotId(), oftpServerProperties.getMessageBoxEndPoint());
            messageQueue.addAll(messages);
        } catch (Exception e) {
            logger.error("Failed to enqueue messages", e);
        }
    }

    public void processInboundMessages() {
        while (!messageQueue.isEmpty()) {
            MessageRecordWithAttachment message = messageQueue.poll();
            if (message != null) {
                processAndSendMessage(message);
            }
        }
    }

    private void processAndSendMessage(MessageRecord message) {
        try {
            // 1. File 생성
            PhysicalPartner physicalHomePartner = oftpServerProperties.findHomePartner();
            PhysicalPartner.LogicalPartner logicalPartner = oftpServerProperties.findLogicalPartnerBySfId(REMOTE_SFID);

            String data = getPayloadFromMessageBox(message, oftpServerProperties.getMessageBoxEndPoint());

            if(data.isEmpty()){
                throw new OftpException(ErrorCode.EMPTY_PAYLOAD_ERROR);
            }
            byte[] payload = data.getBytes(StandardCharsets.UTF_8.name());
            String datasetName = message.getMessageType();
            String localSfId = message.getSenderId();
            if(localSfId.isEmpty()){
                localSfId = physicalHomePartner.getSsId();
            }
            if("-".equals(datasetName)){
                StringBuilder sb = new StringBuilder();
                sb.append(UUID.randomUUID().toString());
                datasetName = sb.toString();
            }

            File dataFile = createDataFile(oftpServerProperties.getBaseDirectory(), logicalPartner.getSsId(), logicalPartner.getSfId(), payload, datasetName);
            boolean isEnveloped = logicalPartner.isFileCompressionYn() || logicalPartner.isFileSignYn() || logicalPartner.isFileCompressionYn();
            EnvelopedVirtualFile envelopedVirtualFile;
            if (isEnveloped) {
                envelopedVirtualFile = convertEnvelopedOftpFile(dataFile, localSfId, logicalPartner.getSfId(), datasetName, logicalPartner, physicalHomePartner);
            } else {
                envelopedVirtualFile = createVirtualFile(oftpServerProperties.getBaseDirectory(), localSfId, logicalPartner.getSsId(), logicalPartner.getSfId(), dataFile);
            }

            // 3. Virtual File 저장
            File virtualFileDir = new File(oftpServerProperties.getBaseDirectory(), String.join(File.separator, logicalPartner.getSsId(), logicalPartner.getSfId(), "outbox", "vfile"));
            OftpServerUtil.storeVirtualFile(envelopedVirtualFile, virtualFileDir);

            // 4. oftp client 호출
            //findGatewayBySsId(logicalPartner.getPartnerSsId());

            // 5. 성공시 mbox의 상태값을 변경하고 큐에서 제거
            messageQueue.poll();


        } catch (Exception e) {
            messageQueue.poll();
            //moveMessageToExceptionFolder(message);
            logger.error("Failed to process and send message: {}. Moving to exception folder.", e.getMessage(), e);
        }
    }

    public EnvelopedVirtualFile convertEnvelopedOftpFile(File originFile, String homeSFID, String remoteSFID, String datasetName, PhysicalPartner.LogicalPartner logicalPartner, PhysicalPartner physicalHomePartner) throws IOException, OdetteFtpException{
        DefaultEnvelopedVirtualFile vf = new DefaultEnvelopedVirtualFile();
        vf.setOriginator(homeSFID);
        vf.setDatasetName(datasetName);
        vf.setDateTime(new Date());
        vf.setDestination(remoteSFID);
        vf.setSignedNotificationRequest(logicalPartner.isSignedEerpRequestYn());
		vf.setEnvelopingFormat(FileEnveloping.CMS);
        SecurityLevel securityLevel = SecurityLevel.parse((logicalPartner.isFileSignYn()?1:0) + (logicalPartner.isFileEncryptionYn()?2:0));
        vf.setSecurityLevel(securityLevel);
        vf.setCipherSuite(CipherSuite.AES_RSA_SHA1);        
        vf.setCompressionAlgorithm(logicalPartner.isFileCompressionYn()?FileCompression.ZLIB:FileCompression.NO_COMPRESSION);
        
		PrivateKey homePrivateKey = null;
		X509Certificate homeCert = null;
        X509Certificate remoteCert = null;
        if(securityLevel.equals(SecurityLevel.SIGNED) || securityLevel.equals(SecurityLevel.ENCRYPTED_AND_SIGNED)){
            try {
                Keystore keystore = new Keystore(physicalHomePartner.getKeystorePath(), physicalHomePartner.getKeystorePassword().toCharArray());                
                homePrivateKey = keystore.getPrivateKey();
                homeCert = SecurityUtil.getCertificateEntry(keystore.getKeyStore());
            } catch (Exception e) {
                logger.error("Cannot parse enveloped file. Error loading user private key or certificate: " + e.getMessage(), e);
                throw new IOException("Cannot parse enveloped file. Error loading user private key or certificate: " + e.getMessage(), e);
            }
        }
        if(securityLevel.equals(SecurityLevel.ENCRYPTED) || securityLevel.equals(SecurityLevel.ENCRYPTED_AND_SIGNED)){        
            try {
                remoteCert = SecurityUtil.openCertificate(new File(logicalPartner.getFileSigningCertPath()));
            } catch (Exception e) {
                logger.error("cannot parse enveloped file. Error loading partner certificate: " + e.getMessage(), e);
                throw new IOException("cannot parse enveloped file. Error loading partner certificate: " + e.getMessage(), e);
            }
        }
        
        File envelopedFile = File.createTempFile("enveloped_"+originFile.getName() + "_", null, originFile.getParentFile());

        OftpServerUtil.createEnvelopedFile(originFile, envelopedFile, vf, homeCert, homePrivateKey, remoteCert);

        vf.setFile(envelopedFile);
		vf.setOriginalFileSize(originFile.length());
		vf.setSize(envelopedFile.length());
        //originFile.delete();
        return vf;
    }

    public EnvelopedVirtualFile createVirtualFile(String baseDirectory, String localSfId, String remoteSsId, String remoteSfId, File dataFile) {
        File remoteSfidDir = new File(baseDirectory, String.join(File.separator, remoteSsId, remoteSfId, "outbox"));
        String datasetName = dataFile.getName();
        String virtualFileName = createVirtualFileName(localSfId, remoteSfId, new Date(), datasetName);
        File dataDir = new File(remoteSfidDir, "data");
        try {
            FileUtil.notExistCreateDir(dataDir);
            File tempOriginData = File.createTempFile(virtualFileName + "_", null, dataDir);
            OftpServerUtil.fileMove(dataFile, tempOriginData);
            return convertOftpFile(tempOriginData, localSfId, remoteSfId, datasetName);
        } catch (IOException e) {
            logger.error("Error", e);
            throw new OftpException(ErrorCode.CONFLICT_FILE);
        }
    }

    public EnvelopedVirtualFile convertOftpFile(File tempOriginData, String localSfId, String remoteSfId, String datasetName) throws IOException{
        DefaultEnvelopedVirtualFile vf = new DefaultEnvelopedVirtualFile();
        vf.setOriginator(localSfId);
        vf.setDatasetName(datasetName);
        vf.setDateTime(new Date());
        vf.setDestination(remoteSfId);
        vf.setFile(tempOriginData);
        vf.setSize(tempOriginData.length());
        return vf;
    }

    public static String createVirtualFileName(String homeSFID, String remoteSFID, Date date, String datasetName) {
        return String.join("$",
				homeSFID,
				remoteSFID,
				ProtocolUtil.formatDate("yyyyMMddHHmmSS.sss", date),
				datasetName,
				".vfile");
	}

    public File createDataFile(String baseDirectory, String partnerSsid, String partnerSfid, byte[] data, String dataSetName) throws IOException {
        File partnerDirectory = new File(baseDirectory, String.join(File.separator, partnerSsid, partnerSfid, "outbox"));
        File dataDir = new File(partnerDirectory, "org_data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        String fileName = "NOT-DEFINE-CATEGORY";
        if(dataSetName!=null && !dataSetName.isEmpty())
            fileName = dataSetName;
        File localVf = new File(dataDir, fileName);
        FileUtil.storeFile(localVf, data);
        return localVf;
    }

    public String getPayloadFromMessageBox(MessageRecord messageRecord, String endPoint) {
        try (MessageBoxClient client = new MessageBoxClient(endPoint)) {
            byte[] payload = client.getPayload(messageRecord.getMessage_box_id(), messageRecord.getMessage_slot_id(), messageRecord.getMessage_id());
            return new String(payload, StandardCharsets.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new OftpException(ErrorCode.PAYLOAD_ENCODING_ERROR);
        } catch (Exception e) {
            throw new OftpException(ErrorCode.MBOX_CLIENT_ERROR, e.getMessage());
        }
    }

    public List<MessageRecordWithAttachment> getWaitMessageList(String slotId, String endPoint) throws Exception {
        List<MessageRecordWithAttachment> recordList = new ArrayList<>();
        try (MessageBoxClient client = new MessageBoxClient(endPoint)) {
            PendingMessageRecordQuery query = new PendingMessageRecordQuery();
            query.setSlotId(slotId);
            query.setStatus(MessageStatus.TBDL);
            recordList.addAll(client.getPendingRecords(query));
            query.setStatus(MessageStatus.WAIT);
            recordList.addAll(client.getPendingRecords(query));
        }
        return recordList;
    }

}

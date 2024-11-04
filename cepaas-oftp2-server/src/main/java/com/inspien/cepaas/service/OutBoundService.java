package com.inspien.cepaas.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.inspien.cepaas.auth.Keystore;
import com.inspien.cepaas.client.MessageBoxClient;
import com.inspien.cepaas.client.OftpMetaInfo;
import com.inspien.cepaas.config.OftpServerProperties;
import com.inspien.cepaas.config.OftpServerProperties.PhysicalPartner;
import com.inspien.cepaas.enums.ErrorCode;
import com.inspien.cepaas.exception.OftpException;
import com.inspien.cepaas.msgbox.api.MessageDirection;
import com.inspien.cepaas.msgbox.api.MessageId;
import com.inspien.cepaas.msgbox.api.MessagePostRequest;
import com.inspien.cepaas.msgbox.api.MessageRecord;
import com.inspien.cepaas.msgbox.api.MessageStatus;
import com.inspien.cepaas.util.OftpServerUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.neociclo.odetteftp.protocol.OdetteFtpObject;
import org.neociclo.odetteftp.protocol.VirtualFile;
import org.neociclo.odetteftp.protocol.v20.EnvelopedVirtualFile;
import org.neociclo.odetteftp.protocol.v20.SecurityLevel;
import org.neociclo.odetteftp.util.OdetteFtpSupport;
import org.neociclo.odetteftp.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class OutBoundService {

    private static final Logger logger = LoggerFactory.getLogger(OutBoundService.class);
    private final OftpServerProperties oftpServerProperties;
    private final ConcurrentLinkedQueue<File> fileQueue = new ConcurrentLinkedQueue<>();

    public OutBoundService(OftpServerProperties oftpServerProperties) {
        this.oftpServerProperties = oftpServerProperties;
    }

    @Scheduled(fixedRate = 60000)
    public void scheduleFileProcessing() {
        if (fileQueue.isEmpty()) {
            enqueueFiles();
        }

        if (!fileQueue.isEmpty()) {
            processOutBoundDataFiles();
        }
    }

    private void enqueueFiles() {
        //TODO Outbound mbox를 가지고 있는 SFID들에 대해 로직 수행
        File directory = new File(oftpServerProperties.getBaseDirectory() + "/MENDELSON_SSID/work");
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                fileQueue.addAll(Arrays.asList(files));
            }
        }
    }

    public void processOutBoundDataFiles() {
        while (!fileQueue.isEmpty()) {
            File file = fileQueue.poll();
            if (file != null) {
                processAndSendFile(file);
            }
        }
    }

    private void processAndSendFile(File file) {
        try {
            OdetteFtpObject odetteFtpObject = (OdetteFtpObject) OftpServerUtil.loadSerializableObject(file);
            PhysicalPartner.LogicalPartner logicalPartner;
            PhysicalPartner physicalHomePartner = oftpServerProperties.findHomePartner();
            if (odetteFtpObject instanceof VirtualFile) {
                VirtualFile virtualFile = (VirtualFile) odetteFtpObject;
                File dataFile = virtualFile.getFile();
                logicalPartner = oftpServerProperties.findLogicalPartnerBySfId(virtualFile.getDestination());
                Map<String, String> customUserField = new HashMap<>();
                customUserField.put("oftp-dataSetName", virtualFile.getDatasetName());
                customUserField.put("oftp-dateTime", new SimpleDateFormat("yyyyMMddHHmmss").format(virtualFile.getDateTime()));
                customUserField.put("oftp-timeCounter", virtualFile.getTicker().toString());
                EnvelopedVirtualFile vf = (EnvelopedVirtualFile) virtualFile;
                if (vf.getSecurityLevel() == SecurityLevel.ENCRYPTED || vf.getSecurityLevel() == SecurityLevel.ENCRYPTED_AND_SIGNED) {
                    File deEnvelopFile = new File(dataFile.getParent(), "decrypted_" + dataFile.getName());
                    Keystore keystore = new Keystore(physicalHomePartner.getKeystorePath(), physicalHomePartner.getKeystorePassword().toCharArray());
                    OdetteFtpSupport.parseEnvelopedFile(dataFile, deEnvelopFile, vf, keystore.getCertificate(), keystore.getPrivateKey(), SecurityUtil.openCertificate(new File(logicalPartner.getFileSigningCertPath())));
                    storeMessage(oftpServerProperties.getMessageBoxId(), logicalPartner.getSlotId(), oftpServerProperties.getMessageBoxEndPoint(), 
                        OftpMetaInfo.of(virtualFile), OftpServerUtil.fileToByteArray(deEnvelopFile, false, ""), customUserField, MessageStatus.TBDL);
                        moveFileToProcessedFileFolder(deEnvelopFile);
                } else {
                    storeMessage(oftpServerProperties.getMessageBoxId(), logicalPartner.getSlotId(), oftpServerProperties.getMessageBoxEndPoint(),
                        OftpMetaInfo.of(virtualFile), OftpServerUtil.fileToByteArray(dataFile, false, ""), customUserField, MessageStatus.TBDL);
                }
                logger.info("Message stored:{}",dataFile.getName());
                moveFileToProcessedFileFolder(dataFile);
                moveFileToProcessedFileFolder(file);
            }
            //TODO notification 필요
        } catch (Exception e) {
            moveFileToExceptionFolder(file);
            logger.error("Failed to process and send file: {}. Moving to exception folder.", file.getName(), e);
        }
    }

    private void moveFileToExceptionFolder(File file) {
        try {
            File exceptionDir = new File(oftpServerProperties.getBaseDirectory() + "/exceptions");
            if (!exceptionDir.exists()) {
                exceptionDir.mkdirs();
            }
            File destFile = new File(exceptionDir, file.getName());
            OftpServerUtil.fileMove(file, destFile);
        } catch (IOException e) {
            throw new OftpException(ErrorCode.FILE_MOVE_FAIL, "Failed to move file to exception folder: " + file.getAbsolutePath());
        }
    }

    private void moveFileToProcessedFileFolder(File file) {
        try {
            File exceptionDir = new File(oftpServerProperties.getBaseDirectory() + "/processed");
            if (!exceptionDir.exists()) {
                exceptionDir.mkdirs();
            }
            File destFile = new File(exceptionDir, file.getName());
            OftpServerUtil.fileMove(file, destFile);
        } catch (IOException e) {
            throw new OftpException(ErrorCode.FILE_MOVE_FAIL, "Failed to move file to processed folder: " + file.getAbsolutePath());
        }
    }

    public void storeMessage(String messageBoxId, String slotId, String endpoint, OftpMetaInfo info, byte[] payload, Map<String,String> customUserField, MessageStatus messageStatus) throws IOException {
        try (MessageBoxClient client = new MessageBoxClient(endpoint)) {
            MessageRecord messageRecord = createRecord(messageBoxId, slotId, info, payload, customUserField, messageStatus);
            MessagePostRequest request = new MessagePostRequest();
            request.setPayload(payload);
            request.setRecord(messageRecord);
            client.createMessage(request);
        }
    }

    private MessageRecord createRecord(String messageBoxId, String slotId, OftpMetaInfo info, byte[] payload, Map<String,String> customUserField, MessageStatus messageStatus){
        MessageRecord messageRecord = new MessageRecord();
        long createTime = System.currentTimeMillis();
        messageRecord.setMessage_id(new MessageId(System.currentTimeMillis()).toString());
        messageRecord.setMessage_box_id(messageBoxId);
        messageRecord.setMessage_slot_id(slotId);
        messageRecord.setCreate_time(createTime);
        messageRecord.setMessage_status(messageStatus);
        messageRecord.setMessageType(info.getDatasetName());
        messageRecord.setDirection(MessageDirection.RECV);
        messageRecord.setReceiverId(info.getDestination());
        messageRecord.setSenderId(info.getOriginator());
        messageRecord.setPayloadSize(payload.length);
        if (customUserField != null)
            messageRecord.setUserFields(customUserField);
        return messageRecord;
    }
}

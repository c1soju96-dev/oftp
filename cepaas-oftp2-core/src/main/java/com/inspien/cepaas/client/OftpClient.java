package com.inspien.cepaas.client;

import org.neociclo.odetteftp.TransferMode;
import org.neociclo.odetteftp.oftplet.OftpletFactory;
import org.neociclo.odetteftp.protocol.DefaultVirtualFile;
import org.neociclo.odetteftp.protocol.OdetteFtpObject;
import org.neociclo.odetteftp.security.MappedCallbackHandler;
import org.neociclo.odetteftp.security.PasswordCallback;
import org.neociclo.odetteftp.service.TcpClient;
import org.neociclo.odetteftp.support.InOutSharedQueueOftpletFactory;
import org.neociclo.odetteftp.support.OdetteFtpConfiguration;
import org.neociclo.odetteftp.support.PasswordHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inspien.cepaas.enums.ErrorCode;
import com.inspien.cepaas.exception.OftpException;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OftpClient implements IOftpClient {

    private static final Logger logger = LoggerFactory.getLogger(OftpClient.class);

    @Override
    public void sendFile(String host, int port, String remoteSsId, String userPassword, File payload, String destination) {

        // OFTP 설정
        OdetteFtpConfiguration conf = new OdetteFtpConfiguration();
        conf.setTransferMode(TransferMode.SENDER_ONLY);

        // 보안 콜백 핸들러 설정
        MappedCallbackHandler securityCallbacks = new MappedCallbackHandler();
        securityCallbacks.addHandler(PasswordCallback.class, new PasswordHandler(remoteSsId, userPassword));

        // 전송할 파일 큐 설정
        Queue<OdetteFtpObject> filesToSend = new ConcurrentLinkedQueue<>();
        DefaultVirtualFile vf = new DefaultVirtualFile();
        vf.setDatasetName(payload.getName());
        vf.setDestination(destination);
        vf.setFile(payload);

        filesToSend.offer(vf);

        // Oftplet 팩토리 설정
        OftpletFactory factory = new InOutSharedQueueOftpletFactory(conf, securityCallbacks, filesToSend, null, null);

        // OFTP 클라이언트 설정 및 연결
        TcpClient oftpClient = new TcpClient();
        oftpClient.setOftpletFactory(factory);

        // 서버에 연결하고 파일 전송 시작
        try {
            oftpClient.connect(host, port, true);
        } catch (Exception e) {
            throw new OftpException(ErrorCode.RESPONSE_NOT_SUCCESS);
        }
        logger.info("File sent successfully to {} ", destination);
        //TODO 원본 파일 삭제 또는 이동
    }
}
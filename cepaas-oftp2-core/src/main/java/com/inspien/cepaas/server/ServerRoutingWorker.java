package com.inspien.cepaas.server;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.neociclo.odetteftp.protocol.OdetteFtpObject;
import org.neociclo.odetteftp.util.IoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inspien.cepaas.util.OftpServerUtil;

class ServerRoutingWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerRoutingWorker.class);

    static class MakeDeliveryTask implements Runnable {

        private final String userCode;
        private final OdetteFtpObject obj;
        private final File baseDir;

        public MakeDeliveryTask(File baseDir, String userCode, OdetteFtpObject obj) {
            this.baseDir = baseDir;
            this.userCode = userCode;
            this.obj = obj;
        }

        @Override
        public void run() {
            String filename = OftpServerUtil.createFileName(obj, UUID::randomUUID);

            File sourceDir = OftpServerUtil.getUserWorkDir(baseDir, userCode);
            ensureDirectoryExists(sourceDir);
            File sourceFile = new File(sourceDir, filename);

            String recipientOid = obj.getDestination();
            File destDir = OftpServerUtil.getUserMailboxDir(baseDir, recipientOid);
            ensureDirectoryExists(destDir);
            File destFile = new File(destDir, filename);

            if (destFile.exists()) {
                LOGGER.warn("Delivery failed. Duplicate file in recipient mailbox. This is a simple server " +
                        "implementation and it doesn't support delivery retries. Overwriting file: {}", destFile);
            }

            try {
                IoUtil.move(sourceFile, destFile);
                LOGGER.info("Delivered to [{}]: {}", recipientOid, obj);
            } catch (IOException e) {
                LOGGER.error("Delivery failed. Cannot move object file to the recipient box [{}]. Source File: {}", recipientOid, sourceFile);
            }
        }

        void ensureDirectoryExists(File dir) {
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }

    private final ExecutorService executor;

    public ServerRoutingWorker() {
        this.executor = Executors.newCachedThreadPool();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
    }

    public void deliver(File baseDir, String userCode, OdetteFtpObject obj) {
        MakeDeliveryTask task = new MakeDeliveryTask(baseDir, userCode, obj);
        executor.submit(task);
    }

	// 테스트 전용 메서드 추가
    void awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        executor.awaitTermination(timeout, unit);
    }
	
}
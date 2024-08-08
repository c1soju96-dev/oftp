package com.inspien.cepaas.server;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.neociclo.odetteftp.protocol.OdetteFtpObject;
import org.neociclo.odetteftp.util.IoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ServerRoutingWorker {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerRoutingWorker.class);

	private static class MakeDeliveryTask implements Runnable {
	
		private String userCode;
		private OdetteFtpObject obj;
		private File baseDir;
	
		public MakeDeliveryTask(File baseDir, String userCode, OdetteFtpObject obj) {
			super();
			this.baseDir = baseDir;
			this.userCode = userCode;
			this.obj = obj;
		}
	
		public void run() {
	
			String filename = OftpServerHelper.createFileName(obj);
	
			File sourceDir = OftpServerHelper.getUserWorkDir(baseDir, userCode);
			if (!sourceDir.exists()) {
    			sourceDir.mkdirs();
    		}
			File sourceFile = new File(sourceDir, filename);
	
			String recipientOid = obj.getDestination();
			File destDir = OftpServerHelper.getUserMailboxDir(baseDir, recipientOid);
			if (!destDir.exists()) {
    			destDir.mkdirs();
    		}
			File destFile = new File(destDir, filename);
	
			if (destFile.exists()) {
				LOGGER.warn("Delivery failed. Duplicate file in recipient mailbox. This is a simple server " +
						"implementation and it doesn't support delivery retries. Overwriting file: {}", destFile);
			}

			try {
				LOGGER.trace("EXISTS: {}", sourceFile.exists());
				IoUtil.move(sourceFile, destFile);
				LOGGER.info("Delivered to [{}]: ", recipientOid, obj);
			} catch (IOException e) {
				LOGGER.info("Delivery failed. Cannot move object file to the recipient box [{}]: {}", recipientOid,
						sourceFile);
				LOGGER.error("Routing failed.", e);
				return;
			}
	
		}
		
	}

	private ExecutorService executor;

	public ServerRoutingWorker() {
		super();
		this.executor = Executors.newCachedThreadPool();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				executor.shutdown();
			}
		}));
	}

	public void deliver(File baseDir, String userCode, OdetteFtpObject obj) {

		ServerRoutingWorker.MakeDeliveryTask task = new MakeDeliveryTask(baseDir, userCode, obj);
		executor.submit(task);

	}
}
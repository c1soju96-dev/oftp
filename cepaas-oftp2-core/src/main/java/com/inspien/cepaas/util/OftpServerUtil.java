package com.inspien.cepaas.util;

import java.io.*;
import java.util.Date;
import java.util.UUID;
import java.util.function.Supplier;

import org.neociclo.odetteftp.protocol.OdetteFtpObject;
import org.neociclo.odetteftp.protocol.VirtualFile;
import org.neociclo.odetteftp.util.ProtocolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OftpServerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(OftpServerUtil.class);

    private static final FilenameFilter EXCHANGES_FILENAME_FILTER = (dir, name) -> 
            name.endsWith(".vfile") || name.endsWith(".notif");

    private OftpServerUtil() {
    }

    public static void createUserDirStructureIfNotExist(String userCode, File serverBaseDir) {
        File dataDir = getServerDataDir(serverBaseDir);
        File mailboxDir = getUserMailboxDir(serverBaseDir, userCode);
        File workDir = getUserWorkDir(serverBaseDir, userCode);

        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        if (!mailboxDir.exists()) {
            mailboxDir.mkdirs();
        }

        if (!workDir.exists()) {
            workDir.mkdirs();
        }
    }

	public static String createFileName(OdetteFtpObject obj, Supplier<UUID> uuidSupplier) {
		StringBuffer sb = new StringBuffer();
		String uuid = uuidSupplier.get().toString();
		sb.append(uuid).append('$');
		sb.append(obj.getOriginator()).append('$');
		sb.append(obj.getDestination()).append('$');
		Date dateTime = obj.getDateTime();
		if (dateTime != null) {
			sb.append(ProtocolUtil.formatDate("yyyyMMddHHmmSS.sss", dateTime)).append('$');
		}
		sb.append(obj.getDatasetName());
		if (obj instanceof VirtualFile) {
			sb.append(".vfile");
		} else {
			sb.append(".notif");
		}
		return sb.toString();
	}

    public static File getServerDataDir(File baseDir) {
        return new File(baseDir, "data");
    }

    public static File getUserDir(File baseDir, String userCode) {
        return new File(baseDir, userCode.toLowerCase());
    }

    public static File getUserMailboxDir(File baseDir, String userCode) {
        return new File(getUserDir(baseDir, userCode), "mailbox");
    }

    public static File getUserWorkDir(File baseDir, String userCode) {
        return new File(getUserDir(baseDir, userCode), "work");
    }

    public static File getUserConfigFile(File baseDir, String userCode) {
        return new File(getUserDir(baseDir, userCode), "accord-oftp.conf");
    }

    public static OdetteFtpObject loadObject(File input) throws IOException {
        OdetteFtpObject obj = null;

        try (FileInputStream stream = new FileInputStream(input);
             ObjectInputStream os = new ObjectInputStream(stream)) {
            obj = (OdetteFtpObject) os.readObject();
        } catch (ClassNotFoundException cnfe) {
            LOGGER.error("Cannot load Odette FTP Object file: " + input, cnfe);
        }

        return obj;
    }

    public static void storeObject(File output, OdetteFtpObject obj) throws IOException {
        try (FileOutputStream stream = new FileOutputStream(output);
             ObjectOutputStream os = new ObjectOutputStream(stream)) {
            os.writeObject(obj);
        }
    }

    public static File createDataFile(VirtualFile vf, File serverBaseDir, Supplier<UUID> uuidSupplier) throws IOException {
        String filename = createFileName(vf, uuidSupplier);
        File dataDir = getServerDataDir(serverBaseDir);
        return File.createTempFile(filename + "_", null, dataDir);
    }

    public static void storeInWork(String userCode, OdetteFtpObject obj, File serverBaseDir, Supplier<UUID> uuidSupplier) throws IOException {
        File workDir = getUserWorkDir(serverBaseDir, userCode);
        String filename = createFileName(obj, uuidSupplier);
        File outputFile = new File(workDir, filename);
        storeObject(outputFile, obj);
    }

    public static void storeInMailbox(String userCode, OdetteFtpObject obj, File serverBaseDir, Supplier<UUID> uuidSupplier) throws IOException {
        File mailboxDir = getUserMailboxDir(serverBaseDir, userCode);
        String filename = createFileName(obj, uuidSupplier);
        File outputFile = new File(mailboxDir, filename);
        storeObject(outputFile, obj);
    }

    public static File[] listExchanges(String userCode, File serverBaseDir) {
        File mailboxDir = getUserMailboxDir(serverBaseDir, userCode);
        return mailboxDir.listFiles(EXCHANGES_FILENAME_FILTER);
    }

    public static boolean hasExchange(String userCode, File serverBaseDir) {
        File[] exchanges = listExchanges(userCode, serverBaseDir);
        return (exchanges != null && exchanges.length > 0);
    }

    public static void deleteExchange(String userCode, OdetteFtpObject obj, File serverBaseDir) {
        if (obj instanceof VirtualFile) {
            VirtualFile vf = (VirtualFile) obj;
            File payloadFile = vf.getFile();
            if (payloadFile.exists()) {
                payloadFile.delete();
            }
        }

        File mailboxDir = getUserMailboxDir(serverBaseDir, userCode);
        String filename = createFileName(obj, UUID::randomUUID);
        File mailboxFile = new File(mailboxDir, filename);

        if (mailboxFile.exists()) {
            mailboxFile.delete();
        }
    }

    public static File createFile(VirtualFile vf, File tmpDir, Supplier<UUID> uuidSupplier) throws IOException {
        String filename = createVirtualFileName(vf);
        return File.createTempFile(filename + "_", null, tmpDir);
    }

    public static String createVirtualFileName(OdetteFtpObject obj) {
        String sb = String.join("$",
                obj.getOriginator(),
                obj.getDestination(),
                ProtocolUtil.formatDate("yyyyMMddHHmmSS.sss", obj.getDateTime()),
                obj.getDatasetName());
        if (obj instanceof VirtualFile) {
            sb += ".vfile";
        } else {
            sb += ".notif";
        }
        return sb;
    }
}

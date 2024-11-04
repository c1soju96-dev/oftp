package com.inspien.cepaas.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Supplier;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.neociclo.odetteftp.protocol.OdetteFtpObject;
import org.neociclo.odetteftp.protocol.VirtualFile;
import org.neociclo.odetteftp.protocol.v20.CipherSuite;
import org.neociclo.odetteftp.protocol.v20.EnvelopedVirtualFile;
import org.neociclo.odetteftp.protocol.v20.FileCompression;
import org.neociclo.odetteftp.protocol.v20.FileEnveloping;
import org.neociclo.odetteftp.protocol.v20.SecurityLevel;
import org.neociclo.odetteftp.util.EnvelopingException;
import org.neociclo.odetteftp.util.EnvelopingUtil;
import org.neociclo.odetteftp.util.IoUtil;
import org.neociclo.odetteftp.util.ProtocolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inspien.cepaas.enums.ErrorCode;
import com.inspien.cepaas.exception.OftpException;

public class OftpServerUtil {

    private static final Logger logger = LoggerFactory.getLogger(OftpServerUtil.class);

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
        StringBuilder sb = new StringBuilder();
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
            logger.error("Cannot load Odette FTP Object file: " + input, cnfe);
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
        if (!workDir.exists() && !workDir.mkdirs()) {
            throw new IOException("Failed to create work directory: " + workDir.getAbsolutePath());
        }
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
                try {
					Files.delete(payloadFile.toPath());
					logger.info("Successfully deleted the file:{}", payloadFile.getName());
				} catch (IOException deleteException) {
					logger.error("Failed to delete the file: " + payloadFile, deleteException);
				}
            }
        }

        File mailboxDir = getUserMailboxDir(serverBaseDir, userCode);
        String filename = createFileName(obj, UUID::randomUUID);
        File mailboxFile = new File(mailboxDir, filename);

        if (mailboxFile.exists()) {
            try {
                Files.delete(mailboxFile.toPath());
                logger.info("Successfully deleted the file:{}", mailboxFile.getName());
            } catch (IOException deleteException) {
                logger.error("Failed to delete the file: " + mailboxFile, deleteException);
            }
        }
    }

    public static Object loadSerializableObject(File input) throws IOException {
        Object obj = null;
        try(FileInputStream stream = new FileInputStream(input);
            ObjectInputStream os = new ObjectInputStream(stream);){
            obj = os.readObject();
        } catch (ClassNotFoundException e) {
            logger.error("Cannot load Odette FTP Object file: " + input, e);
        }
        return obj;
    }

    // public static void fileMove(File sourceFile, File destFile) throws IOException {
    //     try {
    //         Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    //         Files.delete(sourceFile.toPath());
    //         logger.info("File moved successfully from {} to {}", sourceFile.getAbsolutePath(), destFile.getAbsolutePath());
    //     } catch (IOException e) {
    //         throw new IOException("Failed to move file: " + sourceFile.getAbsolutePath(), e);
    //     }
    // }

    public static void fileMove(File file, File tempOriginData) throws IOException {
        try {
            Files.move(file.toPath(), tempOriginData.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IOException("Cannot create directory: " + file.getAbsolutePath());
        }
    }

    public static byte[] fileToByteArray(File file, boolean isBase64Encode, String sourceEncoding) throws IOException {
		byte[] bArray = new byte[(int) file.length()];
		try(FileInputStream fis = new FileInputStream(file)){
			int readByte = fis.read(bArray);
			if(readByte != bArray.length){
				throw new IOException("Failed to read file: " + file + "\nRead byte: " + readByte + ", File length: " + bArray.length);
			}
		}
		if (isBase64Encode) {
			return Base64.getEncoder().encode(bArray);
		}else{
			if (sourceEncoding.isEmpty() || sourceEncoding.equals("UTF-8")){
				return bArray;
			}
			String str = new String(bArray, sourceEncoding);
			return str.getBytes(StandardCharsets.UTF_8);
		}
	}

    public static void createEnvelopedFile(File input, File output, EnvelopedVirtualFile virtualFile) throws EnvelopingException {
        createEnvelopedFile(input, output, virtualFile, (X509Certificate)null, (PrivateKey)null, (X509Certificate)null);
    }

    public static void createEnvelopedFile(File input, File output, EnvelopedVirtualFile virtualFile, X509Certificate userCert, PrivateKey userPrivateKey, X509Certificate partnerCert) throws EnvelopingException {
        SecurityLevel securityLevel = virtualFile.getSecurityLevel() == null ? SecurityLevel.NO_SECURITY_SERVICES : virtualFile.getSecurityLevel();
        CipherSuite cipherSel = virtualFile.getCipherSuite() == null ? CipherSuite.NO_CIPHER_SUITE_SELECTION : virtualFile.getCipherSuite();
        FileCompression compressionAlgo = virtualFile.getCompressionAlgorithm() == null ? FileCompression.NO_COMPRESSION : virtualFile.getCompressionAlgorithm();
        FileEnveloping envelopingFormat = virtualFile.getEnvelopingFormat() == null ? FileEnveloping.NO_ENVELOPE : virtualFile.getEnvelopingFormat();
        createEnvelopedFile(input, output, securityLevel, cipherSel, compressionAlgo, envelopingFormat, partnerCert, userCert, userPrivateKey);
   }

   public static void createEnvelopedFile(File input, File output, SecurityLevel securityLevel, CipherSuite cipherSel, FileCompression compressionAlgo, FileEnveloping envelopingFormat, X509Certificate partnerCert, X509Certificate userCert, PrivateKey userPrivateKey) throws EnvelopingException {
        String[] argNames = new String[]{"input", "output", "securityLevel", "cipherSel", "compressionAlgo", "envelopingFormat"};
        Object[] argValues = new Object[]{input, output, securityLevel, cipherSel, compressionAlgo, envelopingFormat};

        for(int i = 0; i < argValues.length; ++i) {
            if (argValues[i] == null) {
                throw new NullPointerException(argNames[i]);
            }
        }

        if (envelopingFormat == FileEnveloping.NO_ENVELOPE) {
            throw new EnvelopingException("Cannot parse enveloped file. Incompatible parameter: envelopingFormat=NO_ENVELOPE.");
        } else {
            boolean isSigned = securityLevel == SecurityLevel.SIGNED || securityLevel == SecurityLevel.ENCRYPTED_AND_SIGNED;
            boolean isEncrypted = securityLevel == SecurityLevel.ENCRYPTED || securityLevel == SecurityLevel.ENCRYPTED_AND_SIGNED;
            boolean isCompressed = compressionAlgo != FileCompression.NO_COMPRESSION;
            if (isSigned) {
                if (cipherSel == CipherSuite.NO_CIPHER_SUITE_SELECTION) {
                    throw new EnvelopingException("Cannot parse enveloped file. No signature algorithm specified (cipherSel=NO_CIPHER_SUITE_SELECTION).");
                }
                if (userCert == null) {
                    throw new NullPointerException("userCert");
                }
                if (userPrivateKey == null) {
                    throw new NullPointerException("userKey");
                }
            }
            if (isEncrypted) {
                if (cipherSel == CipherSuite.NO_CIPHER_SUITE_SELECTION) {
                    throw new EnvelopingException("Cannot parse enveloped file. No encryption algorithm specified (cipherSel=NO_CIPHER_SUITE_SELECTION).");
                }
                if (partnerCert == null) {
                    throw new NullPointerException("partnerCert");
                }
            }

            OutputStream outStream = null;

            try {
                outStream = new FileOutputStream(output, false);
            } catch (FileNotFoundException var21) {
                throw new EnvelopingException("Failed to create enveloped file. Cannot open output file: " + output, var21);
            }

            ArrayList<OutputStream> toClose = new ArrayList();
            toClose.add(outStream);

            try {
                if (isEncrypted) {
                outStream = EnvelopingUtil.openEnvelopedDataStreamGenerator((OutputStream)outStream, cipherSel, partnerCert);
                toClose.add(0, outStream);
                }

                if (isCompressed) {
                outStream = EnvelopingUtil.openCompressedDataStreamGenerator((OutputStream)outStream);
                toClose.add(0, outStream);
                }

                if (isSigned) {
                outStream = EnvelopingUtil.openSignedDataStreamGenerator((OutputStream)outStream, cipherSel, userCert, userPrivateKey);
                toClose.add(0, outStream);
                }
            } catch (Exception var23) {
                throw new EnvelopingException("Failed to create enveloped file. Cannot open CMS output processings.", var23);
            }

            FileInputStream inStream = null;

            try {
                inStream = new FileInputStream(input);
                IoUtil.copyStream(inStream, (OutputStream)outStream);
            } catch (FileNotFoundException var19) {
                throw new EnvelopingException("Failed to create enveloped file. Source file doesn't exist: " + input, var19);
            } catch (IOException var20) {
                throw new EnvelopingException("Failed to create enveloped file. Buffer copying error.", var20);
            }

            try {
                inStream.close();
                Iterator var17 = toClose.iterator();

                while(var17.hasNext()) {
                OutputStream stream = (OutputStream)var17.next();
                stream.flush();
                stream.close();
                }

            } catch (IOException var22) {
                throw new EnvelopingException("Failed to close enveloped output stream: " + output, var22);
            }
        }
    }

    public static void storeVirtualFile(EnvelopedVirtualFile envelopedVirtualFile, File virtualFileDir) throws IOException {

        notExistCreateDir(virtualFileDir);
        File file = new File(virtualFileDir, envelopedVirtualFile.getDatasetName() + ".vfile");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(envelopedVirtualFile);
            logger.info("EnvelopedVirtualFile has been saved to: {}", file.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to save EnvelopedVirtualFile: {}", file.getAbsolutePath(), e);
            throw new OftpException(ErrorCode.FILE_MOVE_FAIL);
        }
    }

    public static void notExistCreateDir(File file) throws IOException {
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("Cannot create directory: " + file.getAbsolutePath());
        }
    }

}

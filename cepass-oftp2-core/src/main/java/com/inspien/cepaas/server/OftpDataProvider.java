// package com.inspien.cepaas.server;

// import java.io.File;
// import java.io.IOException;

// import org.neociclo.odetteftp.protocol.OdetteFtpObject;
// import org.neociclo.odetteftp.protocol.VirtualFile;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import com.inspien.cepaas.util.FileUtil;

// import static com.inspien.cepaas.server.OftpServerHelper.*;

// public class OftpDataProvider{
//     private static final Logger logger = LoggerFactory.getLogger(OftpDataProvider.class);
//     private File dataDir;

//     public OftpDataProvider(File dataDir){
//         this.dataDir = dataDir;
//     }

//     public File storeData(String remoteSSID, OdetteFtpObject ob) {
//         File homeStationPendingDir = new File(dataDir, String.join(File.separator, remoteSSID, ob.getDestination(), "data"));
//         if (!homeStationPendingDir.exists()) {
//             homeStationPendingDir.mkdirs();
//         }
//         try {
//             File tmpData = createFile((VirtualFile)ob, homeStationPendingDir);
//             return tmpData;
//         } catch (IOException e) {
//             logger.error("Failed to store data file, remoteSSID:{}, remoteSFID:{}", remoteSSID, ob.getDestination(), e);
//         }
//         return null;
//     }

//     public File storeEnvelopedData(String remoteSSID, OdetteFtpObject ob) {
//         File homeStationPendingDir = new File(dataDir, String.join(File.separator, remoteSSID, ob.getDestination(), "enveloped-data"));
//         if (!homeStationPendingDir.exists()) {
//             homeStationPendingDir.mkdirs();
//         }
//         try {
//             File tmpData = createFile((VirtualFile)ob, homeStationPendingDir);
//             return tmpData;
//         } catch (IOException e) {
//             logger.error("Failed to store data file, remoteSSID:{}, remoteSFID:{}", remoteSSID, ob.getDestination(), e);
//         }
//         return null;
//     }

//     public boolean existHomeSFID(String remoteSSID, String homeSFID) {
//         File homeStationDir = new File(dataDir, remoteSSID + File.separator + homeSFID);
//         return homeStationDir.exists() && homeStationDir.isDirectory();
//     }

//     public void storeVirtualFile(String remoteSSID, OdetteFtpObject ob) throws IOException {
//         File homeStationDir = new File(dataDir, String.join(File.separator, remoteSSID, ob.getDestination()));
//         File homeStationDataDir = new File(homeStationDir, "vf-data");
//         if (!homeStationDataDir.exists()) {
//             homeStationDataDir.mkdirs();
//         }
//         String fn = createVirtualFileName(ob);
//         File localVf = new File(homeStationDataDir, fn);
//         FileUtil.storeSerializableObject(localVf, ob);
//     }

//     public OdetteFtpObject getRestartFile(String remoteSSID, String homeSFID, OdetteFtpObject vf) throws IOException {
//         File processDir = new File(dataDir, String.join(File.separator, remoteSSID, homeSFID, "vf-process"));
//         String vfName = createVirtualFileName(vf);
//         File restartFile = new File(processDir, vfName);
//         return (OdetteFtpObject)FileUtil.loadSerializableObject(restartFile);
//     }
// }
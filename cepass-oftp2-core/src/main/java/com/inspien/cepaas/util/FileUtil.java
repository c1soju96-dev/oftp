package com.inspien.cepaas.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;

public class FileUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    public static void storeSerializableObject(File output, Object obj) throws IOException {
        try (FileOutputStream stream = new FileOutputStream(output);
             ObjectOutputStream os = new ObjectOutputStream(stream);){
            os.writeObject(obj);
            os.flush();
            stream.flush();
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

    public static void fileMove(File sourceFile, File destFile) throws IOException{
        try (InputStream inStream = Files.newInputStream(sourceFile.toPath());
             OutputStream outStream = Files.newOutputStream(destFile.toPath())) {

            byte[] buffer = new byte[1024];
            int length;

            while ((length = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, length);
            }
        } catch (IOException e) {
            throw new IOException("Failed to move file: " + sourceFile.getAbsolutePath(), e);
        }
        sourceFile.delete();
    }

    public static void notExistCreateDir(File f) throws IOException {
        if (!f.exists()) {
            if(!f.mkdirs()) throw new IOException("Cannot create directory: " + f.getAbsolutePath());
        }
    }

}

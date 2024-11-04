package com.inspien.cepaas.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    public static void storeFile(File output, byte[] data) throws IOException {
		try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(data);
            stream.flush();
        }
	}

    public static void notExistCreateDir(File file) throws IOException {
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("Cannot create directory: " + file.getAbsolutePath());
        }
    }

    public static void fileMove(File file, File tempOriginData) throws IOException {
        try {
            Files.move(file.toPath(), tempOriginData.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IOException("Cannot create directory: " + file.getAbsolutePath());
        }
    }
    
}

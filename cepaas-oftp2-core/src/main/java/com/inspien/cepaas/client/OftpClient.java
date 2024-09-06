package com.inspien.cepaas.client;

import java.io.File;

public interface OftpClient {
    void sendFile(String host, int port, String userCode, String userPassword, File payload, String destination);
}
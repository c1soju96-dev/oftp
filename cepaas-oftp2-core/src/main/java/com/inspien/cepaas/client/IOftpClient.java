package com.inspien.cepaas.client;

import java.io.File;

public interface IOftpClient {
    void sendFile(String host, int port, String remoteSsId, String userPassword, File payload, String destination);
}
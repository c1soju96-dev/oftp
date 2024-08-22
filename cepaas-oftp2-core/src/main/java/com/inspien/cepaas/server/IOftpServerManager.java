package com.inspien.cepaas.server;

import com.inspien.cepaas.exception.OftpException;

public interface IOftpServerManager {

    void startServer() throws OftpException;

    void stopServer();

    void restartServer();

    boolean isRunning();
}

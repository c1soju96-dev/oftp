package com.inspien.cepaas.exception;

import com.inspien.cepaas.enums.ErrorCode;

public class AuthException extends OftpException {
    protected AuthException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
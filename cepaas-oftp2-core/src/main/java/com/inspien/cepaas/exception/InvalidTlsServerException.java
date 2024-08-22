package com.inspien.cepaas.exception;

import com.inspien.cepaas.enums.ErrorCode;

public class InvalidTlsServerException extends OftpException{
    public static final ErrorCode CODE = ErrorCode.INVALID_TLS_SERVER_SETTING;
    public static final String MESSAGE = ErrorCode.INVALID_TLS_SERVER_SETTING.getDetail();

    public InvalidTlsServerException() {
        super(CODE, MESSAGE);
    }
}

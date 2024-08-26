package com.inspien.cepaas.exception;

import com.inspien.cepaas.enums.ErrorCode;

import lombok.Getter;

@Getter
public class OftpException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String message;

    public OftpException(ErrorCode errorCode) {
        super(errorCode.getDetail());
        this.errorCode = errorCode;
        this.message = errorCode.getDetail();
    }

    public OftpException(ErrorCode errorCode, String message) {
        super(errorCode.getDetail() + ": " + message);
        this.errorCode = errorCode;
        this.message = errorCode.getDetail() + message;
    }

}
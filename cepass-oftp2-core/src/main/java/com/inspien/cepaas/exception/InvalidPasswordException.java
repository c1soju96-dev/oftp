package com.inspien.cepaas.exception;

import com.inspien.cepaas.enums.ErrorCode;

public class InvalidPasswordException extends AuthException{
    public static final ErrorCode CODE = ErrorCode.INVALID_PASSWORD;
    public static final String MESSAGE = ErrorCode.INVALID_PASSWORD.getDetail();

    public InvalidPasswordException() {
        super(CODE, MESSAGE);
    }
}

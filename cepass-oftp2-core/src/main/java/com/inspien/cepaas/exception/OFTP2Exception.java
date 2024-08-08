package com.inspien.cepaas.exception;

public class OFTP2Exception extends Exception {
    private String errorCode;
    private String errorMessage;

    public OFTP2Exception(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
package com.inspien.cepaas.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // Auth
    INVALID_AUTH(101, "Authentication is invalid."),
    INVALID_PASSWORD(102, "Password is invalid."),

    INVALID_TLS_SERVER_SETTING(110, "TLS Server setting is invalid."),
    INVALID_SERVER_SETTING(120, "Server setting is invalid."),
    // Property
    INVALID_FILE(103, "File is invalid.");

    private final int code;
    private final String detail;
}

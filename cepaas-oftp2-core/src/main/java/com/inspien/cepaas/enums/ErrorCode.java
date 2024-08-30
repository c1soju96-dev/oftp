package com.inspien.cepaas.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // Protocol, Client, Oftp-Server, Service (P, C, O, S)
    // 100 : Invalid
    // 200 : Success
    // 400 : Bad Request
    // 401 : Unauthorized
    // 404 : Not Found
    // 409 : Conflict
    // 500 : Server Error

    /* OFTP Server */
    // Invalid (100)
    INVALID_AUTH("O101", "Authentication is invalid."),
    INVALID_PASSWORD("O102", "Password is invalid."),
    INVALID_KEY("O101", "Key is invalid."),
    INVALID_TLS_SERVER_SETTING("O110", "TLS Server setting is invalid."),
    INVALID_SERVER_SETTING("O120", "Server setting is invalid."),
    INVALID_FILE("O103", "File is invalid."),

    // Conflict (300)
    CONFLICT_FILE("301", "Conflict File."),

    // NOT FOUND (400)
    NOT_FOUND_KEY("O401", "Not Found Key."),
    NOT_FOUND_FILE("O402", "Not Found File."),
    NOT_FOUND_DIRECTORY("O403", "Not Found Directory."),
    FILE_MOVE_FAIL("O404", "File Move Fail"),

    // SEVER (500)
    REQUEST_FAILED("O501", "Request failed"),
    RESPONSE_NOT_SUCCESS("O502", "Response Not Success.");

    /* Service */

    /* Client */

    private final String code;
    private final String detail;
}

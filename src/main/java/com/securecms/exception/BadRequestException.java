package com.securecms.exception;

import lombok.Getter;

/** Generic 400 BAD REQUEST for business-rule violations (e.g. invalid sort field). */
@Getter
public class BadRequestException extends RuntimeException {

    private final String errorCode;

    public BadRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

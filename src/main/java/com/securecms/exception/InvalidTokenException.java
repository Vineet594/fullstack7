package com.securecms.exception;

import lombok.Getter;

/** Thrown for invalid / expired / revoked JWT access tokens and refresh tokens (401). */
@Getter
public class InvalidTokenException extends RuntimeException {

    private final String errorCode;

    public InvalidTokenException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

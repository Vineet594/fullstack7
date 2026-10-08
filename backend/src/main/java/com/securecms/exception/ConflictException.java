package com.securecms.exception;

import lombok.Getter;

/** Thrown for duplicate records or state conflicts (HTTP 409). */
@Getter
public class ConflictException extends RuntimeException {
    private final String errorCode;

    public ConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

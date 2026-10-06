package com.securecms.exception;

import lombok.Getter;

/** Generic 409 CONFLICT (duplicate category name, category still in use, ...). */
@Getter
public class ResourceConflictException extends RuntimeException {

    private final String errorCode;

    public ResourceConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

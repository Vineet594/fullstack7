package com.securecms.exception;

public class UserAlreadyExistsException extends ConflictException {
    public UserAlreadyExistsException(String errorCode, String message) {
        super(errorCode, message);
    }
}

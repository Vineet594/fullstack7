package com.securecms.exception;

import lombok.Getter;

import java.util.Locale;

@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String errorCode;

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found with id: " + id);
        this.errorCode = resource.toUpperCase(Locale.ROOT).replace(' ', '_') + "_NOT_FOUND";
    }
}

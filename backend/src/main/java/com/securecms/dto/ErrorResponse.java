package com.securecms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(boolean success, String message, String errorCode, Instant timestamp,
                            String path, String correlationId, Map<String, String> validationErrors) {

    public static ErrorResponse of(String message, String errorCode, String path, Map<String, String> validationErrors) {
        return new ErrorResponse(false, message, errorCode, Instant.now(), path, MDC.get("correlationId"), validationErrors);
    }
}

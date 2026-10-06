package com.securecms.dto;

import java.time.Instant;

/** Standard success envelope used by every endpoint. */
public record ApiResponse<T>(boolean success, String message, T data, Instant timestamp) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }
}

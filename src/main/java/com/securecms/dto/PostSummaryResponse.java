package com.securecms.dto;

import java.time.Instant;

/** Lightweight projection used by the list screen: only 5 columns are read from the database. */
public record PostSummaryResponse(
        Long id,
        String title,
        String authorUsername,
        String categoryName,
        Instant createdAt) {
}

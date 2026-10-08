package com.securecms.util;

import com.securecms.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/** Builds a safe Pageable: caps page size and only allows whitelisted sort fields. */
public final class PageableFactory {
    public static final int MAX_PAGE_SIZE = 50;

    private PageableFactory() {
    }

    public static Pageable create(int page, int size, String sort, Set<String> allowedSortFields) {
        if (page < 0) {
            throw new BadRequestException("INVALID_PAGE", "Page index must be 0 or greater");
        }
        if (size < 1) {
            throw new BadRequestException("INVALID_SIZE", "Page size must be at least 1");
        }
        int safeSize = Math.min(size, MAX_PAGE_SIZE);

        String[] parts = sort.split(",");
        String field = parts[0].trim();
        if (!allowedSortFields.contains(field)) {
            throw new BadRequestException("INVALID_SORT_FIELD", "Sorting is only allowed by: " + allowedSortFields);
        }
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1) {
            String dir = parts[1].trim();
            if (dir.equalsIgnoreCase("desc")) {
                direction = Sort.Direction.DESC;
            } else if (!dir.equalsIgnoreCase("asc")) {
                throw new BadRequestException("INVALID_SORT_DIRECTION", "Sort direction must be 'asc' or 'desc'");
            }
        }
        return PageRequest.of(page, safeSize, Sort.by(direction, field));
    }
}

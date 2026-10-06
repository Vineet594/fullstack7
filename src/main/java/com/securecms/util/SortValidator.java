package com.securecms.util;

import com.securecms.exception.BadRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/** Only whitelisted fields may be used for sorting (prevents errors and information probing). */
public final class SortValidator {

    private SortValidator() {
    }

    public static Pageable validate(Pageable pageable, Set<String> allowedFields) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedFields.contains(order.getProperty())) {
                throw new BadRequestException("INVALID_SORT_FIELD",
                        "Sorting by '" + order.getProperty() + "' is not allowed. Allowed fields: "
                                + String.join(", ", allowedFields));
            }
        }
        return pageable;
    }
}

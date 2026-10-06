package com.securecms.filter;

import com.securecms.util.RequestContextKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gives every request a correlation ID:
 *  - reuses the client's X-Correlation-ID header if it is present and safe
 *  - otherwise generates a new UUID
 *  - stores it in the logging MDC so every log line of this request carries it
 *  - returns it in the response header
 * Runs first (highest precedence) so even security errors have a correlation ID.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-ID";

    /** Only accept simple values - prevents log injection through a malicious header. */
    private static final Pattern SAFE_VALUE = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER_NAME);
        if (correlationId == null || !SAFE_VALUE.matcher(correlationId).matches()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(RequestContextKeys.MDC_CORRELATION_ID, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}

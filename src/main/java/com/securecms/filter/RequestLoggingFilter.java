package com.securecms.filter;

import com.securecms.util.RequestContextKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs one line per request:  POST /api/posts - user=vineet - status=201 - time=85ms
 * Only the method, path, user, status and duration are logged - never headers, bodies,
 * query strings, passwords or tokens.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            Object username = request.getAttribute(RequestContextKeys.ATTR_AUTHENTICATED_USERNAME);
            int status = response.getStatus();

            if (status >= 500) {
                log.error("{} {} - user={} - status={} - time={}ms", request.getMethod(),
                        request.getRequestURI(), username != null ? username : "anonymous", status, durationMs);
            } else if (status >= 400) {
                log.warn("{} {} - user={} - status={} - time={}ms", request.getMethod(),
                        request.getRequestURI(), username != null ? username : "anonymous", status, durationMs);
            } else {
                log.info("{} {} - user={} - status={} - time={}ms", request.getMethod(),
                        request.getRequestURI(), username != null ? username : "anonymous", status, durationMs);
            }
        }
    }
}

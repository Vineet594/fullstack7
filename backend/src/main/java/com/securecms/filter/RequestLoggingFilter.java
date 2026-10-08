package com.securecms.filter;

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
 * Logs method, URL, user, status and duration of every request, e.g.
 * POST /api/posts - user=alice - status=201 - time=85ms
 * Never logs passwords, tokens, headers, bodies or query strings.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String USER_ATTRIBUTE = "authenticatedUsername";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            Object user = request.getAttribute(USER_ATTRIBUTE);
            log.info("{} {} - user={} - status={} - time={}ms", request.getMethod(), request.getRequestURI(),
                    user != null ? user : "anonymous", response.getStatus(), elapsedMs);
        }
    }
}

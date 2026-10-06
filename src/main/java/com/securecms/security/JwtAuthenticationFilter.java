package com.securecms.security;

import com.securecms.exception.InvalidTokenException;
import com.securecms.util.RequestContextKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs once per request, before Spring's own authentication filter:
 *  1. read the Authorization header
 *  2. check that it starts with "Bearer "
 *  3. validate the JWT (signature + expiry)
 *  4. put the authenticated user (id, username, role) into the SecurityContext
 *  5. continue the filter chain
 *
 * If the token is invalid the context stays empty and the error is remembered as a request attribute;
 * {@link JwtAuthenticationEntryPoint} then answers with a proper 401 JSON response for protected endpoints.
 *
 * NOTE: intentionally NOT a Spring @Component - it is created inside SecurityConfig so that it only
 * runs inside the security filter chain (not twice).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                AuthenticatedUser user = jwtService.parseAccessToken(token);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                request.setAttribute(RequestContextKeys.ATTR_AUTHENTICATED_USERNAME, user.getUsername());
                MDC.put(RequestContextKeys.MDC_USERNAME, user.getUsername());
            } catch (InvalidTokenException ex) {
                SecurityContextHolder.clearContext();
                request.setAttribute(RequestContextKeys.ATTR_JWT_ERROR, ex);
            }
        }

        filterChain.doFilter(request, response);
    }
}

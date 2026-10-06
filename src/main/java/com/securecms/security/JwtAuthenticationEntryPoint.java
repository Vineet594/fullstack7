package com.securecms.security;

import com.securecms.exception.InvalidTokenException;
import com.securecms.util.RequestContextKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Called when an unauthenticated caller reaches a protected endpoint -> 401 UNAUTHORIZED. */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter errorWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object jwtError = request.getAttribute(RequestContextKeys.ATTR_JWT_ERROR);
        if (jwtError instanceof InvalidTokenException tokenError) {
            errorWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                    tokenError.getErrorCode(), tokenError.getMessage());
        } else {
            errorWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                    "AUTHENTICATION_REQUIRED", "Authentication is required to access this resource");
        }
    }
}

package com.securecms.util;

/** Central place for MDC keys and request attribute names (avoids "magic strings"). */
public final class RequestContextKeys {

    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_USERNAME = "username";
    public static final String ATTR_AUTHENTICATED_USERNAME = "securecms.authenticatedUsername";
    public static final String ATTR_JWT_ERROR = "securecms.jwtError";

    private RequestContextKeys() {
    }
}

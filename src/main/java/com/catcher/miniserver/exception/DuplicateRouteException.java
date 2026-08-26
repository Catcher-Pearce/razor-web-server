package com.catcher.miniserver.exception;

import com.catcher.miniserver.http.HttpMethod;

public final class DuplicateRouteException extends RuntimeException {
    public DuplicateRouteException(HttpMethod method, String path) {
        super("Route already registered: " + method + " " + path);
    }
}

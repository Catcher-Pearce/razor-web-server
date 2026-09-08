package io.github.catcherpearce.razorserver.exception;

import io.github.catcherpearce.razorserver.http.HttpMethod;

public final class DuplicateRouteException extends RuntimeException {
    public DuplicateRouteException(HttpMethod method, String path) {
        super("Route already registered: " + method + " " + path);
    }
}

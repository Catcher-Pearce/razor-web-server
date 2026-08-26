package com.catcher.miniserver.exception;

import com.catcher.miniserver.http.HttpMethod;

import java.util.Set;

public final class MethodNotAllowedException extends HttpException {
    private final Set<HttpMethod> allowedMethods;

    public MethodNotAllowedException(HttpMethod method, String path, Set<HttpMethod> allowedMethods) {
        super(405, method + " is not allowed for path: " + path);
        this.allowedMethods = Set.copyOf(allowedMethods);
    }

    public Set<HttpMethod> allowedMethods() {
        return allowedMethods;
    }
}

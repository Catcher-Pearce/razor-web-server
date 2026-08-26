package com.catcher.miniserver.exception;

import java.util.List;

public final class RequestValidationException extends HttpException {
    private final List<String> violations;

    public RequestValidationException(List<String> violations) {
        super(400, "Request validation failed: " + String.join(", ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> violations() {
        return violations;
    }
}

package com.catcher.miniserver.exception;

public abstract class HttpException extends RuntimeException {
    private final int statusCode;

    protected HttpException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    protected HttpException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}

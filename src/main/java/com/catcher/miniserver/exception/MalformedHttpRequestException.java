package com.catcher.miniserver.exception;

public final class MalformedHttpRequestException extends HttpException {
    public MalformedHttpRequestException(String message) {
        super(400, message);
    }

    public MalformedHttpRequestException(String message, Throwable cause) {
        super(400, message, cause);
    }
}

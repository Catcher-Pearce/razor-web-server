package com.catcher.miniserver.exception;

public final class UnsupportedMediaTypeException extends HttpException {
    public UnsupportedMediaTypeException(String contentType) {
        super(415, contentType == null
                ? "Missing Content-Type"
                : "Unsupported Content-Type: " + contentType);
    }
}

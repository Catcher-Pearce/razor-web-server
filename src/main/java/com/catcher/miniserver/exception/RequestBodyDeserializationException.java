package com.catcher.miniserver.exception;

public final class RequestBodyDeserializationException extends HttpException {
    public RequestBodyDeserializationException(Throwable cause) {
        super(400, "Request body could not be deserialized", cause);
    }
}

package io.github.catcherpearce.razorserver.exception;

public final class RequestTimeoutException extends HttpException {
    public RequestTimeoutException() {
        super(408, "Request timed out.");
    }
}

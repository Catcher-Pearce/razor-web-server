package io.github.catcherpearce.razorserver.exception;

public final class InvalidRoutePatternException extends RuntimeException {
    public InvalidRoutePatternException(String route, String reason) {
        super("Invalid route pattern '" + route + "': " + reason);
    }
}

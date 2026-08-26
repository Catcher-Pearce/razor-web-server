package com.catcher.miniserver.exception;

public final class RouteNotFoundException extends HttpException {
    public RouteNotFoundException(String path) {
        super(404, "No route found for path: " + path);
    }
}

package com.catcher.miniserver.server;

import com.catcher.miniserver.http.HttpRequest;

import java.util.Map;

public class RequestMapper {
    public ServerRequest map(Route route, HttpRequest request, Map<String, String> pathVariables) {
        return new ServerRequest(
                request.method(),
                request.path(),
                pathVariables,
                null,
                request.headers(),
                request.body()
        );
    }
}

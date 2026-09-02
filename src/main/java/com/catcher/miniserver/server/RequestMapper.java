package com.catcher.miniserver.server;


import com.catcher.miniserver.http.HttpRequest;
import com.catcher.miniserver.validation.RequestDeserializer;

import java.util.Map;

public class RequestMapper {
    private final RequestDeserializer requestDeserializer;

    public RequestMapper() {
        this.requestDeserializer = new RequestDeserializer();
    }

    public ServerRequest map(
            Route route,
            HttpRequest request,
            Map<String, String> pathVariables
    ) {
        Object body = request.body();

        if (route.requestShape() != null) {
            body = requestDeserializer.deserialize(
                    request.body(),
                    request.headers().get("content-type"),
                    route.requestShape()
            );
        }

        return new ServerRequest(
                request.method(),
                request.path(),
                pathVariables,
                null,
                request.headers(),
                body
        );
    }
}
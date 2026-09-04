package com.catcher.miniserver.server;

import com.catcher.miniserver.http.HttpRequest;
import com.catcher.miniserver.validation.RequestDeserializer;
import com.catcher.miniserver.validation.RequestShape;
import com.catcher.miniserver.validation.RequestValidator;

import java.util.Map;

/**
 * Converts an internally parsed HTTP request into the request exposed to a
 * route handler. For a route with a registered request shape, it also
 * deserializes and validates the body; otherwise, the raw string body is
 * preserved.
 */
public class RequestMapper {
    private final RequestDeserializer requestDeserializer;
    private final RequestValidator requestValidator;

    public RequestMapper() {
        this.requestDeserializer = new RequestDeserializer();
        this.requestValidator = new RequestValidator();
    }

    /**
     * Builds a handler-facing request using routing information captured while
     * matching the request. If the route declares a request shape, the body is
     * converted to that type and must pass validation before this method
     * returns.
     *
     * @param route matched route and its optional request-body shape
     * @param request parsed HTTP request
     * @param pathVariables path variables captured from the matched route
     * @param queryParams decoded query parameters from the request target
     * @return the request passed to the route handler
     */
    public ServerRequest map(
            Route route,
            HttpRequest request,
            Map<String, String> pathVariables,
            Map<String, String> queryParams
    ) {
        Object body = request.body();

        if (route.requestShape() != null) {
            RequestShape requestShape = requestDeserializer.deserialize(
                    request.body(),
                    request.headers().get("content-type"),
                    route.requestShape()
            );

            requestValidator.validate(requestShape);
            body = requestShape;
        }

        return new ServerRequest(
                request.method(),
                request.path(),
                pathVariables,
                queryParams,
                request.headers(),
                body
        );
    }
}

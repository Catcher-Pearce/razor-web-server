package com.catcher.miniserver.server;

import com.catcher.miniserver.http.HttpMethod;

import java.util.Map;

/**
 * The request information exposed to a route handler.
 *
 * <p>For routes registered with a request shape, {@link #body()} contains the
 * deserialized and validated request object. For routes without a request
 * shape, it contains the raw request body as a {@link String}.</p>
 *
 * @param method HTTP method used for the request
 * @param path request path, excluding query parameters
 * @param pathVariables values captured from variables in the route pattern
 * @param queryParams decoded query parameters from the request target
 * @param headers request headers, keyed by normalized header name
 * @param body raw or deserialized request body, depending on route registration
 */
public record ServerRequest(
        HttpMethod method,
        String path,
        Map<String, String> pathVariables,
        Map<String, String> queryParams,
        Map<String, String> headers,
        Object body
) {
    /**
     * Returns the request body cast to the supplied type.
     *
     * <p>This is intended for routes registered with the same request-shape
     * class. Such bodies have already been deserialized and validated before
     * the route handler is invoked.</p>
     *
     * @param type expected body type
     * @param <T> expected body type
     * @return the request body cast to {@code type}
     * @throws ClassCastException if the body is not an instance of {@code type}
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public <T> T bodyAs(Class<T> type) {
        return type.cast(body);
    }
}

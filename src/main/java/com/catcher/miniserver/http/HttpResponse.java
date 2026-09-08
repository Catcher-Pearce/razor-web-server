package com.catcher.miniserver.http;

import java.util.Map;

/**
 * An HTTP response returned by a request handler.
 *
 * @param status HTTP status code
 * @param headers response headers, or null when none are supplied
 * @param body response body, or null for an empty response
 */
public record HttpResponse (
        int status,
        Map<String, String> headers,
        Object body
) {
    /**
     * Creates a response with the supplied status and no headers or body.
     *
     * @param status HTTP status code
     */
    public HttpResponse(int status) {
        this(status, null, null);
    }
}



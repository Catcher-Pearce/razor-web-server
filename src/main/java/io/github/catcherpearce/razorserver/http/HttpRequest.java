package io.github.catcherpearce.razorserver.http;

import java.util.Map;


/**
 * A parsed HTTP request before route matching and request-body mapping.
 *
 * @param method HTTP method accepted by the parser
 * @param path raw request path including any query string, with an absolute
 *             URL's scheme and authority removed
 * @param version HTTP version from the request line
 * @param headers request headers keyed by lowercase name; for absolute URLs,
 *                the host entry contains the URL authority
 * @param body request body decoded as UTF-8, or an empty string when absent
 */
public record HttpRequest (
    HttpMethod method,
    String path,
    String version,
    Map<String, String> headers,
    String body
) {}

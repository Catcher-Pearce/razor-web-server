package com.catcher.miniserver.server;

import java.util.List;
import java.util.Map;

/**
 * The result of matching a request path and method against registered routes.
 *
 * @param route matched request path without its query string
 * @param pathVariables raw captured segment values in path order, corresponding
 *                      to the variable names in the matched route
 * @param queryParameters decoded query names and values
 * @param matchedRoute selected handler and its request-mapping metadata
 */
record RouteMatch(
        String route,
        List<String> pathVariables,
        Map<String, String> queryParameters,
        Route matchedRoute
) {
}

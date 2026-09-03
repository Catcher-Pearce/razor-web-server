package com.catcher.miniserver.server;

import java.util.List;
import java.util.Map;

record RouteMatch(
        String route,
        List<String> pathVariables,
        Map<String, String> queryParameters,
        Route matchedRoute
) {
}

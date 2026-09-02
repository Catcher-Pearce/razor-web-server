package com.catcher.miniserver.server;

import java.util.List;

record RouteMatch(
        String route,
        List<String> pathVariables,
        Route matchedRoute
) {
}

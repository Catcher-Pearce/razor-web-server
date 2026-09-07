package com.catcher.miniserver.server;

import java.util.List;

public record ParsedRoute(
        List<String> path,
        List<String> pathVariables
) {
}
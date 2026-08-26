package com.catcher.miniserver.server;

import java.util.List;

public record ParsedRoute(
        String path,
        List<String> pathVariables
) {
}
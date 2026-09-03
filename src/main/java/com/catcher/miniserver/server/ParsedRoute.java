package com.catcher.miniserver.server;

import java.util.List;
import java.util.Map;

public record ParsedRoute(
        String path,
        List<String> pathVariables
) {
}
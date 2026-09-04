package com.catcher.miniserver.server;

import com.catcher.miniserver.exception.InvalidRoutePatternException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses registered routes for correctness and normalizes the route
 * for proper route mapping
 */
public class RoutePattern {
    private static final Pattern LITERAL_SEGMENT = Pattern.compile("[A-Za-z0-9._~-]+");
    private static final Pattern VARIABLE_SEGMENT = Pattern.compile("\\{([A-Za-z_][A-Za-z0-9_]*)}");

    private final String route;

    public RoutePattern(String route) {
        this.route = route;
    }

    public ParsedRoute normalizeRoute() {
        validateRoute();

        if (route.equals("/")) {
            return new ParsedRoute("/", List.of());
        }

        String[] segments = route.substring(1).split("/", -1);
        List<String> serializedSegments = new ArrayList<>();
        List<String> pathVariables = new ArrayList<>();
        Set<String> variableNames = new HashSet<>();

        for (String segment : segments) {
            Matcher variableMatcher = VARIABLE_SEGMENT.matcher(segment);

            if (variableMatcher.matches()) {
                String variableName = variableMatcher.group(1);
                if (!variableNames.add(variableName)) {
                    throw invalid("duplicate path variable name: " + variableName);
                }
                serializedSegments.add("{}");
                pathVariables.add(variableName);
            } else if (LITERAL_SEGMENT.matcher(segment).matches()) {
                serializedSegments.add(segment);
            } else {
                throw invalid("each path segment must be a literal or a path variable");
            }
        }

        return new ParsedRoute(
                "/" + String.join("/", serializedSegments),
                List.copyOf(pathVariables)
        );
    }

    private void validateRoute() {
        if (route == null || route.isEmpty()) {
            throw invalid("route cannot be empty");
        }
        if (!route.startsWith("/")) {
            throw invalid("route must start with '/'");
        }
        if (route.length() > 1 && route.endsWith("/")) {
            throw invalid("route cannot end with '/'");
        }
        if (route.contains("//")) {
            throw invalid("route cannot contain empty path segments");
        }
    }

    private InvalidRoutePatternException invalid(String reason) {
        return new InvalidRoutePatternException(route, reason);
    }
}

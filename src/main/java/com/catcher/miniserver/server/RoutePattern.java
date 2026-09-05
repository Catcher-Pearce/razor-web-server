package com.catcher.miniserver.server;

import com.catcher.miniserver.exception.InvalidRoutePatternException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates registered route patterns and normalizes named path variables
 * to {@code {}} placeholders for route mapping.
 * Literal segments are preserved, and variable names are collected in path order.
 */
public class RoutePattern {
    private static final Pattern LITERAL_SEGMENT = Pattern.compile("[A-Za-z0-9._~-]+");
    private static final Pattern VARIABLE_SEGMENT = Pattern.compile("\\{([A-Za-z_][A-Za-z0-9_]*)}");

    private final String route;

    /**
     * Stores a route pattern for later validation and normalization.
     *
     * @param route the route pattern, such as {@code /users/{id}};
     *              validated when {@link #normalizeRoute()} is called
     */
    public RoutePattern(String route) {
        this.route = route;
    }

    /**
     * Validates the route and replaces each named variable segment with {@code {}}.
     * For example, {@code /users/{id}} produces the path {@code /users/{}}
     * and the variable list {@code [id]}. The root route {@code /} produces
     * an empty variable list.
     *
     * <p>Routes must start with {@code /} and must not contain empty segments or
     * a trailing slash, except for the root route. Literal segments may contain
     * ASCII letters, digits, dots, underscores, tildes, and hyphens. Variables
     * must occupy an entire segment and have unique, case-sensitive names
     * matching {@code [A-Za-z_][A-Za-z0-9_]*} enclosed in braces.
     *
     * @return the normalized path and an immutable list of variable names in path order
     * @throws InvalidRoutePatternException if the route is null, empty, malformed,
     *         or contains duplicate variable names
     */
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

    /**
     * Checks that the route is nonempty, starts with a slash, and contains
     * neither empty segments nor a trailing slash other than the root slash.
     * Segment contents are checked separately during normalization.
     *
     * @throws InvalidRoutePatternException if the route fails these structural checks
     */
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

    /**
     * Creates an exception describing a validation failure for this route.
     *
     * @param reason the reason the route is invalid
     * @return an exception containing the route and failure reason
     */
    private InvalidRoutePatternException invalid(String reason) {
        return new InvalidRoutePatternException(route, reason);
    }
}

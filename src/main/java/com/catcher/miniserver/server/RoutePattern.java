package com.catcher.miniserver.server;

import com.catcher.miniserver.exception.InvalidRoutePatternException;

import java.util.ArrayList;

public class RoutePattern {
    private final String route;

    public RoutePattern(String route) {
        this.route = route;
    }

    public ParsedRoute serializeRoute() {
        StringBuilder builder = new StringBuilder();
        StringBuilder pathVariableBuilder = new StringBuilder();
        ArrayList<String> pathVariables = new ArrayList<String>();
        boolean inPathVariable = false;
        if (route == null || route.isEmpty()) {
            throw new InvalidRoutePatternException(route, "route cannot be empty");
        }

        if (route.charAt(0) == '{') {
            throw new InvalidRoutePatternException(route, "route must begin with a literal path segment");
        }

        for (char c : route.toCharArray()) {

            if (c == '{') {
                if (inPathVariable) {
                    throw new InvalidRoutePatternException(route, "nested path variables are not allowed");
                }

                pathVariableBuilder = new StringBuilder();
                inPathVariable = true;
                builder.append("{}");

            } else if (c == '}') {
                if (!inPathVariable || pathVariableBuilder.toString().isEmpty()) {
                    throw new InvalidRoutePatternException(route, "path variable must have a name and matching braces");
                }

                inPathVariable = false;
                pathVariables.add(pathVariableBuilder.toString());

            } else if (!inPathVariable) {
                builder.append(c);
            } else {
                pathVariableBuilder.append(c);
            }
        }

        if (inPathVariable) {
            throw new InvalidRoutePatternException(route, "path variable is missing a closing brace");
        }

        return new ParsedRoute(
                builder.toString(),
                pathVariables
        );
    }
}

package com.catcher.miniserver.server;

import com.catcher.miniserver.exception.MethodNotAllowedException;
import com.catcher.miniserver.exception.RouteNotFoundException;
import com.catcher.miniserver.http.HttpMethod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class RouteMatcher {
    RouteMatch match(
            Map<String, Map<HttpMethod, Route>> routes,
            String requestPath,
            HttpMethod requestMethod
    ) {
        // TODO: Separate the query string from the path before matching. Currently it
        // becomes part of the final literal segment or captured path-variable value.
        String[] requestRoute = requestPath.split("/");
        String parsedRequestRoute = "";
        ArrayList<String> parsedPathVariables = new ArrayList<>();
        List<Integer> bestScore = new ArrayList<>();
        Route bestRoute = null;
        Set<HttpMethod> allowedMethods = new HashSet<>();

        for (String route : routes.keySet()) {
            ArrayList<String> pathVariables = new ArrayList<>();
            boolean matched = true;
            String[] mappedRoute = route.split("/");
            List<Integer> currScore = new ArrayList<>();


            if (mappedRoute.length != requestRoute.length) {
                continue;
            }

            for (int i = 0; i < mappedRoute.length; i++) {
                if (!requestRoute[i].equals(mappedRoute[i]) && !mappedRoute[i].equals("{}")) {
                    matched = false;
                    break;
                } else if (mappedRoute[i].equals("{}")) {
                    // TODO: Define whether captured path variables should be URL-decoded.
                    pathVariables.add(requestRoute[i]);
                    currScore.add(0);
                } else {
                    currScore.add(1);
                }
            }
            if (!matched) {
                continue;
            }

            Map<HttpMethod, Route> routesByMethod = routes.get(route);
            allowedMethods.addAll(routesByMethod.keySet());
            Route candidateRoute = routesByMethod.get(requestMethod);

            // A matching path with the wrong method must not hide another matching
            // path that can handle the requested method.
            if (candidateRoute == null) {
                continue;
            }

            if (bestRoute == null || isMoreSpecific(currScore, bestScore)) {
                parsedRequestRoute = route;
                parsedPathVariables = pathVariables;
                bestScore = List.copyOf(currScore);
                bestRoute = candidateRoute;
            }
        }

        if (allowedMethods.isEmpty()) {
            throw new RouteNotFoundException(requestPath);
        }

        if (bestRoute == null) {
            throw new MethodNotAllowedException(
                    requestMethod,
                    requestPath,
                    allowedMethods
            );
        }

        return new RouteMatch(parsedRequestRoute, parsedPathVariables, bestRoute);
    }

    private boolean isMoreSpecific(List<Integer> candidate, List<Integer> currentBest) {
        for (int i = 0; i < candidate.size(); i++) {
            int comparison = Integer.compare(candidate.get(i), currentBest.get(i));
            if (comparison != 0) {
                return comparison > 0;
            }
        }

        return false;
    }
}

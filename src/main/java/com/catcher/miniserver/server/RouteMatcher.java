package com.catcher.miniserver.server;

import com.catcher.miniserver.exception.MalformedHttpRequestException;
import com.catcher.miniserver.exception.MethodNotAllowedException;
import com.catcher.miniserver.exception.RouteNotFoundException;
import com.catcher.miniserver.http.HttpMethod;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

class RouteMatcher {
    private static final int MAX_QUERY_LENGTH = 8_192;
    private static final int MAX_QUERY_PARAMETERS = 100;

    /**
     * Matches a request target and HTTP method against the registered routes.
     * The first literal {@code ?} separates the path from the raw query. Query
     * parameters are parsed according to the rules documented by
     * {@link #extractQueryParameters(String)} and do not participate in route
     * matching.
     *
     * <p>A route segment equal to {@code {}} matches any request-path segment
     * and captures its value. When multiple routes match, literal segments take
     * precedence over variable segments, compared from left to right. Only
     * routes registered for {@code requestMethod} are eligible for selection.</p>
     *
     * @param routes registered route patterns grouped by supported HTTP method
     * @param requestPath request target containing a path and an optional query
     *                    string
     * @param requestMethod HTTP method used for the request
     * @return the selected route together with its captured path variables and
     *         decoded query parameters
     * @throws MalformedHttpRequestException if the query string is malformed
     * @throws RouteNotFoundException if no registered route pattern matches the
     *                                request path
     * @throws MethodNotAllowedException if the path matches at least one route,
     *                                   but none supports {@code requestMethod}
     */
    RouteMatch match(
            Map<String, Map<HttpMethod, Route>> routes,
            String requestPath,
            HttpMethod requestMethod
    ) {
        int questionMark = requestPath.indexOf('?');

        String path = questionMark == -1
                ? requestPath
                : requestPath.substring(0, questionMark);

        Map<String, String> queryParams = questionMark == -1
                ? Map.of()
                : extractQueryParameters(requestPath.substring(questionMark + 1));

        String[] requestRoute = path.split("/");
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

        return new RouteMatch(parsedRequestRoute, parsedPathVariables, queryParams, bestRoute);
    }

    /**
     * Parses the raw query portion of a request target into decoded key-value pairs.
     * The supplied string must not include the leading {@code ?}.
     *
     * <p>This parser uses HTML form-style query semantics: keys and values are
     * UTF-8 percent-decoded, and {@code +} is decoded as a space. Components are
     * separated before decoding, so encoded delimiters such as {@code %26} and
     * {@code %3D} remain part of a key or value. Only the first literal {@code =}
     * separates a key from its value.</p>
     *
     * <p>A parameter without {@code =}, such as {@code flag}, and one with an
     * explicitly empty value, such as {@code flag=}, both map to an empty string.
     * An empty query produces an empty map. Empty keys, empty parameter entries,
     * duplicate decoded keys, invalid percent encoding, queries longer than
     * {@value #MAX_QUERY_LENGTH} characters, and queries containing more than
     * {@value #MAX_QUERY_PARAMETERS} parameters are rejected.</p>
     *
     * @param rawQuery raw query text following the first {@code ?}
     * @return decoded parameters, with at most one value per key
     * @throws MalformedHttpRequestException if the query violates the parsing
     *                                      rules described above
     */
    private Map<String, String> extractQueryParameters(String rawQuery) {
        Map<String, String> queryParams = new HashMap<>();

        if (rawQuery.isEmpty()) {
            return queryParams;
        }

        if (rawQuery.length() > MAX_QUERY_LENGTH) {
            throw new MalformedHttpRequestException("Query string is too long");
        }

        String[] pairs = rawQuery.split("&", -1);

        if (pairs.length > MAX_QUERY_PARAMETERS) {
            throw new MalformedHttpRequestException("Too many query parameters");
        }

        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);


            String rawKey = keyValue[0];
            String rawValue = keyValue.length == 2 ? keyValue[1] : "";

            if (rawKey.isEmpty()) {
                throw new MalformedHttpRequestException(
                        "Query parameter key cannot be empty"
                );
            }

            String key = decodeQueryComponent(rawKey);
            String value = decodeQueryComponent(rawValue);

            if (key.isEmpty()) {
                throw new MalformedHttpRequestException(
                        "Query parameter key cannot be empty"
                );
            }

            if (queryParams.containsKey(key)) {
                throw new MalformedHttpRequestException(
                        "Duplicate query parameter: " + key
                );
            }

            queryParams.put(key, value);
        }
        return queryParams;
    }

    private String decodeQueryComponent(String component) {
        try {
            return URLDecoder.decode(component, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new MalformedHttpRequestException(
                    "Query parameter contains invalid percent encoding"
            );
        }
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

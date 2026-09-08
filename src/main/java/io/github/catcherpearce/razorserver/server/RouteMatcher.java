package io.github.catcherpearce.razorserver.server;

import io.github.catcherpearce.razorserver.exception.MalformedHttpRequestException;
import io.github.catcherpearce.razorserver.exception.RouteNotFoundException;
import io.github.catcherpearce.razorserver.http.HttpMethod;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Matches one request against a shared route tree and collects its path variables
 * and decoded query parameters.
 *
 * <p>Create a new instance for each request and call {@link #match()} once.
 * Captured path variables are mutable state retained by this instance.</p>
 */
class RouteMatcher {
    private static final int MAX_QUERY_LENGTH = 8_192;
    private static final int MAX_QUERY_PARAMETERS = 100;

    private final RouteNode root;
    private final HttpMethod requestMethod;
    private final String path;
    private final Map<String, String> queryParams;
    private final String[] requestRoute;
    private final List<String> pathVariables = new ArrayList<>();

    /**
     * Prepares the path segments and query parameters for one request.
     * The first literal {@code ?} separates the path from the query, which is
     * parsed by {@link #extractQueryParameters(String)}. Query parameters do not
     * participate in route selection.
     *
     * <p>The leading slash is removed before splitting the path. The root path
     * {@code /} has no segments and matches directly against the root node.
     * Other paths retain empty segments, including those after a trailing slash.
     * Path segments are not URL-decoded.</p>
     *
     * @param root          shared tree of registered routes
     * @param requestPath   request target beginning with {@code /}, optionally
     *                      followed by a query string
     * @param requestMethod HTTP method required on the matched node
     * @throws MalformedHttpRequestException if the query violates the query
     *                                       parsing rules
     */
    RouteMatcher(
            RouteNode root,
            String requestPath,
            HttpMethod requestMethod
    ) {
        int questionMark = requestPath.indexOf('?');

        this.root = root;
        this.requestMethod = requestMethod;

        this.path = questionMark == -1
                ? requestPath
                : requestPath.substring(0, questionMark);

        this.queryParams = questionMark == -1
                ? Map.of()
                : extractQueryParameters(requestPath.substring(questionMark + 1));

        this.requestRoute = path.equals("/")
                ? new String[0]
                : path.substring(1).split("/", -1);
    }

    /**
     * Finds a complete path match supporting this request's HTTP method.
     * Literal branches are searched before variable branches at each segment.
     * A variable branch is tried when the literal branch has no complete match
     * for the requested method.
     *
     * @return the path without its query string, captured variable values in
     * path order, decoded query parameters, and selected route
     * @throws RouteNotFoundException if no complete match supports the requested
     *                                method, including when the path exists only
     *                                for other methods
     */
    RouteMatch match() {
        RouteNode matchedRoute = backtrack(root, 0);

        if (matchedRoute == null) {
            throw new RouteNotFoundException(path);
        }

        return new RouteMatch(
                path,
                pathVariables,
                queryParams,
                matchedRoute.getRoute(requestMethod)
        );
    }

    /**
     * Searches from a node using the next unconsumed request segment.
     * A node is accepted only after all segments have been consumed, and it has
     * a route for the requested method.
     *
     * <p>Variable values are appended before exploring a variable branch and
     * removed if that branch fails. A successful search retains its captured
     * values; a failed search restores the list to its state on entry.</p>
     *
     * @param currNode node to search, or {@code null} for a missing branch
     * @param i        index of the next segment, from zero through the segment count
     * @return the first matching node in literal-first order, or {@code null}
     */
    private RouteNode backtrack(RouteNode currNode, int i) {
        if (currNode == null) {
            return null;
        }

        RouteNode bestPath = null;

        if (i == requestRoute.length) {
            return currNode.getRoute(requestMethod) != null ? currNode : null;
        }

        RouteNode literalChild = currNode.getLiteralChild(requestRoute[i]);
        RouteNode variableChild = currNode.getVariableChild();

        if (literalChild != null) {
            bestPath = backtrack(literalChild, i + 1);
        }

        if (bestPath == null && variableChild != null) {
            pathVariables.add(requestRoute[i]);
            bestPath = backtrack(variableChild, i + 1);

            if (bestPath == null) {
                pathVariables.removeLast();
            }
        }

        return bestPath;
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
     *                                       rules described above
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

    /**
     * Decodes one query key or value using UTF-8 form decoding, including
     * converting {@code +} to a space.
     *
     * @param component encoded query key or value
     * @return decoded component
     * @throws MalformedHttpRequestException if a percent escape is invalid
     */
    private String decodeQueryComponent(String component) {
        try {
            return URLDecoder.decode(component, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new MalformedHttpRequestException(
                    "Query parameter contains invalid percent encoding"
            );
        }
    }
}

package com.catcher.miniserver.server;

import com.catcher.miniserver.exception.MalformedHttpRequestException;
import com.catcher.miniserver.exception.RouteNotFoundException;
import com.catcher.miniserver.http.HttpMethod;
import com.catcher.miniserver.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RouteMatcherTest {

    @Test
    void prefersLiteralSegmentsFromLeftToRight() {
        Route variableRoute = route();
        Route literalRoute = route();
        Map<String, Map<HttpMethod, Route>> routes = Map.of(
                "/users/{}/details", Map.of(HttpMethod.GET, variableRoute),
                "/users/accounts/{}", Map.of(HttpMethod.GET, literalRoute)
        );

        RouteMatch match = match(routes, "/users/accounts/details", HttpMethod.GET);

        assertEquals("/users/accounts/details", match.route());
        assertEquals(List.of("details"), match.pathVariables());
        assertSame(literalRoute, match.matchedRoute());
    }

    @Test
    void considersMethodBeforeChoosingAnOverlappingPath() {
        Route getVariableRoute = route();
        Route postLiteralRoute = route();
        Map<String, Map<HttpMethod, Route>> routes = Map.of(
                "/users/{}", Map.of(HttpMethod.GET, getVariableRoute),
                "/users/me", Map.of(HttpMethod.POST, postLiteralRoute)
        );

        RouteMatch match = match(routes, "/users/me", HttpMethod.POST);

        assertEquals("/users/me", match.route());
        assertSame(postLiteralRoute, match.matchedRoute());
    }

    @Test
    void throwsWhenNoPathMatches() {
        Map<String, Map<HttpMethod, Route>> routes = Map.of(
                "/users/{}", Map.of(HttpMethod.GET, route())
        );

        assertThrows(
                RouteNotFoundException.class,
                () -> match(routes, "/accounts/42", HttpMethod.GET)
        );
    }

    @Test
    void returnsAnEmptyQueryMapWhenTheRequestHasNoQuery() {
        RouteMatch match = match("/search");

        assertEquals(Map.of(), match.queryParameters());
    }

    @Test
    void returnsAnEmptyQueryMapForAnEmptyQuery() {
        RouteMatch match = match("/search?");

        assertEquals(Map.of(), match.queryParameters());
    }

    @Test
    void extractsMultipleQueryParameters() {
        RouteMatch match = match("/search?q=java&page=2");

        assertEquals(
                Map.of("q", "java", "page", "2"),
                match.queryParameters()
        );
    }

    @Test
    void treatsMissingAndExplicitlyEmptyValuesAsEmptyStrings() {
        RouteMatch match = match("/search?missing&empty=");

        assertEquals(
                Map.of("missing", "", "empty", ""),
                match.queryParameters()
        );
    }

    @Test
    void keepsAdditionalEqualsCharactersInTheValue() {
        RouteMatch match = match("/search?token=a=b=c");

        assertEquals(Map.of("token", "a=b=c"), match.queryParameters());
    }

    @Test
    void decodesUtf8FormEncodedKeysAndValuesAfterSplittingPairs() {
        RouteMatch match = match(
                "/search?first+name=Jos%C3%A9+Silva&expression=a%26b%3Dc"
        );

        assertEquals(
                Map.of(
                        "first name", "José Silva",
                        "expression", "a&b=c"
                ),
                match.queryParameters()
        );
    }

    @Test
    void permitsQuestionMarksInsideTheQueryValue() {
        RouteMatch match = match("/search?q=what?ever");

        assertEquals(Map.of("q", "what?ever"), match.queryParameters());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/search?=value",
            "/search?&a=1",
            "/search?a=1&",
            "/search?a=1&&b=2",
            "/search?a=1&a=2",
            "/search?%61=1&a=2",
            "/search?a=%",
            "/search?a=%2",
            "/search?a=%GG"
    })
    void rejectsMalformedQueries(String requestPath) {
        assertThrows(
                MalformedHttpRequestException.class,
                () -> match(requestPath)
        );
    }

    @Test
    void acceptsTheMaximumNumberOfQueryParameters() {
        String query = createQueryWithParameterCount(100);

        RouteMatch match = match("/search?" + query);

        assertEquals(100, match.queryParameters().size());
    }

    @Test
    void rejectsMoreThanTheMaximumNumberOfQueryParameters() {
        String query = createQueryWithParameterCount(101);

        assertThrows(
                MalformedHttpRequestException.class,
                () -> match("/search?" + query)
        );
    }

    @Test
    void acceptsAQueryAtTheMaximumLength() {
        String query = "key=" + "a".repeat(8_188);

        RouteMatch match = match("/search?" + query);

        assertEquals(8_188, match.queryParameters().get("key").length());
    }

    @Test
    void rejectsAQueryOverTheMaximumLength() {
        String query = "key=" + "a".repeat(8_189);

        assertThrows(
                MalformedHttpRequestException.class,
                () -> match("/search?" + query)
        );
    }

    @Test
    void excludesTheQueryFromRouteMatchingAndPathVariables() {
        Route matchedRoute = route();
        Map<String, Map<HttpMethod, Route>> routes = Map.of(
                "/users/{}", Map.of(HttpMethod.GET, matchedRoute)
        );

        RouteMatch match = match(
                routes,
                "/users/42?expanded=true",
                HttpMethod.GET
        );

        assertEquals("/users/42", match.route());
        assertEquals(List.of("42"), match.pathVariables());
        assertEquals(Map.of("expanded", "true"), match.queryParameters());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/?q=root"})
    void matchesRoutesRegisteredAtTheRoot(String target) {
        Route expected = route();
        RouteMatch result = match(Map.of("/", Map.of(HttpMethod.GET, expected)), target, HttpMethod.GET);
        assertSame(expected, result.matchedRoute());
        assertEquals("/", result.route());
        assertEquals(List.of(), result.pathVariables());
        assertEquals(target.contains("?") ? Map.of("q", "root") : Map.of(), result.queryParameters());
    }

    @Test
    void matchesLiteralRouteWithLeadingSlash() {
        Route expected = route();
        RouteMatch result = match(Map.of("/users", Map.of(HttpMethod.POST, expected)), "/users", HttpMethod.POST);
        assertSame(expected, result.matchedRoute());
        assertEquals(List.of(), result.pathVariables());
    }

    @Test
    void rejectsAnEmptyTree() {
        assertThrows(RouteNotFoundException.class, () -> new RouteMatcher(new RouteNode(), "/", HttpMethod.GET).match());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/users", "/users/42/details/extra", "/Users/42/details", "/users/42/details/"})
    void requiresACompleteCaseSensitivePath(String target) {
        var routes = Map.of("/users/{}/details", Map.of(HttpMethod.GET, route()));
        assertThrows(RouteNotFoundException.class, () -> match(routes, target, HttpMethod.GET));
    }

    @Test
    void rootHandlerDoesNotMatchDescendantPaths() {
        var routes = Map.of("/", Map.of(HttpMethod.GET, route()));
        assertThrows(RouteNotFoundException.class, () -> match(routes, "/users", HttpMethod.GET));
    }

    @Test
    void capturesMultipleVariablesInPathOrder() {
        Route expected = route();
        var routes = Map.of("/{}/users/{}/{}", Map.of(HttpMethod.GET, expected));
        RouteMatch result = match(routes, "/acme/users/42/settings", HttpMethod.GET);
        assertSame(expected, result.matchedRoute());
        assertEquals(List.of("acme", "42", "settings"), result.pathVariables());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/users/", "/users//details"})
    void preservesEmptySegmentsForVariableMatching(String target) {
        String pattern = target.endsWith("details") ? "/users/{}/details" : "/users/{}";
        RouteMatch result = match(Map.of(pattern, Map.of(HttpMethod.GET, route())), target, HttpMethod.GET);
        assertEquals(List.of(""), result.pathVariables());
    }

    @Test
    void prefersEarlierLiteralEvenWhenAnotherRouteHasMoreLiterals() {
        Route expected = route();
        var routes = Map.of(
                "/a/{}/{}/{}", Map.of(HttpMethod.GET, expected),
                "/{}/b/c/d", Map.of(HttpMethod.GET, route()));
        RouteMatch result = match(routes, "/a/b/c/d", HttpMethod.GET);
        assertSame(expected, result.matchedRoute());
        assertEquals(List.of("b", "c", "d"), result.pathVariables());
    }

    @Test
    void fallsBackWhenLiteralBranchCannotConsumeTheRemainingPath() {
        Route expected = route();
        var routes = Map.of(
                "/users/me/settings", Map.of(HttpMethod.GET, route()),
                "/users/{}/details", Map.of(HttpMethod.GET, expected));
        RouteMatch result = match(routes, "/users/me/details", HttpMethod.GET);
        assertSame(expected, result.matchedRoute());
        assertEquals(List.of("me"), result.pathVariables());
    }

    @Test
    void fallsBackWhenLiteralEndpointOnlySupportsAnotherMethod() {
        Route expected = route();
        var routes = Map.of(
                "/users/me", Map.of(HttpMethod.POST, route()),
                "/users/{}", Map.of(HttpMethod.GET, expected));
        RouteMatch result = match(routes, "/users/me", HttpMethod.GET);
        assertSame(expected, result.matchedRoute());
        assertEquals(List.of("me"), result.pathVariables());
    }

    @Test
    void fallsBackWhenLiteralEndpointIsOnlyAnIntermediateNode() {
        Route expected = route();
        var routes = Map.of(
                "/users/me/settings", Map.of(HttpMethod.GET, route()),
                "/users/{}", Map.of(HttpMethod.GET, expected));
        assertSame(expected, match(routes, "/users/me", HttpMethod.GET).matchedRoute());
    }

    @Test
    void removesSeveralFailedCapturesBeforeTryingAnEarlierVariableBranch() {
        Route expected = route();
        var routes = Map.of(
                "/a/{}/{}/missing", Map.of(HttpMethod.GET, route()),
                "/{}/b/{}/done", Map.of(HttpMethod.GET, expected));
        RouteMatch result = match(routes, "/a/b/c/done", HttpMethod.GET);
        assertSame(expected, result.matchedRoute());
        assertEquals(List.of("a", "c"), result.pathVariables());
    }

    @Test
    void preservesAncestorCaptureWhileRemovingFailedDescendantCaptures() {
        Route expected = route();
        var routes = Map.of(
                "/{}/a/{}/missing", Map.of(HttpMethod.GET, route()),
                "/{}/{}/b/done", Map.of(HttpMethod.GET, expected));
        RouteMatch result = match(routes, "/tenant/a/b/done", HttpMethod.GET);
        assertSame(expected, result.matchedRoute());
        assertEquals(List.of("tenant", "a"), result.pathVariables());
    }

    @Test
    void removesCapturesWhenFailureOccursAtMethodSelection() {
        Route expected = route();
        var routes = Map.of(
                "/a/{}", Map.of(HttpMethod.POST, route()),
                "/{}/b", Map.of(HttpMethod.GET, expected));
        RouteMatch result = match(routes, "/a/b", HttpMethod.GET);
        assertSame(expected, result.matchedRoute());
        assertEquals(List.of("a"), result.pathVariables());
    }

    @Test
    void selectsRequestedMethodOnTheSameNode() {
        Route get = route();
        Route post = route();
        var routes = Map.of("/users", Map.of(HttpMethod.GET, get, HttpMethod.POST, post));
        assertSame(get, match(routes, "/users", HttpMethod.GET).matchedRoute());
        assertSame(post, match(routes, "/users", HttpMethod.POST).matchedRoute());
    }

    @Test
    void unsupportedMethodCurrentlyProducesRouteNotFound() {
        var routes = Map.of("/users", Map.of(HttpMethod.POST, route()));
        assertThrows(RouteNotFoundException.class, () -> match(routes, "/users", HttpMethod.GET));
    }

    @Test
    void separateMatchersSharingATreeKeepIndependentResults() {
        RouteNode root = tree(Map.of("/users/{}", Map.of(HttpMethod.GET, route())));
        RouteMatch first = new RouteMatcher(root, "/users/42?q=first", HttpMethod.GET).match();
        RouteMatch second = new RouteMatcher(root, "/users/99?q=second", HttpMethod.GET).match();
        assertEquals(List.of("42"), first.pathVariables());
        assertEquals(Map.of("q", "first"), first.queryParameters());
        assertEquals(List.of("99"), second.pathVariables());
        assertEquals(Map.of("q", "second"), second.queryParameters());
    }

    @Test
    void concurrentMatchersSharingATreeKeepIndependentCaptures() throws Exception {
        Route expected = route();
        RouteNode root = tree(Map.of("/users/{}", Map.of(HttpMethod.GET, expected)));
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = new java.util.ArrayList<java.util.concurrent.Callable<RouteMatch>>();
            for (int i = 0; i < 100; i++) {
                String target = "/users/" + i + "?id=" + i;
                tasks.add(() -> new RouteMatcher(root, target, HttpMethod.GET).match());
            }
            var results = executor.invokeAll(tasks);
            for (int i = 0; i < results.size(); i++) {
                RouteMatch result = results.get(i).get();
                assertSame(expected, result.matchedRoute());
                assertEquals(List.of(String.valueOf(i)), result.pathVariables());
                assertEquals(Map.of("id", String.valueOf(i)), result.queryParameters());
            }
        }
    }

    @Test
    void validatesQueryDuringConstructionEvenWhenPathDoesNotExist() {
        assertThrows(MalformedHttpRequestException.class,
                () -> new RouteMatcher(new RouteNode(), "/missing?q=%", HttpMethod.GET));
    }

    private RouteMatch match(Map<String, Map<HttpMethod, Route>> routes, String target, HttpMethod method) {
        return new RouteMatcher(tree(routes), target, method).match();
    }

    private RouteNode tree(Map<String, Map<HttpMethod, Route>> routes) {
        RouteNode root = new RouteNode();
        routes.forEach((pattern, methods) -> {
            RouteNode node = root;
            if (!pattern.equals("/")) {
                for (String segment : pattern.substring(1).split("/", -1)) {
                    node = segment.equals("{}")
                            ? node.getOrCreateVariableChild()
                            : node.getOrCreateLiteralChild(segment);
                }
            }
            RouteNode endpoint = node;
            methods.forEach((method, route) -> endpoint.addRoute(method, route, pattern));
        });
        return root;
    }

    private RouteMatch match(String requestPath) {
        Map<String, Map<HttpMethod, Route>> routes = Map.of(
                "/search", Map.of(HttpMethod.GET, route())
        );

        return match(routes, requestPath, HttpMethod.GET);
    }

    private String createQueryWithParameterCount(int count) {
        StringBuilder query = new StringBuilder();

        for (int i = 0; i < count; i++) {
            if (i > 0) {
                query.append('&');
            }
            query.append('p').append(i).append('=').append(i);
        }

        return query.toString();
    }

    private Route route() {
        return new Route(request -> new HttpResponse(200), null, List.of());
    }
}

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
    private final RouteMatcher routeMatcher = new RouteMatcher();

    @Test
    void prefersLiteralSegmentsFromLeftToRight() {
        Route variableRoute = route();
        Route literalRoute = route();
        Map<String, Map<HttpMethod, Route>> routes = Map.of(
                "/users/{}/details", Map.of(HttpMethod.GET, variableRoute),
                "/users/accounts/{}", Map.of(HttpMethod.GET, literalRoute)
        );

        RouteMatch match = routeMatcher.match(routes, "/users/accounts/details", HttpMethod.GET);

        assertEquals("/users/accounts/{}", match.route());
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

        RouteMatch match = routeMatcher.match(routes, "/users/me", HttpMethod.POST);

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
                () -> routeMatcher.match(routes, "/accounts/42", HttpMethod.GET)
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

        RouteMatch match = routeMatcher.match(
                routes,
                "/users/42?expanded=true",
                HttpMethod.GET
        );

        assertEquals("/users/{}", match.route());
        assertEquals(List.of("42"), match.pathVariables());
        assertEquals(Map.of("expanded", "true"), match.queryParameters());
    }

    private RouteMatch match(String requestPath) {
        Map<String, Map<HttpMethod, Route>> routes = Map.of(
                "/search", Map.of(HttpMethod.GET, route())
        );

        return routeMatcher.match(routes, requestPath, HttpMethod.GET);
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

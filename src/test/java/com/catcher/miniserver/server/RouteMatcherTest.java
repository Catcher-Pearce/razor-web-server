package com.catcher.miniserver.server;

import com.catcher.miniserver.exception.RouteNotFoundException;
import com.catcher.miniserver.http.HttpMethod;
import com.catcher.miniserver.http.HttpResponse;
import org.junit.jupiter.api.Test;

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

    private Route route() {
        return new Route(request -> new HttpResponse(200), null, List.of());
    }
}

package io.github.catcherpearce.razorserver.server;

import io.github.catcherpearce.razorserver.exception.InvalidRoutePatternException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RoutePatternTest {

    @Test
    public void normalizesSinglePathVariable() {
        RoutePattern pattern = new RoutePattern("/users/{id}");

        ParsedRoute result = pattern.normalizeRoute();

        assertEquals(List.of("users", "{}"), result.path());
        assertEquals(List.of("id"), result.pathVariables());
    }

    @Test
    public void normalizesDoublePathVariables() {
        RoutePattern pattern = new RoutePattern("/users/{id}/accounts/{value}");

        ParsedRoute result = pattern.normalizeRoute();

        assertEquals(List.of("users", "{}", "accounts", "{}"), result.path());
        assertEquals(List.of("id", "value"), result.pathVariables());
    }

    @Test
    public void rejectsDuplicatePathVariableNames() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users/{id}/accounts/{id}").normalizeRoute()
        );
    }

    @Test
    public void rejectsUnclosedPathVariable() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users/{id").normalizeRoute()
        );
    }

    @Test
    public void rejectsEmptyPathVariableNames() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users/{}").normalizeRoute()
        );
    }

    @Test
    public void rejectsMissingOpeningBracket() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users/id}").normalizeRoute()
        );
    }

    @Test
    public void rejectsEmptyRoute() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("").normalizeRoute()
        );
    }

    @Test
    public void rejectsNestedRoute() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users/{{id}}").normalizeRoute()
        );
    }

    @Test
    public void rejectsPathVariableMixedWithLiteralText() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users/id{id}").normalizeRoute()
        );
    }

    @Test
    public void rejectsRouteStartingWithPathVariables() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("{id}/users").normalizeRoute()
        );
    }

    @Test
    public void rejectsSlashInsidePathVariable() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users/{user/id}").normalizeRoute()
        );
    }

    @Test
    public void rejectsRouteWithoutLeadingSlash() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("users/{id}").normalizeRoute()
        );
    }

    @Test
    public void rejectsEmptyPathSegment() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users//accounts").normalizeRoute()
        );
    }

    @Test
    public void rejectsTrailingSlash() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users/").normalizeRoute()
        );
    }

    @Test
    public void normalizesRootRoute() {
        ParsedRoute result = new RoutePattern("/").normalizeRoute();

        assertEquals(List.of(), result.path());
        assertEquals(List.of(), result.pathVariables());
    }
}

package com.catcher.miniserver.server;

import com.catcher.miniserver.exception.InvalidRoutePatternException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RoutePatternTest {

    @Test
    public void serializesSinglePathVariable() {
        RoutePattern pattern = new RoutePattern("/users/{id}");

        ParsedRoute result = pattern.serializeRoute();

        assertEquals("/users/{}", result.path());
        assertEquals(List.of("id"), result.pathVariables());
    }

    @Test
    public void serializesDoublePathVariables() {
        RoutePattern pattern = new RoutePattern("/users/{id}/accounts/{value}");

        ParsedRoute result = pattern.serializeRoute();

        assertEquals("/users/{}/accounts/{}", result.path());
        assertEquals(List.of("id", "value"), result.pathVariables());
    }

    @Test
    public void rejectsDuplicatePathVariableNames() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users/{id}/accounts/{id}").serializeRoute()
        );
    }

    @Test
    public void rejectsUnclosedPathVariable() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users/{id").serializeRoute()
        );
    }

    @Test
    public void rejectsEmptyPathVariableNames() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users/{}").serializeRoute()
        );
    }

    @Test
    public void rejectsMissingOpeningBracket() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users/id}").serializeRoute()
        );
    }

    @Test
    public void rejectsEmptyRoute() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("").serializeRoute()
        );
    }

    @Test
    public void rejectsNestedRoute() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users/{{id}}").serializeRoute()
        );
    }

    @Test
    public void rejectsPathVariableMixedWithLiteralText() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users/id{id}").serializeRoute()
        );
    }

    @Test
    public void rejectsRouteStartingWithPathVariables() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("{id}/users").serializeRoute()
        );
    }

    @Test
    public void rejectsSlashInsidePathVariable() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users/{user/id}").serializeRoute()
        );
    }

    @Test
    public void rejectsRouteWithoutLeadingSlash() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("users/{id}").serializeRoute()
        );
    }

    @Test
    public void rejectsEmptyPathSegment() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users//accounts").serializeRoute()
        );
    }

    @Test
    public void rejectsTrailingSlash() {
        assertThrows(
                InvalidRoutePatternException.class,
                () -> new RoutePattern("/users/").serializeRoute()
        );
    }

    @Test
    public void serializesRootRoute() {
        ParsedRoute result = new RoutePattern("/").serializeRoute();

        assertEquals("/", result.path());
        assertEquals(List.of(), result.pathVariables());
    }
}

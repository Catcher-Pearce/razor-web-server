package io.github.catcherpearce.razorserver.server;

import io.github.catcherpearce.razorserver.exception.DuplicateRouteException;
import io.github.catcherpearce.razorserver.exception.InvalidRoutePatternException;
import io.github.catcherpearce.razorserver.exception.MalformedHttpRequestException;
import io.github.catcherpearce.razorserver.exception.RequestBodyDeserializationException;
import io.github.catcherpearce.razorserver.exception.RequestValidationException;
import io.github.catcherpearce.razorserver.exception.RouteNotFoundException;
import io.github.catcherpearce.razorserver.exception.UnsupportedMediaTypeException;
import io.github.catcherpearce.razorserver.http.HttpMethod;
import io.github.catcherpearce.razorserver.http.HttpRequest;
import io.github.catcherpearce.razorserver.http.HttpResponse;
import io.github.catcherpearce.razorserver.http.Response;
import io.github.catcherpearce.razorserver.validation.RequestShape;
import io.github.catcherpearce.razorserver.validation.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RequestDispatcherTest {
    private final RequestDispatcher dispatcher = new RequestDispatcher();

    @TempDir
    Path staticRoot;

    @Test
    void dispatchesRootRouteAndReturnsExactHandlerResponse() {
        HttpResponse expected = Response.text("root");
        AtomicInteger calls = new AtomicInteger();
        dispatcher.createRoute("/", HttpMethod.GET, request -> {
            calls.incrementAndGet();
            return expected;
        }, null);

        assertSame(expected, dispatcher.handleRequest(request(HttpMethod.GET, "/")));
        assertEquals(1, calls.get());
    }

    @Test
    void passesCapturedVariablesDecodedQueryHeadersAndRawBodyToHandler() {
        AtomicReference<ServerRequest> captured = new AtomicReference<>();
        dispatcher.createRoute("/teams/{team}/users/{user}", HttpMethod.POST, request -> {
            captured.set(request);
            return Response.noContent();
        }, null);
        Map<String, String> headers = Map.of("content-type", "text/plain");

        dispatcher.handleRequest(new HttpRequest(HttpMethod.POST,
                "/teams/red/users/42?search=Ada+Lovelace&symbol=%26", "HTTP/1.1", headers, "hello"));

        ServerRequest mapped = captured.get();
        assertNotNull(mapped);
        assertEquals(HttpMethod.POST, mapped.method());
        assertEquals(Map.of("team", "red", "user", "42"), mapped.pathVariables());
        assertEquals(Map.of("search", "Ada Lovelace", "symbol", "&"), mapped.queryParams());
        assertEquals(headers, mapped.headers());
        assertEquals("hello", mapped.body());
    }

    @Test
    void supportsDifferentMethodsAndVariableNamesOnSamePattern() {
        dispatcher.createRoute("/users/{id}", HttpMethod.GET,
                request -> Response.text(request.pathVariables().get("id")), null);
        dispatcher.createRoute("/users/{name}", HttpMethod.POST,
                request -> Response.text(request.pathVariables().get("name")), null);

        assertEquals("42", dispatcher.handleRequest(request(HttpMethod.GET, "/users/42")).body());
        assertEquals("Ada", dispatcher.handleRequest(request(HttpMethod.POST, "/users/Ada")).body());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/users/{id}", "/users/{other}"})
    void rejectsDuplicateNormalizedRouteWithoutReplacingOriginal(String duplicate) {
        HttpResponse original = Response.text("original");
        dispatcher.createRoute("/users/{id}", HttpMethod.GET, request -> original, null);

        assertThrows(DuplicateRouteException.class,
                () -> dispatcher.createRoute(duplicate, HttpMethod.GET, request -> Response.text("replacement"), null));
        assertSame(original, dispatcher.handleRequest(request(HttpMethod.GET, "/users/42")));
    }

    @Test
    void rejectsInvalidRoutePattern() {
        assertThrows(InvalidRoutePatternException.class,
                () -> dispatcher.createRoute("/users/{id}/{id}", HttpMethod.GET, request -> Response.noContent(), null));
        assertThrows(RouteNotFoundException.class,
                () -> dispatcher.handleRequest(request(HttpMethod.GET, "/users/1/2")));
    }

    @Test
    void prefersLiteralRouteAndFallsBackToVariableRouteWhenMethodDiffers() {
        dispatcher.createRoute("/users/{id}", HttpMethod.GET,
                request -> Response.text(request.pathVariables().get("id")), null);
        dispatcher.createRoute("/users/me", HttpMethod.GET, request -> Response.text("literal"), null);
        dispatcher.createRoute("/users/new", HttpMethod.POST, request -> Response.noContent(), null);

        assertEquals("literal", dispatcher.handleRequest(request(HttpMethod.GET, "/users/me")).body());
        assertEquals("new", dispatcher.handleRequest(request(HttpMethod.GET, "/users/new")).body());
    }

    @Test
    void keepsVariablesAndQueriesIndependentBetweenRequests() {
        dispatcher.createRoute("/users/{id}", HttpMethod.GET, Response::ok, null);
        ServerRequest first = (ServerRequest) dispatcher.handleRequest(request(HttpMethod.GET, "/users/1?q=first")).body();
        ServerRequest second = (ServerRequest) dispatcher.handleRequest(request(HttpMethod.GET, "/users/2")).body();

        assertEquals(Map.of("id", "1"), first.pathVariables());
        assertEquals(Map.of("q", "first"), first.queryParams());
        assertEquals(Map.of("id", "2"), second.pathVariables());
        assertEquals(Map.of(), second.queryParams());
    }

    @Test
    void throwsForMissingRouteAndWrongMethod() {
        dispatcher.createRoute("/users", HttpMethod.GET, request -> Response.noContent(), null);
        assertThrows(RouteNotFoundException.class,
                () -> dispatcher.handleRequest(request(HttpMethod.GET, "/missing")));
        assertThrows(RouteNotFoundException.class,
                () -> dispatcher.handleRequest(request(HttpMethod.POST, "/users")));
    }

    @Test
    void deliversValidatedTypedBodyToHandler() {
        dispatcher.createRoute("/users", HttpMethod.POST,
                request -> Response.text(request.bodyAs(NameRequest.class).name), NameRequest.class);
        HttpResponse response = dispatcher.handleRequest(new HttpRequest(HttpMethod.POST, "/users", "HTTP/1.1",
                Map.of("content-type", "application/json"), "{\"name\":\"Ada\"}"));
        assertEquals("Ada", response.body());
    }

    @Test
    void mappingFailuresPreventHandlerInvocationEvenWithStaticFallback() {
        dispatcher.serveStaticFiles(staticRoot);
        dispatcher.createRoute("/users", HttpMethod.GET, request -> fail("Invalid requests must not reach handler"), NameRequest.class);

        assertThrows(RequestBodyDeserializationException.class, () -> dispatcher.handleRequest(
                new HttpRequest(HttpMethod.GET, "/users", "HTTP/1.1", Map.of("content-type", "application/json"), "{bad")));
        assertThrows(RequestValidationException.class, () -> dispatcher.handleRequest(
                new HttpRequest(HttpMethod.GET, "/users", "HTTP/1.1", Map.of("content-type", "application/json"), "{}")));
        assertThrows(UnsupportedMediaTypeException.class,
                () -> dispatcher.handleRequest(request(HttpMethod.GET, "/users")));
    }

    @Test
    void propagatesHandlerFailureWithStaticFallbackConfigured() {
        dispatcher.serveStaticFiles(staticRoot);
        IllegalStateException failure = new IllegalStateException("handler failed");
        dispatcher.createRoute("/users", HttpMethod.GET, request -> { throw failure; }, null);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> dispatcher.handleRequest(request(HttpMethod.GET, "/users"))));
    }

    @Test
    void servesStaticFileForUnmatchedGet() throws Exception {
        byte[] contents = {1, 2, 3};
        Files.write(staticRoot.resolve("asset.bin"), contents);
        dispatcher.serveStaticFiles(staticRoot);

        HttpResponse response = dispatcher.handleRequest(request(HttpMethod.GET, "/asset.bin"));
        assertEquals(200, response.status());
        assertArrayEquals(contents, assertInstanceOf(byte[].class, response.body()));
    }

    @Test
    void registeredRouteTakesPrecedenceOverStaticFile() throws Exception {
        Files.writeString(staticRoot.resolve("page.txt"), "static");
        dispatcher.serveStaticFiles(staticRoot);
        HttpResponse expected = Response.text("dynamic");
        dispatcher.createRoute("/page.txt", HttpMethod.GET, request -> expected, null);
        assertSame(expected, dispatcher.handleRequest(request(HttpMethod.GET, "/page.txt")));
    }

    @Test
    void returnsStaticNotFoundForMissingFile() {
        dispatcher.serveStaticFiles(staticRoot);
        assertEquals(404, dispatcher.handleRequest(request(HttpMethod.GET, "/missing.txt")).status());
    }

    @Test
    void doesNotServeStaticFilesForPostOrDirectRegisteredRouteLookup() throws Exception {
        Files.writeString(staticRoot.resolve("page.txt"), "static");
        dispatcher.serveStaticFiles(staticRoot);
        assertThrows(RouteNotFoundException.class,
                () -> dispatcher.handleRequest(request(HttpMethod.POST, "/page.txt")));
        assertThrows(RouteNotFoundException.class,
                () -> dispatcher.handleRegisteredRoute(request(HttpMethod.GET, "/page.txt")));
    }

    @Test
    void malformedQueryDoesNotFallBackToStaticFiles() throws Exception {
        Files.writeString(staticRoot.resolve("page.txt"), "static");
        dispatcher.serveStaticFiles(staticRoot);
        assertThrows(MalformedHttpRequestException.class,
                () -> dispatcher.handleRequest(request(HttpMethod.GET, "/page.txt?q=%ZZ")));
    }

    private HttpRequest request(HttpMethod method, String path) {
        return new HttpRequest(method, path, "HTTP/1.1", Map.of(), "");
    }

    public static class NameRequest implements RequestShape {
        @NotNull
        public String name;
    }
}

package com.catcher.miniserver.http;

import com.catcher.miniserver.exception.MalformedHttpRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.catcher.miniserver.server.RequestDispatcher;

import java.util.Map;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpParserTest {

    @Test
    public void parsesRequestLineHeadersAndBody() {
        HttpRequest request = parse(
                "POST /messages HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: 5\r\n" +
                "\r\n" +
                "hello"
        );

        assertEquals(HttpMethod.POST, request.method());
        assertEquals("/messages", request.path());
        assertEquals("HTTP/1.1", request.version());
        assertEquals("localhost", request.headers().get("host"));
        assertEquals("text/plain", request.headers().get("content-type"));
        assertEquals("5", request.headers().get("content-length"));
        assertEquals("hello", request.body());
    }

    @Test
    public void parsesRequestWithoutHeadersOrBody() {
        HttpRequest request = parse("GET / HTTP/1.1\r\n\r\n");

        assertEquals(HttpMethod.GET, request.method());
        assertEquals("/", request.path());
        assertTrue(request.headers().isEmpty());
        assertEquals("", request.body());
    }

    @Test
    public void supportsEveryDeclaredHttpMethod() {
        for (HttpMethod method : HttpMethod.values()) {
            HttpRequest request = parse(method + " /resource HTTP/1.1\r\n\r\n");
            assertEquals(method, request.method());
        }
    }

    @Test
    public void normalizesHeaderNamesToLowercase() {
        HttpRequest request = parse(
                "GET / HTTP/1.1\r\n" +
                "X-MiXeD-CaSe: value\r\n\r\n"
        );

        assertEquals("value", request.headers().get("x-mixed-case"));
    }

    @Test
    public void trimsWhitespaceAroundHeaderValues() {
        HttpRequest request = parse(
                "GET / HTTP/1.1\r\n" +
                "X-Example:    some value   \r\n\r\n"
        );

        assertEquals("some value", request.headers().get("x-example"));
    }

    @Test
    public void preservesColonsInsideHeaderValues() {
        HttpRequest request = parse(
                "GET / HTTP/1.1\r\n" +
                "Location: http://localhost:8080/resource\r\n\r\n"
        );

        assertEquals("http://localhost:8080/resource", request.headers().get("location"));
    }

    @Test
    public void allowsEmptyHeaderValue() {
        HttpRequest request = parse(
                "GET / HTTP/1.1\r\n" +
                "X-Empty:\r\n\r\n"
        );

        assertEquals("", request.headers().get("x-empty"));
    }

    @Test
    public void acceptsZeroLengthBody() {
        HttpRequest request = parse(
                "POST /messages HTTP/1.1\r\n" +
                "Content-Length: 0\r\n\r\n"
        );

        assertEquals("", request.body());
    }

    @Test
    public void measuresContentLengthInBytesForUtf8Text() {
        String body = "café";
        int byteLength = body.getBytes(StandardCharsets.UTF_8).length;

        HttpRequest request = parse(
                "POST /messages HTTP/1.1\r\n" +
                "Content-Length: " + byteLength + "\r\n\r\n" +
                body
        );

        assertEquals(body, request.body());
    }

    @Test
    public void rejectsStreamEndingBeforeHeadersAreComplete() {
        assertMalformed("GET / HTTP/1.1\r\nHost: localhost\r\n");
    }

    @Test
    public void rejectsRequestLineWithMissingComponent() {
        assertMalformed("GET /\r\n\r\n");
    }

    @Test
    public void rejectsRequestLineWithExtraComponent() {
        assertMalformed("GET / HTTP/1.1 extra\r\n\r\n");
    }

    @Test
    public void rejectsUnknownMethod() {
        assertMalformed("OPTIONS / HTTP/1.1\r\n\r\n");
    }

    @Test
    public void rejectsUnsupportedHttpVersion() {
        assertMalformed("GET / HTTP/1.0\r\n\r\n");
    }

    @Test
    public void rejectsHeaderWithoutColon() {
        assertMalformed("GET / HTTP/1.1\r\nMalformed header\r\n\r\n");
    }

    @Test
    public void rejectsHeaderWithEmptyName() {
        assertMalformed("GET / HTTP/1.1\r\n   : value\r\n\r\n");
    }

    @Test
    public void rejectsEmptyContentLength() {
        assertMalformed("POST / HTTP/1.1\r\nContent-Length:\r\n\r\n");
    }

    @Test
    public void rejectsNonNumericContentLength() {
        assertMalformed("POST / HTTP/1.1\r\nContent-Length: five\r\n\r\n");
    }

    @Test
    public void rejectsSignedContentLength() {
        assertMalformed("POST / HTTP/1.1\r\nContent-Length: +5\r\n\r\nhello");
    }

    @Test
    public void rejectsContentLengthThatOverflowsLong() {
        assertMalformed(
                "POST / HTTP/1.1\r\n" +
                "Content-Length: 999999999999999999999999\r\n\r\n"
        );
    }

    @Test
    public void rejectsBodyLargerThanMaximum() {
        assertMalformed(
                "POST / HTTP/1.1\r\n" +
                "Content-Length: " + (HttpParser.MAX_BODY_BYTES + 1L) + "\r\n\r\n"
        );
    }

    @Test
    public void rejectsBodyShorterThanContentLength() {
        assertMalformed("POST / HTTP/1.1\r\nContent-Length: 5\r\n\r\nhey");
    }

    @Test
    public void rejectsRequestLineLargerThanMaximum() {
        String oversizedPath = "/" + "a".repeat(HttpParser.MAX_REQUEST_LINE_BYTES);
        assertMalformed("GET " + oversizedPath + " HTTP/1.1\r\n\r\n");
    }

    @Test
    public void rejectsHeadersLargerThanMaximum() {
        String oversizedValue = "a".repeat(HttpParser.MAX_HEADER_BYTES);

        assertMalformed(
                "GET / HTTP/1.1\r\n" +
                "X-Large: " + oversizedValue + "\r\n\r\n"
        );
    }

    @Test
    public void rejectsDuplicateHeaders() {
        assertMalformed(
                "GET / HTTP/1.1\r\n" +
                        "content-length: 32\r\ncontentLength: 32\r\n\r\n"
        );
    }

    @ParameterizedTest
    @CsvSource({
            "http://example.com/users/42?name=Ada, /users/42?name=Ada",
            "https://example.com:8443/users, /users",
            "HTTP://example.com/users, /users",
            "http://[::1]:3000/users, /users",
            "http://example.com, /",
            "http://example.com?name=Ada, /?name=Ada",
            "http://example.com?, /?",
            "http://example.com/a/../b, /a/../b",
            "http://example.com/a%2Fb?q=%26%3D+%2520, /a%2Fb?q=%26%3D+%2520"
    })
    void normalizesAbsoluteUrlsWithoutDecoding(String target, String expected) {
        assertEquals(expected, parse("GET " + target + " HTTP/1.1\r\nHost: example.com\r\n\r\n").path());
    }

    @Test
    void usesAbsoluteUrlAuthorityInsteadOfConflictingHostHeader() {
        HttpRequest request = parse("POST http://example.com:8080/messages HTTP/1.1\r\n"
                + "Host: other.example\r\nContent-Length: 5\r\n\r\nhello");
        assertEquals("example.com:8080", request.headers().get("host"));
        assertEquals("/messages", request.path());
        assertEquals(HttpMethod.POST, request.method());
        assertEquals("hello", request.body());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http:///users", "http://", "http:users", "ftp://example.com/users",
            "http://user:password@example.com/users", "http://example.com/users#fragment",
            "http://example.com/%ZZ", "http://example.com/?q=%", "http://[::1/users",
            "http://example.com:abc/users", "users/42"
    })
    void rejectsMalformedOrUnsupportedAbsoluteTargets(String target) {
        assertMalformed("GET " + target + " HTTP/1.1\r\nHost: example.com\r\n\r\n");
    }

    @Test
    void preservesOriginFormTargetAndHost() {
        HttpRequest request = parse("GET /a%2Fb?q=%26+text HTTP/1.1\r\nHost: localhost:3000\r\n\r\n");
        assertEquals("/a%2Fb?q=%26+text", request.path());
        assertEquals("localhost:3000", request.headers().get("host"));
    }

    @Test
    void routesAbsoluteUrlWithPathVariablesAndDecodedQuery() {
        RequestDispatcher dispatcher = new RequestDispatcher();
        dispatcher.createRoute("/users/{id}", HttpMethod.GET, request -> Response.ok(Map.of(
                "id", request.pathVariables().get("id"),
                "name", request.queryParams().get("name")
        )), null);
        HttpRequest request = parse("GET http://example.com/users/42?name=Ada+Lovelace HTTP/1.1\r\n"
                + "Host: example.com\r\n\r\n");
        assertEquals(Map.of("id", "42", "name", "Ada Lovelace"), dispatcher.handleRequest(request).body());
    }

    private HttpRequest parse(String rawRequest) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(
                rawRequest.getBytes(StandardCharsets.UTF_8)
        );

        return new HttpParser(inputStream).parse();
    }

    private void assertMalformed(String rawRequest) {
        MalformedHttpRequestException exception = assertThrows(
                MalformedHttpRequestException.class,
                () -> parse(rawRequest)
        );

        assertEquals(400, exception.statusCode());
    }
}

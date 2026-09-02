package com.catcher.miniserver.http;

import com.catcher.miniserver.exception.MalformedHttpRequestException;
import org.junit.jupiter.api.Test;

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

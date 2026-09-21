package io.github.catcherpearce.razorserver.server;

import io.github.catcherpearce.razorserver.exception.RequestBodyDeserializationException;
import io.github.catcherpearce.razorserver.exception.RequestValidationException;
import io.github.catcherpearce.razorserver.exception.UnsupportedMediaTypeException;
import io.github.catcherpearce.razorserver.http.HttpMethod;
import io.github.catcherpearce.razorserver.http.HttpRequest;
import io.github.catcherpearce.razorserver.http.ProxyHandler;
import io.github.catcherpearce.razorserver.validation.RequestShape;
import io.github.catcherpearce.razorserver.validation.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequestMapperTest {
    private static final String REMOTE_IP = "192.168.1.10";
    private final RequestMapper mapper = new RequestMapper();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"plain text", "{invalid json", "{\"name\":null}"})
    void preservesRawBodyAndMetadataWithoutARequestShape(String body) {
        Map<String, String> headers = Map.of("content-type", "text/plain", "x-request-id", "abc");
        Map<String, String> variables = Map.of("id", "42");
        Map<String, String> query = Map.of("search", "Ada Lovelace");
        HttpRequest request = new HttpRequest(HttpMethod.POST, "/users/42", "HTTP/1.1", headers, body, REMOTE_IP);

        ServerRequest mapped = mapper.map(new Route(ignored -> fail("Mapping must not invoke the handler"),
                null, List.of("id")), request, variables, query);

        assertAll(
                () -> assertEquals(HttpMethod.POST, mapped.method()),
                () -> assertEquals("/users/42", mapped.path()),
                () -> assertEquals(variables, mapped.pathVariables()),
                () -> assertEquals(query, mapped.queryParams()),
                () -> assertEquals(headers, mapped.headers()),
                () -> assertEquals(REMOTE_IP, mapped.remoteId()),
                () -> assertEquals(REMOTE_IP, mapped.clientId()),
                () -> assertEquals(body, mapped.body())
        );
    }

    @Test
    void acceptsRawBodyWithoutContentType() {
        ServerRequest mapped = map(null, Map.of(), "raw body");
        assertEquals("raw body", mapped.body());
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/json; charset=utf-8"})
    void deserializesAndValidatesRegisteredShape(String contentType) {
        ServerRequest mapped = map(NameRequest.class, Map.of("content-type", contentType), "{\"name\":\"Ada\"}");
        assertEquals("Ada", assertInstanceOf(NameRequest.class, mapped.body()).name);
        assertEquals(Map.of("content-type", contentType), mapped.headers());
        assertEquals(HttpMethod.POST, mapped.method());
        assertEquals("/users", mapped.path());
        assertEquals(Map.of(), mapped.pathVariables());
        assertEquals(Map.of(), mapped.queryParams());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{broken", "[]"})
    void propagatesDeserializationFailures(String body) {
        assertThrows(RequestBodyDeserializationException.class,
                () -> map(NameRequest.class, Map.of("content-type", "application/json"), body));
    }

    @Test
    void rejectsDeserializedBodyThatFailsValidation() {
        RequestValidationException exception = assertThrows(RequestValidationException.class,
                () -> map(NameRequest.class, Map.of("content-type", "application/json"), "{}"));
        assertEquals(List.of("name must not be null"), exception.violations());
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/plain", "application/xml"})
    void rejectsUnsupportedContentTypeForRegisteredShape(String contentType) {
        assertThrows(UnsupportedMediaTypeException.class,
                () -> map(NameRequest.class, Map.of("content-type", contentType), "{\"name\":\"Ada\"}"));
    }

    @Test
    void rejectsMissingContentTypeForRegisteredShape() {
        assertThrows(UnsupportedMediaTypeException.class,
                () -> map(NameRequest.class, Map.of(), "{\"name\":\"Ada\"}"));
    }

    @Test
    void ignoresForwardedHeaderWhenProxyHandlingIsNotConfigured() {
        ServerRequest mapped = map(null, Map.of("x-forwarded-for", "203.0.113.10"), "");

        assertEquals(REMOTE_IP, mapped.clientId());
    }

    @Test
    void ignoresForwardedHeaderFromUntrustedPeer() {
        mapper.proxyHandler = new ProxyHandler(List.of("10.0.0.0/8"));

        ServerRequest mapped = map(null, Map.of("x-forwarded-for", "203.0.113.10"), "");

        assertEquals(REMOTE_IP, mapped.clientId());
    }

    @Test
    void usesRemoteIpWhenTrustedPeerHasNoForwardedHeader() {
        mapper.proxyHandler = new ProxyHandler(List.of("192.168.1.0/24"));

        ServerRequest mapped = map(null, Map.of(), "");

        assertEquals(REMOTE_IP, mapped.clientId());
    }

    @Test
    void resolvesForwardedClientAndPreservesRemoteIp() {
        mapper.proxyHandler = new ProxyHandler(List.of("192.168.1.0/24"));
        Map<String, String> headers = Map.of("x-forwarded-for", " 203.0.113.10 , 192.168.1.20 ");

        ServerRequest mapped = map(null, headers, "");

        assertEquals("203.0.113.10", mapped.clientId());
        assertEquals(REMOTE_IP, mapped.remoteId());
        assertEquals(headers, mapped.headers());
    }

    private ServerRequest map(Class<? extends RequestShape> shape, Map<String, String> headers, String body) {
        return mapper.map(new Route(ignored -> fail("Mapping must not invoke the handler"), shape, List.of()),
                new HttpRequest(HttpMethod.POST, "/users", "HTTP/1.1", headers, body, REMOTE_IP), Map.of(), Map.of());
    }

    public static class NameRequest implements RequestShape {
        @NotNull
        public String name;
    }
}

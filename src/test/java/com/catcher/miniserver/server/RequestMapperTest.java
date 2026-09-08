package com.catcher.miniserver.server;

import com.catcher.miniserver.exception.RequestBodyDeserializationException;
import com.catcher.miniserver.exception.RequestValidationException;
import com.catcher.miniserver.exception.UnsupportedMediaTypeException;
import com.catcher.miniserver.http.HttpMethod;
import com.catcher.miniserver.http.HttpRequest;
import com.catcher.miniserver.validation.RequestShape;
import com.catcher.miniserver.validation.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequestMapperTest {
    private final RequestMapper mapper = new RequestMapper();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"plain text", "{invalid json", "{\"name\":null}"})
    void preservesRawBodyAndMetadataWithoutARequestShape(String body) {
        Map<String, String> headers = Map.of("content-type", "text/plain", "x-request-id", "abc");
        Map<String, String> variables = Map.of("id", "42");
        Map<String, String> query = Map.of("search", "Ada Lovelace");
        HttpRequest request = new HttpRequest(HttpMethod.POST, "/users/42", "HTTP/1.1", headers, body);

        ServerRequest mapped = mapper.map(new Route(ignored -> fail("Mapping must not invoke the handler"),
                null, List.of("id")), request, variables, query);

        assertAll(
                () -> assertEquals(HttpMethod.POST, mapped.method()),
                () -> assertEquals("/users/42", mapped.path()),
                () -> assertEquals(variables, mapped.pathVariables()),
                () -> assertEquals(query, mapped.queryParams()),
                () -> assertEquals(headers, mapped.headers()),
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

    private ServerRequest map(Class<? extends RequestShape> shape, Map<String, String> headers, String body) {
        return mapper.map(new Route(ignored -> fail("Mapping must not invoke the handler"), shape, List.of()),
                new HttpRequest(HttpMethod.POST, "/users", "HTTP/1.1", headers, body), Map.of(), Map.of());
    }

    public static class NameRequest implements RequestShape {
        @NotNull
        public String name;
    }
}

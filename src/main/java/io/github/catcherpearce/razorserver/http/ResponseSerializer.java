package io.github.catcherpearce.razorserver.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * Converts HTTP response bodies to bytes according to their content type.
 * Supports raw byte arrays, JSON serialization, and UTF-8 text encoding.
 */
public class ResponseSerializer {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Serializes the body of the given response.
     * Raw {@code byte[]} bodies are returned directly without copying, regardless
     * of content type. Otherwise, a {@code Content-Type} value starting with
     * {@code application/json} selects JSON serialization, while {@code text/}
     * selects UTF-8 encoding of the body's {@link Object#toString()} result.
     * Content-type prefix comparisons are case-sensitive.
     *
     * @param response the response whose body is to be serialized
     * @return the serialized body, or the original array for a {@code byte[]} body
     * @throws JsonProcessingException if JSON serialization fails
     * @throws UnsupportedOperationException if a non-byte-array body has an
     *         unsupported content type
     * @throws NullPointerException if the response or its headers are null,
     *         a non-byte-array body has no {@code Content-Type} value, or a text
     *         body is null
     */
    public byte[] serialize(HttpResponse response) throws JsonProcessingException {
        String contentType = response.headers().get("Content-Type");

        if (response.body() instanceof byte[] bytes) {
            return bytes;
        }

        if (contentType.startsWith("application/json")) {
            return objectMapper.writeValueAsBytes(response.body());
        }

        if (contentType.startsWith("text/")) {
            return response.body().toString().getBytes(StandardCharsets.UTF_8);
        }

        throw new UnsupportedOperationException();
    }
}

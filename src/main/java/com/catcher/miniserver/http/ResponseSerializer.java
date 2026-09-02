package com.catcher.miniserver.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

public class ResponseSerializer {
    private final ObjectMapper objectMapper = new ObjectMapper();

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
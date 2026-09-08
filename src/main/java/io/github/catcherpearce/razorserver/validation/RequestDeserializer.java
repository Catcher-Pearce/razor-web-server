package io.github.catcherpearce.razorserver.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.catcherpearce.razorserver.exception.RequestBodyDeserializationException;
import io.github.catcherpearce.razorserver.exception.UnsupportedMediaTypeException;

public class RequestDeserializer {

    private final ObjectMapper objectMapper;

    public RequestDeserializer() {
        this.objectMapper = new ObjectMapper();
    }

    public RequestShape deserialize(
            String body,
            String contentType,
            Class<? extends RequestShape> requestShape
    ) {

        if (requestShape == null) {
            return null;
        }

        if (contentType == null) {
            throw new UnsupportedMediaTypeException(null);
        }

        if (contentType.startsWith("application/json")) {
            try {
                return objectMapper.readValue(body, requestShape);
            } catch (JsonProcessingException e) {
                throw new RequestBodyDeserializationException(e);
            }
        }

        throw new UnsupportedMediaTypeException(contentType);
    }
}

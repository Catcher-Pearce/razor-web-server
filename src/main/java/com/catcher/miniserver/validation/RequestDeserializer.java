package com.catcher.miniserver.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.catcher.miniserver.exception.RequestBodyDeserializationException;
import com.catcher.miniserver.exception.UnsupportedMediaTypeException;

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

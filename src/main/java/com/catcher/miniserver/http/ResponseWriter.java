package com.catcher.miniserver.http;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ResponseWriter {
    private final ResponseSerializer responseSerializer;

    public ResponseWriter() {
        this.responseSerializer = new ResponseSerializer();
    }

    public byte[] write(HttpResponse response) throws JsonProcessingException {
        byte[] bodyBytes = responseSerializer.serialize(response);

        StringBuilder responseBuilder = new StringBuilder();

         responseBuilder
                 .append("HTTP/1.1 ")
                 .append(response.status())
                 .append(" ")
                 .append(getReasonPhrase(response.status()))
                 .append("\r\n");

        for (String key : response.headers().keySet()) {
            responseBuilder
                    .append(key)
                    .append(": ")
                    .append(response.headers().get(key))
                    .append("\r\n");
        }

        responseBuilder
                .append("content-length: ")
                .append(bodyBytes.length)
                .append("\r\n")
                .append("\r\n");

        byte[] headerBytes = responseBuilder.toString().getBytes(StandardCharsets.UTF_8);

        byte[] responseBytes = new byte[headerBytes.length + bodyBytes.length];

        System.arraycopy(headerBytes, 0, responseBytes, 0, headerBytes.length);
        System.arraycopy(bodyBytes, 0, responseBytes, headerBytes.length, bodyBytes.length);

        return responseBytes;

    }

    private byte[] buildResponse(HttpResponse response) {
        return null;
    }

    private String getReasonPhrase(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 500 -> "Internal Server Error";
            default -> "Unknown";
        };
    }
}

package io.github.catcherpearce.razorserver.http;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.nio.charset.StandardCharsets;

/**
 * Encodes HTTP responses as HTTP/1.1 messages containing a status line,
 * headers, and a serialized body.
 */
public class ResponseWriter {
    private final ResponseSerializer responseSerializer;

    /**
     * Creates a writer that uses {@link ResponseSerializer} to encode response bodies.
     */
    public ResponseWriter() {
        this.responseSerializer = new ResponseSerializer();
    }

    /**
     * Encodes the given response into bytes, using UTF-8 for the status line and headers.
     * Any supplied {@code Content-Length} header is replaced with the actual body
     * length in bytes. Responses with status {@code 204} have an empty body and
     * bypass body serialization.
     *
     * @param response the response to encode
     * @return the complete HTTP/1.1 message as a byte array
     * @throws JsonProcessingException if JSON serialization of the body fails
     * @throws UnsupportedOperationException if the body is not a byte array and
     *         its content type is unsupported by the serializer
     */
    public byte[] write(HttpResponse response) throws JsonProcessingException {
        byte[] bodyBytes = response.status() == 204
                ? new byte[0]
                : responseSerializer.serialize(response);

        StringBuilder responseBuilder = new StringBuilder();

         responseBuilder
                 .append("HTTP/1.1 ")
                 .append(response.status())
                 .append(" ")
                 .append(getReasonPhrase(response.status()))
                 .append("\r\n");

        for (String key : response.headers().keySet()) {
            if (key.equalsIgnoreCase("Content-Length")) {
                continue;
            }

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

    /**
     * Returns the reason phrase for a supported HTTP status code.
     *
     * @param statusCode the HTTP status code
     * @return the corresponding reason phrase, or {@code "Unknown"} if unsupported
     */
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

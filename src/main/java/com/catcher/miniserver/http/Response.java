package com.catcher.miniserver.http;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory methods for HTTP responses with JSON, plain-text, or raw byte bodies.
 * Each response includes a {@code Content-Type} header and
 * {@code Connection: close}. Bodies are stored without serialization or copying.
 */
public class Response {

    /**
     * Creates a JSON response with status {@code 200 OK}.
     *
     * @param body the response body
     * @return the JSON response
     */
    public static HttpResponse ok(Object body) {
        return json(200, body);
    }

    /**
     * Creates a JSON response with status {@code 201 Created}.
     *
     * @param body the response body
     * @return the JSON response
     */
    public static HttpResponse created(Object body) {
        return json(201, body);
    }

    /**
     * Creates a response with status {@code 204 No Content}, JSON headers,
     * and a null body.
     *
     * @return the response without a body
     */
    public static HttpResponse noContent() {
        Map<String, String> headers = jsonHeaders();

        return new HttpResponse(204, headers, null);
    }

    /**
     * Creates a JSON response with status {@code 400 Bad Request}.
     *
     * @param body the response body
     * @return the JSON response
     */
    public static HttpResponse badRequest(Object body) {
        return json(400, body);
    }

    /**
     * Creates a JSON response with status {@code 401 Unauthorized}.
     *
     * @param body the response body
     * @return the JSON response
     */
    public static HttpResponse unauthorized(Object body) {
        return json(401, body);
    }

    /**
     * Creates a JSON response with status {@code 403 Forbidden}.
     *
     * @param body the response body
     * @return the JSON response
     */
    public static HttpResponse forbidden(Object body) {
        return json(403, body);
    }

    /**
     * Creates a JSON response with status {@code 404 Not Found}.
     *
     * @param body the response body
     * @return the JSON response
     */
    public static HttpResponse notFound(Object body) {
        return json(404, body);
    }

    /**
     * Creates a JSON response with status {@code 500 Internal Server Error}.
     *
     * @param body the response body
     * @return the JSON response
     */
    public static HttpResponse internalServerError(Object body) {
        return json(500, body);
    }

    /**
     * Creates a JSON response with the given status code.
     *
     * @param statusCode the HTTP status code, stored without validation
     * @param body the response body
     * @return the JSON response
     */
    public static HttpResponse status(int statusCode, Object body) {
        return json(statusCode, body);
    }

    /**
     * Creates a JSON response with status {@code 200 OK}.
     *
     * @param body the response body
     * @return the JSON response
     */
    public static HttpResponse json(Object body) {
        return json(200, body);
    }

    /**
     * Creates a response with the given status code and content type
     * {@code application/json; charset=utf-8}.
     *
     * @param statusCode the HTTP status code, stored without validation
     * @param body the response body
     * @return the JSON response
     */
    public static HttpResponse json(int statusCode, Object body) {
        return new HttpResponse(statusCode, jsonHeaders(), body);
    }

    /**
     * Creates a response with status {@code 200 OK}, the supplied content type,
     * and a raw byte body. The byte array is retained without copying.
     *
     * @param bytes the file contents to use as the response body
     * @param contentType the value of the {@code Content-Type} header
     * @return the response containing the supplied bytes
     */
    public static HttpResponse file(byte[] bytes, String contentType) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", contentType);
        headers.put("Connection", "close");

        return new HttpResponse(200, headers, bytes);
    }

    /**
     * Creates a plain-text response with status {@code 200 OK}.
     *
     * @param body the response text
     * @return the plain-text response
     */
    public static HttpResponse text(String body) {
        return text(200, body);
    }

    /**
     * Creates a response with the given status code and content type
     * {@code text/plain; charset=utf-8}.
     *
     * @param statusCode the HTTP status code, stored without validation
     * @param body the response text
     * @return the plain-text response
     */
    public static HttpResponse text(int statusCode, String body) {
        return new HttpResponse(statusCode, textHeaders(), body);
    }

    /**
     * Creates headers declaring UTF-8 JSON content and a closing connection.
     *
     * @return a new mutable header map
     */
    private static Map<String, String> jsonHeaders() {
        Map<String, String> headers = new HashMap<>();

        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("Connection", "close");

        return headers;
    }

    /**
     * Creates headers declaring UTF-8 plain text and a closing connection.
     *
     * @return a new mutable header map
     */
    private static Map<String, String> textHeaders() {
        Map<String, String> headers = new HashMap<>();

        headers.put("Content-Type", "text/plain; charset=utf-8");
        headers.put("Connection", "close");

        return headers;
    }
}

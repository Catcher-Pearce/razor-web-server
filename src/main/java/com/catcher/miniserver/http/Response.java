package com.catcher.miniserver.http;

import java.util.HashMap;
import java.util.Map;

public class Response {

    public static HttpResponse ok(Object body) {
        return json(200, body);
    }

    public static HttpResponse created(Object body) {
        return json(201, body);
    }

    public static HttpResponse noContent() {
        Map<String, String> headers = jsonHeaders();

        return new HttpResponse(204, headers, null);
    }

    public static HttpResponse badRequest(Object body) {
        return json(400, body);
    }

    public static HttpResponse unauthorized(Object body) {
        return json(401, body);
    }

    public static HttpResponse forbidden(Object body) {
        return json(403, body);
    }

    public static HttpResponse notFound(Object body) {
        return json(404, body);
    }

    public static HttpResponse internalServerError(Object body) {
        return json(500, body);
    }

    public static HttpResponse status(int statusCode, Object body) {
        return json(statusCode, body);
    }

    public static HttpResponse json(Object body) {
        return json(200, body);
    }

    public static HttpResponse json(int statusCode, Object body) {
        return new HttpResponse(statusCode, jsonHeaders(), body);
    }

    public static HttpResponse file(byte[] bytes, String contentType) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", contentType);
        headers.put("Connection", "close");

        return new HttpResponse(200, headers, bytes);
    }

    public static HttpResponse text(String body) {
        return text(200, body);
    }

    public static HttpResponse text(int statusCode, String body) {
        return new HttpResponse(statusCode, textHeaders(), body);
    }

    private static Map<String, String> jsonHeaders() {
        Map<String, String> headers = new HashMap<>();

        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("Connection", "close");

        return headers;
    }

    private static Map<String, String> textHeaders() {
        Map<String, String> headers = new HashMap<>();

        headers.put("Content-Type", "text/plain; charset=utf-8");
        headers.put("Connection", "close");

        return headers;
    }
}

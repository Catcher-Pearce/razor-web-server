package com.catcher.miniserver.http;

import java.util.HashMap;
import java.util.Map;

public class Response {

    public static HttpResponse ok(Object body) {
        return new HttpResponse(200, defaultHeaders(), body);
    }

    public static HttpResponse created(Object body) {
        return new HttpResponse(201, defaultHeaders(), body);
    }

    public static HttpResponse noContent() {
        Map<String, String> headers = defaultHeaders();
        headers.put("content-length", "0");

        return new HttpResponse(204, defaultHeaders(), null);
    }

    public static HttpResponse badRequest(Object body) {
        return new HttpResponse(400, defaultHeaders(), body);
    }

    public static HttpResponse unauthorized(Object body) {
        return new HttpResponse(401, defaultHeaders(), body);
    }

    public static HttpResponse forbidden(Object body) {
        return new HttpResponse(403, defaultHeaders(), body);
    }

    public static HttpResponse notFound(Object body) {
        return new HttpResponse(404, defaultHeaders(), body);
    }

    public static HttpResponse internalServerError(Object body) {
        return new HttpResponse(500, defaultHeaders(), body);
    }

    public static HttpResponse status(int statusCode, Object body) {
        return new HttpResponse(statusCode, defaultHeaders(), body);
    }

    public static HttpResponse file(byte[] bytes, String contentType) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", contentType);
        headers.put("Connection", "close");

        return new HttpResponse(200, headers, bytes);
    }

    private static Map<String, String> defaultHeaders() {
        Map<String, String> headers = new HashMap<>();

        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("Connection", "close");

        return headers;
    }
}

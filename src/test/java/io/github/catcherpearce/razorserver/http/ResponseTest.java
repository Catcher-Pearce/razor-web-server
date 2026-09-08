package io.github.catcherpearce.razorserver.http;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResponseTest {

    @Test
    void createsJsonResponseWithDefaultStatus() {
        Map<String, String> body = Map.of("message", "Hello");

        HttpResponse response = Response.json(body);

        assertEquals(200, response.status());
        assertEquals("application/json; charset=utf-8", response.headers().get("Content-Type"));
        assertEquals(body, response.body());
    }

    @Test
    void createsJsonResponseWithCustomStatus() {
        HttpResponse response = Response.json(202, Map.of("status", "pending"));

        assertEquals(202, response.status());
        assertEquals("application/json; charset=utf-8", response.headers().get("Content-Type"));
    }

    @Test
    void keepsOkAsAJsonConvenience() {
        HttpResponse response = Response.ok("Hello");

        assertEquals(200, response.status());
        assertEquals("application/json; charset=utf-8", response.headers().get("Content-Type"));
    }

    @Test
    void createsPlainTextResponseWithDefaultStatus() {
        HttpResponse response = Response.text("Hello");

        assertEquals(200, response.status());
        assertEquals("text/plain; charset=utf-8", response.headers().get("Content-Type"));
        assertEquals("Hello", response.body());
    }

    @Test
    void createsPlainTextResponseWithCustomStatus() {
        HttpResponse response = Response.text(202, "Pending");

        assertEquals(202, response.status());
        assertEquals("text/plain; charset=utf-8", response.headers().get("Content-Type"));
        assertEquals("Pending", response.body());
    }
}

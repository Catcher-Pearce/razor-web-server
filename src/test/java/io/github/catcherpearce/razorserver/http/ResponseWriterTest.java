package io.github.catcherpearce.razorserver.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseWriterTest {
    private final ResponseWriter responseWriter = new ResponseWriter();

    @Test
    void writesAPlainTextResponseInHttpFormat() throws Exception {
        HttpResponse response = Response.text("Hello");

        String rawResponse = writeAsText(response);

        assertTrue(rawResponse.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(rawResponse.contains("Content-Type: text/plain; charset=utf-8\r\n"));
        assertTrue(rawResponse.contains("Connection: close\r\n"));
        assertTrue(rawResponse.contains("content-length: 5\r\n"));
        assertTrue(rawResponse.endsWith("\r\n\r\nHello"));
    }

    @Test
    void serializesAJsonResponse() throws Exception {
        HttpResponse response = Response.json(Map.of("message", "Hello"));

        String rawResponse = writeAsText(response);

        assertTrue(rawResponse.contains(
                "Content-Type: application/json; charset=utf-8\r\n"
        ));
        assertTrue(rawResponse.contains("content-length: 19\r\n"));
        assertTrue(rawResponse.endsWith("\r\n\r\n{\"message\":\"Hello\"}"));
    }

    @Test
    void calculatesContentLengthFromUtf8Bytes() throws Exception {
        HttpResponse response = Response.text("café");

        String rawResponse = writeAsText(response);

        assertTrue(rawResponse.contains("content-length: 5\r\n"));
        assertTrue(rawResponse.endsWith("\r\n\r\ncafé"));
    }

    @Test
    void preservesBinaryResponseBytes() throws Exception {
        byte[] body = new byte[]{0, 1, 2, (byte) 255};
        HttpResponse response = Response.file(body, "application/octet-stream");

        byte[] rawResponse = responseWriter.write(response);
        int bodyStart = findBodyStart(rawResponse);

        String headers = new String(
                rawResponse,
                0,
                bodyStart,
                StandardCharsets.UTF_8
        );
        assertTrue(headers.contains("Content-Type: application/octet-stream\r\n"));
        assertTrue(headers.contains("content-length: 4\r\n"));
        assertArrayEquals(
                body,
                java.util.Arrays.copyOfRange(rawResponse, bodyStart, rawResponse.length)
        );
    }

    @Test
    void writesNoBodyForNoContentResponse() throws Exception {
        String rawResponse = writeAsText(Response.noContent());

        assertTrue(rawResponse.startsWith("HTTP/1.1 204 No Content\r\n"));
        assertTrue(rawResponse.contains("content-length: 0\r\n"));
        assertTrue(rawResponse.endsWith("\r\n\r\n"));
        assertEquals(rawResponse.length() - 4, rawResponse.lastIndexOf("\r\n\r\n"));
    }

    @Test
    void writesCustomResponseHeaders() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain; charset=utf-8");
        headers.put("X-Request-Id", "request-123");
        HttpResponse response = new HttpResponse(200, headers, "Hello");

        String rawResponse = writeAsText(response);

        assertTrue(rawResponse.contains("X-Request-Id: request-123\r\n"));
    }

    @Test
    void replacesAnExistingContentLengthHeader() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain; charset=utf-8");
        headers.put("Content-Length", "999");
        HttpResponse response = new HttpResponse(200, headers, "Hello");

        String rawResponse = writeAsText(response);

        assertFalse(rawResponse.contains("Content-Length: 999\r\n"));
        assertEquals(1, countOccurrences(
                rawResponse.toLowerCase(),
                "content-length:"
        ));
        assertTrue(rawResponse.contains("content-length: 5\r\n"));
    }

    @ParameterizedTest
    @CsvSource({
            "200, OK",
            "201, Created",
            "204, No Content",
            "400, Bad Request",
            "401, Unauthorized",
            "403, Forbidden",
            "404, Not Found",
            "405, Method Not Allowed",
            "500, Internal Server Error",
            "418, Unknown"
    })
    void writesTheReasonPhraseForAStatus(int status, String reason) throws Exception {
        Object body = status == 204 ? null : "body";
        HttpResponse response = new HttpResponse(
                status,
                Map.of("Content-Type", "text/plain; charset=utf-8"),
                body
        );

        String rawResponse = writeAsText(response);

        assertTrue(rawResponse.startsWith(
                "HTTP/1.1 " + status + " " + reason + "\r\n"
        ));
    }

    private String writeAsText(HttpResponse response) throws Exception {
        return new String(responseWriter.write(response), StandardCharsets.UTF_8);
    }

    private int findBodyStart(byte[] response) {
        byte[] separator = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);

        for (int i = 0; i <= response.length - separator.length; i++) {
            boolean matches = true;
            for (int j = 0; j < separator.length; j++) {
                if (response[i + j] != separator[j]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return i + separator.length;
            }
        }

        throw new AssertionError("HTTP response did not contain a header separator");
    }

    private int countOccurrences(String value, String target) {
        int count = 0;
        int index = 0;

        while ((index = value.indexOf(target, index)) != -1) {
            count++;
            index += target.length();
        }

        return count;
    }
}

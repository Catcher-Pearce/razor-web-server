package com.catcher.miniserver.file_serving;

import com.catcher.miniserver.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StaticFileHandlerTest {

    @TempDir
    Path root;

    @Test
    void servesIndexHtmlForRootRequest() throws Exception {
        String html = "<h1>Hello</h1>";
        Files.writeString(root.resolve("index.html"), html, StandardCharsets.UTF_8);
        StaticFileHandler handler = new StaticFileHandler(root);

        HttpResponse response = handler.serve("/");

        assertEquals(200, response.status());
        assertEquals("text/html; charset=utf-8", response.headers().get("Content-Type"));
        assertEquals("close", response.headers().get("Connection"));
        assertArrayEquals(html.getBytes(StandardCharsets.UTF_8), (byte[]) response.body());
    }

    @Test
    void servesAFileFromANestedDirectory() throws Exception {
        Path scripts = Files.createDirectory(root.resolve("scripts"));
        byte[] javascript = "console.log('hello');".getBytes(StandardCharsets.UTF_8);
        Files.write(scripts.resolve("app.js"), javascript);
        StaticFileHandler handler = new StaticFileHandler(root);

        HttpResponse response = handler.serve("/scripts/app.js");

        assertEquals(200, response.status());
        assertEquals("application/javascript; charset=utf-8", response.headers().get("Content-Type"));
        assertArrayEquals(javascript, (byte[]) response.body());
    }

    @Test
    void returnsNotFoundForAMissingFile() {
        StaticFileHandler handler = new StaticFileHandler(root);

        HttpResponse response = handler.serve("/missing.css");

        assertEquals(404, response.status());
        assertEquals("text/plain; charset=utf-8", response.headers().get("Content-Type"));
        assertEquals("File not found", response.body());
    }

    @Test
    void forbidsPathsOutsideTheStaticRoot() {
        StaticFileHandler handler = new StaticFileHandler(root);

        HttpResponse response = handler.serve("/../secret.txt");

        assertEquals(403, response.status());
        assertEquals("Forbidden", response.body());
    }
}

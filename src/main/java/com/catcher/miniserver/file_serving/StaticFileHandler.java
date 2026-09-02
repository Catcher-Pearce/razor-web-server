package com.catcher.miniserver.file_serving;

import com.catcher.miniserver.http.HttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class StaticFileHandler {
    private final Path root;

    public StaticFileHandler(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public HttpResponse serve(String requestPath) {
        String relativePath = requestPath.equals("/")
                ? "index.html"
                : requestPath.substring(1);

        Path file = root.resolve(relativePath).normalize();

        if (!file.startsWith(root)) {
            return errorResponse(403, "Forbidden");
        }

        if (!Files.isRegularFile(file)) {
            return errorResponse(404, "File not found");
        }

        try {
            byte[] contents = Files.readAllBytes(file);

            return new HttpResponse(
                    200,
                    headers(determineContentType(file)),
                    contents
            );
        } catch (IOException exception) {
            return errorResponse(500, "Could not read file");
        }
    }

    private HttpResponse errorResponse(int status, String message) {
        return new HttpResponse(
                status,
                headers("text/plain; charset=utf-8"),
                message
        );
    }

    private Map<String, String> headers(String contentType) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", contentType);
        headers.put("Connection", "close");
        return headers;
    }

    private String determineContentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();

        if (name.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (name.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (name.endsWith(".svg")) {
            return "image/svg+xml";
        }

        return "application/octet-stream";
    }
}
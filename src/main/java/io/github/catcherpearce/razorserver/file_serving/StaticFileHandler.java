package io.github.catcherpearce.razorserver.file_serving;

import io.github.catcherpearce.razorserver.http.HttpResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Serves files relative to a configured directory as HTTP responses.
 * The root request path maps to {@code index.html}; successful responses
 * contain the file bytes and a content type selected from the file extension.
 */
public final class StaticFileHandler {
    private final Path root;

    /**
     * Creates a handler using the absolute, normalized form of the given root.
     * The directory is not checked for existence until a file is requested.
     *
     * @param root directory against which request paths are resolved
     * @throws NullPointerException if {@code root} is null
     */
    public StaticFileHandler(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    /**
     * Reads a requested file, mapping {@code /} to {@code index.html}.
     * Other paths have their leading slash removed before resolution.
     *
     * <p>Returns 403 if the normalized path is outside the root, 404 if the
     * resolved path is not a regular file, or 500 if reading the file throws
     * an {@link IOException}. The containment check is lexical and does not
     * resolve symbolic links.</p>
     *
     * @param requestPath non-empty path beginning with {@code /}, without a query string
     * @return a 200 response containing file bytes, or a plain-text error response;
     *         all responses include {@code Connection: close}
     */
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

    /**
     * Creates a plain-text error response using UTF-8.
     *
     * @param status HTTP error status code
     * @param message response body
     * @return the error response
     */
    private HttpResponse errorResponse(int status, String message) {
        return new HttpResponse(
                status,
                headers("text/plain; charset=utf-8"),
                message
        );
    }

    /**
     * Creates headers specifying the content type and connection closure.
     *
     * @param contentType value of the {@code Content-Type} header
     * @return a new mutable header map
     */
    private Map<String, String> headers(String contentType) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", contentType);
        headers.put("Connection", "close");
        return headers;
    }

    /**
     * Selects a content type from the lowercased filename extension.
     * Supports HTML, CSS, JavaScript, PNG, JPEG, SVG, and PDF files.
     *
     * @param file path whose filename is inspected; file contents are not read
     * @return the matching content type, or {@code application/octet-stream}
     *         for an unrecognized extension
     */
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
        if (name.endsWith(".pdf")) {
            return "application/pdf";
        }

        return "application/octet-stream";
    }
}

package com.catcher.miniserver.http;

import com.catcher.miniserver.exception.MalformedHttpRequestException;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Reads an HTTP/1.1 request from a byte stream for routing and handling.
 * Supports the methods declared by {@link HttpMethod}, headers, and a body
 * framed by {@code Content-Length}. Chunked body decoding is not implemented.
 *
 * <p>Header names are lowercased and bodies are decoded as UTF-8. Absolute
 * HTTP(S) request URLs are converted to raw paths with their query strings
 * retained. The caller owns the stream and is responsible for closing it.</p>
 */
public class HttpParser {
    /** The source of request bytes; reads may block until data becomes available. */
    InputStream inputStream;

    /** Maximum request-line size in bytes, including its terminating CRLF. */
    static final int MAX_REQUEST_LINE_BYTES = 8 * 1024;

    /** Maximum bytes after the request line through the blank line ending the headers. */
    static final int MAX_HEADER_BYTES = 32 * 1024;

    /** Maximum permitted body size in bytes, as declared by Content-Length. */
    static final int MAX_BODY_BYTES = 1024 * 1024;

    /**
     * Creates a parser that reads from the supplied stream without closing it.
     *
     * @param inputStream stream containing the HTTP request bytes
     */
    public HttpParser(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Reads and parses one request from the stream's current position.
     * Reads through the blank line terminating the headers, then consumes
     * exactly the number of body bytes declared by {@code Content-Length}.
     * Without that header, the returned body is an empty string.
     *
     * <p>The request line must contain a supported method, a request target,
     * and {@code HTTP/1.1}. Header names and values are trimmed, names are
     * lowercased, and duplicate names are rejected. The returned path retains
     * its raw query string and percent encoding for later routing.</p>
     *
     * @return the parsed method, path and query, version, headers, and UTF-8 body
     * @throws MalformedHttpRequestException if request syntax or supported
     *         method/version requirements fail, a size limit is exceeded,
     *         Content-Length is invalid, or the stream ends before the headers
     *         or declared body are complete
     * @throws RuntimeException if reading fails with an {@link IOException},
     *         which is retained as the cause
     */
    public HttpRequest parse() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Deque<Integer> lastFour = new ArrayDeque<>();
        int requestLineBytes = 0;
        int headerBytes = 0;
        boolean readingRequestLine = true;
        int previousByte = -1;
        boolean headersComplete = false;


        try {
            int nextByte;

            while ((nextByte = inputStream.read()) != -1) {
                buffer.write(nextByte);

                if (readingRequestLine) {
                    requestLineBytes++;

                    if (requestLineBytes > MAX_REQUEST_LINE_BYTES) {
                        throw new MalformedHttpRequestException("Maximum request line length reached");
                    }

                    if (previousByte == '\r' && nextByte == '\n') {
                        readingRequestLine = false;
                    }
                } else {
                    headerBytes++;

                    if (headerBytes > MAX_HEADER_BYTES) {
                        throw new MalformedHttpRequestException("Maximum header length reached");
                    }
                }

                lastFour.addLast(nextByte);

                if (lastFour.size() > 4) {
                    lastFour.removeFirst();
                }

                if (lastFour.size() == 4) {
                    Integer[] bytes = lastFour.toArray(new Integer[0]);

                    if (bytes[0] == '\r' &&
                            bytes[1] == '\n' &&
                            bytes[2] == '\r' &&
                            bytes[3] == '\n') {
                        headersComplete = true;
                        break;
                    }
                }

                previousByte = nextByte;
            }

            if (!headersComplete) {
                throw new MalformedHttpRequestException(
                        "Unexpected end of stream while reading headers"
                );
            }

            String headerText =
                    buffer.toString(StandardCharsets.UTF_8);

            BufferedReader reader = new BufferedReader(new StringReader(headerText));
            String[] requestLine = reader.readLine().split("\\s+");

            if (requestLine.length != 3) {
                throw new MalformedHttpRequestException("Malformed request line");
            }

            String method = requestLine[0];
            String path = requestLine[1];
            String version = requestLine[2];

            if (!version.equals("HTTP/1.1")) {
                throw new MalformedHttpRequestException("Version not supported, please use HTTP/1.1");
            }

            String line;
            Map<String, String> headers = new HashMap<>();

            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                int colonIndex = line.indexOf(':');

                if (colonIndex <= 0) {
                    throw new MalformedHttpRequestException("Malformed header");
                }

                String key = line.substring(0, colonIndex).trim().toLowerCase(Locale.ROOT);
                String value = line.substring(colonIndex + 1).trim();

                if (key.isEmpty()) {
                    throw new MalformedHttpRequestException("Malformed header");
                }

                if (headers.containsKey(key)) {
                    throw new MalformedHttpRequestException("Duplicate header");
                }
                headers.put(key, value);
            }

            path = normalizeRequestTarget(path, headers);

            buffer = new ByteArrayOutputStream();


            String contentLengthValue = headers.get("content-length");

            if (contentLengthValue != null) {
                if (contentLengthValue.isEmpty() ||
                        !contentLengthValue.chars().allMatch(Character::isDigit)) {
                    throw new MalformedHttpRequestException("Invalid content-length");
                }

                long contentLength = Long.parseLong(contentLengthValue);

                if (contentLength < 0 || contentLength > MAX_BODY_BYTES) {
                    throw new MalformedHttpRequestException("Request body too large");
                }

                for (long i = 0; i < contentLength; i++) {
                    nextByte = inputStream.read();

                    if (nextByte == -1) {
                        throw new MalformedHttpRequestException("Unexpected end of stream while reading request body");
                    }

                    buffer.write(nextByte);
                }
            }

            String body =
                    buffer.toString(StandardCharsets.UTF_8);

            return new HttpRequest(
                    HttpMethod.valueOf(method.toUpperCase()),
                    path,
                    version,
                    headers,
                    body
            );

        } catch (IllegalArgumentException | IndexOutOfBoundsException | NullPointerException e) {
            throw new MalformedHttpRequestException("Malformed HTTP request", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts an absolute HTTP(S) URL to its raw path and query for routing.
     * The URL authority takes precedence over Host for absolute-form requests.
     * Origin-form targets are retained without decoding or path normalization.
     * An absolute URL with an empty path uses {@code /}.
     *
     * @param target request target from the request line
     * @param headers mutable parsed headers; the host entry is replaced with
     *                the authority when the target is an absolute URL
     * @return raw path with the query string appended when present
     * @throws MalformedHttpRequestException if an absolute target has an
     *         unsupported scheme, no valid host, user information, or a fragment
     * @throws IllegalArgumentException if the target is not a syntactically
     *         valid URI; {@link #parse()} converts this to a malformed-request error
     */
    private String normalizeRequestTarget(String target, Map<String, String> headers) {
        if (target.startsWith("/")) {
            return target;
        }

        URI uri = URI.create(target);
        if ((!"http".equalsIgnoreCase(uri.getScheme())
                && !"https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null
                || uri.getRawUserInfo() != null
                || uri.getRawFragment() != null) {
            throw new MalformedHttpRequestException("Invalid absolute request URL");
        }

        headers.put("host", uri.getRawAuthority());
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        String query = uri.getRawQuery();
        return query == null ? path : path + "?" + query;
    }
}

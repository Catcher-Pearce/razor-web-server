package com.catcher.miniserver.http;

import com.catcher.miniserver.exception.MalformedHttpRequestException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpParser {
    InputStream inputStream;
    static final int MAX_REQUEST_LINE_BYTES = 8 * 1024;
    static final int MAX_HEADER_BYTES = 32 * 1024;
    static final int MAX_BODY_BYTES = 1024 * 1024;

    public HttpParser(InputStream inputStream) {
        this.inputStream = inputStream;
    }

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
}

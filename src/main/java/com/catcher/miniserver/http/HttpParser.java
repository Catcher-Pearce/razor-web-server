package com.catcher.miniserver.http;

import com.catcher.miniserver.exception.MalformedHttpRequestException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpParser {
    InputStream inputStream;

    public HttpParser(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public HttpRequest parse() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        Deque<Integer> lastFour = new ArrayDeque<>();

        try {
            int nextByte;

            while ((nextByte = inputStream.read()) != -1) {
                buffer.write(nextByte);

                lastFour.addLast(nextByte);

                if (lastFour.size() > 4) {
                    lastFour.removeFirst();
                }

                if (lastFour.size() == 4) {
                    Integer[] bytes = lastFour.toArray(new Integer[0]);

                    if (bytes[0] == 13 &&
                            bytes[1] == 10 &&
                            bytes[2] == 13 &&
                            bytes[3] == 10) {
                        break;
                    }
                }
            }

            String headerText =
                    buffer.toString(StandardCharsets.UTF_8);

            BufferedReader reader = new BufferedReader(new StringReader(headerText));
            String[] requestLine = reader.readLine().split("\\s+");

            String method = requestLine[0];
            String path = requestLine[1];
            String version = requestLine[2];

            String line;
            Map<String, String> headers = new HashMap<String, String>();

            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                int colonIndex = line.indexOf(":");
                if (colonIndex != -1) {
                    String key = line.substring(0, colonIndex).trim();
                    String value = line.substring(colonIndex + 1).trim();
                    headers.put(key, value);
                }
            }

            buffer = new ByteArrayOutputStream();

            if (headers.containsKey("Content-Length")) {
                for (int i = 0; i < Integer.parseInt(headers.get("Content-Length")); i++) {
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

        } catch (MalformedHttpRequestException e) {
            throw e;
        } catch (IllegalArgumentException | IndexOutOfBoundsException | NullPointerException e) {
            throw new MalformedHttpRequestException("Malformed HTTP request", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

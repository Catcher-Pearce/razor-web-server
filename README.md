# Mini Server

Mini Server is my small, from-scratch HTTP server written in Java. I started it to better understand what frameworks usually hide: accepting socket connections, parsing raw HTTP, matching routes, and turning Java values into HTTP responses.

It is still a work in progress, but the basic request-to-response path is up and running.

## What is implemented

- A blocking TCP server built with `ServerSocket`
- HTTP request parsing for the request line, headers, and `content-length` body
- Support for `GET`, `POST`, `PUT`, `PATCH`, and `DELETE` methods
- Exact-path, method-based route registration and dispatch
- Lambda-based request handlers
- HTTP/1.1 response writing with status lines, headers, and calculated content length
- JSON response serialization with Jackson
- Convenience responses for common status codes, including `200`, `201`, `204`, `400`, `401`, `403`, `404`, and `500`
- Initial request-body infrastructure with `RequestShape` and JSON deserialization
- Validation annotation definitions for `@NotNull`, `@Min`, `@Max`, and `@Size`

## Current example

The example application in `Main.java` starts the server on port `3000` and registers one route:

```java
MiniServer server = new MiniServer(3000);

server.get("/", request -> Response.ok("Hello World"));

server.start();
```

After starting the application, try it with:

```bash
curl http://localhost:3000/
```

The response body is:

```json
"Hello World"
```

## Running locally

You will need Java 25 and Maven.

Compile the project with:

```bash
mvn compile
```

Then run `com.catcher.miniserver.example.Main` from your IDE. The server will keep listening on port `3000` until the process is stopped.

## Project layout

```text
src/main/java/com/catcher/miniserver/
├── example/      # Example application
├── http/         # HTTP models, parsing, serialization, and response helpers
├── server/       # Socket engine, routing, dispatch, and handlers
└── validation/   # Request shapes, deserialization, and validation annotations
```

## Still in progress

This is intentionally a learning project rather than a production-ready server. The next pieces still need to be connected or expanded:

- Deserializing registered request shapes during request mapping
- Applying the validation annotations to incoming request bodies
- Path variables and query-string parsing
- Friendly `404`, `405`, and malformed-request responses
- Concurrent client handling and persistent connections
- Automated tests and more example routes

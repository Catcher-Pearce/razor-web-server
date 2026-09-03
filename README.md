# Mini Server

Mini Server is my small, from-scratch HTTP server written in Java. I started it to better understand what frameworks usually hide: accepting socket connections, parsing raw HTTP, matching routes, and turning Java values into HTTP responses.

It is still a work in progress, but the basic request-to-response path is up and running.

## What is implemented

- A blocking TCP server built with `ServerSocket`
- HTTP request parsing for the request line, headers, and `content-length` body
- Support for `GET`, `POST`, `PUT`, `PATCH`, and `DELETE` methods
- Method-based route registration with literal and named path segments
- Route precedence that favors the most specific literal path
- UTF-8 query-string parsing with form-style percent decoding
- Lambda-based request handlers
- HTTP/1.1 response writing with status lines, headers, and calculated content length
- JSON response serialization with Jackson
- Convenience responses for common status codes, including `200`, `201`, `204`, `400`, `401`, `403`, `404`, and `500`
- JSON request-body deserialization into registered `RequestShape` classes
- Static-file serving with path traversal protection
- Structured `400`, `404`, `405`, `415`, and `500` error responses
- Concurrent request handling with a bounded thread pool
- Validation annotation definitions for `@NotNull`, `@Min`, `@Max`, and `@Size`
- Automated tests for HTTP parsing, routing, query parameters, route patterns, and static files

## Current example

The example application in `Main.java` starts the server on port `3000`. Routes
can read named path variables and query parameters from `ServerRequest`:

```java
MiniServer server = new MiniServer(3000);

server.get("/", request -> Response.ok("Hello World"));

server.get("/users/{userId}", request -> {
    String userId = request.pathVariables().get("userId");
    String name = request.queryParams().get("name");

    if (name == null) {
        return Response.badRequest("name query parameter required");
    }

    return Response.ok(Map.of(
            "userId", userId,
            "name", name
    ));
});

server.start();
```

After starting the application, try it with:

```bash
curl http://localhost:3000/
curl 'http://localhost:3000/users/42?name=Ada%20Lovelace'
```

Response bodies returned by the convenience helpers are serialized as JSON:

```json
{"userId":"42","name":"Ada Lovelace"}
```

`Response` provides helpers such as `ok`, `created`, `noContent`,
`badRequest`, `unauthorized`, `forbidden`, `notFound`,
`internalServerError`, and `status`. A handler can also return an
`HttpResponse` directly when it needs to control the status, headers, and body.

## Query parameters

Everything after the first literal `?` is parsed as the query string and does
not participate in route matching. Keys and values are UTF-8 percent-decoded,
and `+` is treated as a space. For example:

```text
/search?q=java+server&expression=a%26b%3Dc
```

produces:

```java
Map.of(
        "q", "java server",
        "expression", "a&b=c"
)
```

A parameter without a value (`?debug`) and an explicitly empty value
(`?debug=`) both produce an empty string. Empty keys, empty parameter entries,
duplicate keys, invalid percent encoding, queries over 8,192 characters, and
queries with more than 100 parameters are rejected with `400 Bad Request`.

## Running locally

You will need Java 25 and Maven.

Compile the project with:

```bash
mvn compile
```

Run the automated tests with:

```bash
mvn test
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

- Applying the validation annotations to incoming request bodies
- Persistent HTTP connections and request pipelining
- Broader HTTP protocol compliance and request limits
- More response content types and richer header handling
- More integration tests and example routes

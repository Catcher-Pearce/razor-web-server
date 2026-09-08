package io.github.catcherpearce.razorserver.example;

import io.github.catcherpearce.razorserver.http.Response;
import io.github.catcherpearce.razorserver.server.RazorServer;
import io.github.catcherpearce.razorserver.validation.RequestShape;
import io.github.catcherpearce.razorserver.validation.annotations.Max;
import io.github.catcherpearce.razorserver.validation.annotations.Min;
import io.github.catcherpearce.razorserver.validation.annotations.NotNull;
import io.github.catcherpearce.razorserver.validation.annotations.Size;

import java.nio.file.Path;
import java.util.Map;

public class Main {
    public record UserBody(
            @NotNull @Size(min = 2, max = 50) String name,
            @NotNull @Min(18) @Max(120) Integer age
    ) implements RequestShape {}

    public record RenameBody(
            @NotNull @Size(min = 2, max = 50) String name
    ) implements RequestShape {}

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "3000"));
        RazorServer server = new RazorServer(port);

        // Literal routes take precedence over variable routes.
        server.get("/users/me", request -> Response.text("Current user"));

        server.get("/users/{userId}", request -> Response.ok(Map.of(
                "userId", request.pathVariables().get("userId"),
                "greeting", request.queryParams().getOrDefault("greeting", "Hello")
        )));

        // The mapper deserializes and validates JSON before invoking the handler.
        server.post("/users", request -> {
            UserBody body = request.bodyAs(UserBody.class);
            return Response.created(body);
        }, UserBody.class);

        server.put("/users/{userId}", request -> {
            UserBody body = request.bodyAs(UserBody.class);
            return Response.ok(Map.of(
                    "userId", request.pathVariables().get("userId"),
                    "replacement", body
            ));
        }, UserBody.class);

        server.patch("/users/{userId}", request -> {
            RenameBody body = request.bodyAs(RenameBody.class);
            return Response.ok(Map.of(
                    "userId", request.pathVariables().get("userId"),
                    "name", body.name()
            ));
        }, RenameBody.class);

        server.delete("/users/{userId}", request -> Response.noContent());

        // Without a request shape, the body remains a raw string.
        server.post("/echo", request -> Response.text((String) request.body()));

        server.start();
    }
}

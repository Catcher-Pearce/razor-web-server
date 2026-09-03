package com.catcher.miniserver.example;

import com.catcher.miniserver.http.Response;
import com.catcher.miniserver.server.MiniServer;

public class Main {
    public static void main(String[] args) {
        MiniServer server = new MiniServer(3000);

        server.get("/", (request) -> {
            return Response.ok("Hello World");
        });

        server.get("/users/{userId}", (request) -> {
            String userId = request.pathVariables().get("userId");
            if (request.queryParams().containsKey("name")) {
                return Response.ok("User ID: " + request.queryParams().get("name"));
            }
            return Response.badRequest("name query parameter required");
            }
        );

        server.start();
    }
}

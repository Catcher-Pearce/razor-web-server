package com.catcher.miniserver.example;

import com.catcher.miniserver.http.Response;
import com.catcher.miniserver.server.MiniServer;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        int port = Integer.parseInt(
                System.getenv().getOrDefault("PORT", "3000")
        );
        MiniServer server = new MiniServer(port);

        server.get("/", request -> Response.text("Mini Server is running"));

        server.post(
                "/users",
                request -> {
                    CreateUserRequest body = request.bodyAs(CreateUserRequest.class);

                    return Response.created(Map.of(
                            "message", "User request is valid",
                            "name", body.name(),
                            "age", body.age()
                    ));
                },
                CreateUserRequest.class
        );

        server.start();
    }
}

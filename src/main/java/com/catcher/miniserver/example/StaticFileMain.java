package com.catcher.miniserver.example;

import com.catcher.miniserver.server.MiniServer;

import java.nio.file.Path;

/** A separate manual test application for static-file serving. */
public final class StaticFileMain {
    private StaticFileMain() {
    }

    public static void main(String[] args) {
        MiniServer server = new MiniServer(3001);

        server.staticFiles(Path.of("static-test"));
        server.start();
    }
}

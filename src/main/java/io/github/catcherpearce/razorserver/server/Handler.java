package io.github.catcherpearce.razorserver.server;

import io.github.catcherpearce.razorserver.http.HttpResponse;

@FunctionalInterface
public interface Handler {
    HttpResponse handle(ServerRequest serverRequest);
}

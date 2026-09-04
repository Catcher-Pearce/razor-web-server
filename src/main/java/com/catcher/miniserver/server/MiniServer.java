package com.catcher.miniserver.server;

import com.catcher.miniserver.validation.RequestShape;
import com.catcher.miniserver.http.HttpMethod;

import java.nio.file.Path;

/**
 * Public API for configuring and managing an HTTP server.
 * Supports route registration and controls the server lifecycle.
 */
public class MiniServer {
    private final ServerEngine serverEngine;
    private final RequestDispatcher requestDispatcher = new RequestDispatcher();

    /**
     * Creates a server that listens on the specified port.
     *
     * @param port TCP port on which the server accepts connections
     */
    public MiniServer(int port) {
        this.serverEngine = new ServerEngine(port, requestDispatcher);

        Runtime.getRuntime().addShutdownHook(
                new Thread(this::stop, "mini-server-shutdown"));
    }

    /**
     * Starts accepting requests and blocks until the server is stopped.
     */
    public void start() {
        serverEngine.start();
    }

    /**
     * Stops accepting requests and shuts down the server's worker threads.
     */
    public void stop() {
        serverEngine.stop();
    }

    /**
     * Configures a directory from which unmatched GET requests may be served.
     *
     * @param root root directory containing static files
     */
    public void staticFiles(Path root) {
        requestDispatcher.serveStaticFiles(root);
    }

    /**
     * Registers a GET route.
     *
     * @param path route path pattern
     * @param handler handler invoked for matching requests
     */
    public void get(String path, Handler handler) {
        requestDispatcher.createRoute(path, HttpMethod.GET, handler, null);
    }

    /**
     * Registers a POST route with no typed request body.
     *
     * @param path route path pattern
     * @param handler handler invoked for matching requests
     */
    public void post(String path, Handler handler) {
        requestDispatcher.createRoute(path, HttpMethod.POST, handler, null);
    }

    /**
     * Registers a POST route whose body is deserialized and validated against
     * the supplied request shape before its handler is invoked.
     *
     * @param path route path pattern
     * @param handler handler invoked for matching requests
     * @param requestShape type used to deserialize and validate the request body
     */
    public void post(
            String path,
            Handler handler,
            Class<? extends RequestShape> requestShape
    ) {
        requestDispatcher.createRoute(path, HttpMethod.POST, handler, requestShape);
    }

    /**
     * Registers a PATCH route with no typed request body.
     *
     * @param path route path pattern
     * @param handler handler invoked for matching requests
     */
    public void patch(String path, Handler handler) {
        requestDispatcher.createRoute(path, HttpMethod.PATCH, handler, null);
    }

    /**
     * Registers a PATCH route whose body is deserialized and validated against
     * the supplied request shape before its handler is invoked.
     *
     * @param path route path pattern
     * @param handler handler invoked for matching requests
     * @param requestShape type used to deserialize and validate the request body
     */
    public void patch(
            String path,
            Handler handler,
            Class<? extends RequestShape> requestShape
    ) {
        requestDispatcher.createRoute(path, HttpMethod.PATCH, handler, requestShape);
    }

    /**
     * Registers a PUT route with no typed request body.
     *
     * @param path route path pattern
     * @param handler handler invoked for matching requests
     */
    public void put(String path, Handler handler) {
        requestDispatcher.createRoute(path, HttpMethod.PUT, handler, null);
    }

    /**
     * Registers a PUT route whose body is deserialized and validated against
     * the supplied request shape before its handler is invoked.
     *
     * @param path route path pattern
     * @param handler handler invoked for matching requests
     * @param requestShape type used to deserialize and validate the request body
     */
    public void put(
            String path,
            Handler handler,
            Class<? extends RequestShape> requestShape
    ) {
        requestDispatcher.createRoute(path, HttpMethod.PUT, handler, requestShape);
    }

    /**
     * Registers a DELETE route.
     *
     * @param path route path pattern
     * @param handler handler invoked for matching requests
     */
    public void delete(String path, Handler handler) {
        requestDispatcher.createRoute(path, HttpMethod.DELETE, handler, null);
    }
}

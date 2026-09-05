package com.catcher.miniserver.server;

import com.catcher.miniserver.exception.HttpException;
import com.catcher.miniserver.exception.MethodNotAllowedException;
import com.catcher.miniserver.http.HttpParser;
import com.catcher.miniserver.http.HttpRequest;
import com.catcher.miniserver.http.HttpResponse;
import com.catcher.miniserver.http.Response;
import com.catcher.miniserver.http.ResponseWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Accepts TCP connections and dispatches each HTTP request to a bounded
 * worker pool. Each connection handles one request and is then closed.
 */
final class ServerEngine {
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 60;
    private final int port;
    private final RequestDispatcher requestDispatcher;
    private final ResponseWriter responseWriter = new ResponseWriter();
    private final ExecutorService executor;
    private volatile boolean running;
    private volatile ServerSocket serverSocket;

    ServerEngine(int port, RequestDispatcher requestDispatcher) {
        this.port = port;
        this.requestDispatcher = requestDispatcher;
        this.executor = buildExecutor();
    }

    /**
     * Opens the server socket and dispatches accepted connections to the worker
     * pool. This method blocks the calling thread until the server is stopped
     * or the socket fails.
     */
    void start() {
        running = true;

        try (ServerSocket socket = new ServerSocket(port)) {
            serverSocket = socket;
            System.out.println("Server listening on port " + port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                dispatchClient(clientSocket);
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        } finally {
            running = false;
            serverSocket = null;
        }
    }

    void dispatchClient(Socket clientSocket) throws IOException {
        try {
            executor.execute(() -> handleClient(clientSocket));
        } catch (RejectedExecutionException exception) {
            // No worker owns this connection when submission is rejected.
            clientSocket.close();
        }
    }

    /**
     * Stops accepting connections and allows active tasks to finish within the
     * configured shutdown timeout before interrupting them.
     */
    void stop() {
        running = false;
        closeServerSocket();

        executor.shutdown();

        try {
            if (!executor.awaitTermination(
                    SHUTDOWN_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            )) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void closeServerSocket() {
        ServerSocket socket = serverSocket;

        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException exception) {
                System.err.println(
                        "Failed to close server socket: " + exception.getMessage()
                );
            }
        }
    }

    /**
     * Parses and dispatches one request, writes its response, and then closes
     * the client connection.
     *
     * @param clientSocket connected socket from which the request is read
     */
    private void handleClient(Socket clientSocket) {
        try (clientSocket) {
            System.out.println(
                    "Client connected: " + clientSocket.getRemoteSocketAddress()
            );

            HttpResponse response;

            try {
                HttpParser parser =
                        new HttpParser(clientSocket.getInputStream());

                HttpRequest parsedRequest = parser.parse();
                response = requestDispatcher.handleRequest(parsedRequest);
            } catch (HttpException e) {
                e.printStackTrace();
                response = createErrorResponse(e);
            } catch (RuntimeException e) {
                System.err.println("Unhandled request error: " + e.getMessage());
                e.printStackTrace();
                response = Response.internalServerError("Internal Server Error");
            }

            byte[] byteResponse = responseWriter.write(response);
            OutputStream outputStream = clientSocket.getOutputStream();
            outputStream.write(byteResponse);
            outputStream.flush();

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
}

    private ExecutorService buildExecutor() {
        int corePoolSize = 5;
        int maxPoolSize = 10;
        long keepAliveTime = 60L;

        int queueCapacity = 100;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(queueCapacity);

        RejectedExecutionHandler rejectionHandler = (task, pool) -> {
            if (pool.isShutdown()) {
                throw new RejectedExecutionException("Server is shutting down");
            }
            // Preserve caller-runs backpressure when the pool is full.
            task.run();
        };

        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                workQueue,
                rejectionHandler
        );
    }

    private HttpResponse createErrorResponse(HttpException exception) {
        HttpResponse response = Response.status(
                exception.statusCode(),
                exception.getMessage()
        );

        if (exception instanceof MethodNotAllowedException methodNotAllowed) {
            String allowedMethods = methodNotAllowed.allowedMethods().stream()
                    .map(Enum::name)
                    .sorted()
                    .collect(Collectors.joining(", "));
            response.headers().put("Allow", allowedMethods);
        }

        return response;
    }
}

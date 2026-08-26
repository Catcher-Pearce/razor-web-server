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
import java.util.stream.Collectors;

final class ServerEngine {
    private final int port;
    private final RequestDispatcher requestDispatcher;
    private final ResponseWriter responseWriter = new ResponseWriter();

    ServerEngine(int port, RequestDispatcher requestDispatcher) {
        this.port = port;
        this.requestDispatcher = requestDispatcher;
    }

    void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleClient(clientSocket);
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

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

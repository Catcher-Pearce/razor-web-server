package com.catcher.miniserver.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerEngineTest {
    @Test
    void closesEmptyConnectionWithoutWritingAnErrorResponse() throws Exception {
        assertEquals("", responseFor(""));
    }

    @Test
    void rejectsPartiallySentHeaders() throws Exception {
        assertTrue(responseFor("GET / HTTP/1.1\r\n").startsWith("HTTP/1.1 400 "));
    }

    @Test
    void preservesFirstByteOfCompleteRequest() throws Exception {
        assertTrue(responseFor("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n")
                .startsWith("HTTP/1.1 404 "));
    }

    private String responseFor(String request) throws Exception {
        ServerEngine engine = new ServerEngine(0, new RequestDispatcher());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CountDownLatch closed = new CountDownLatch(1);
        Socket socket = new Socket() {
            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public OutputStream getOutputStream() {
                return output;
            }

            @Override
            public void close() {
                closed.countDown();
            }
        };
        try {
            engine.dispatchClient(socket);
            assertTrue(closed.await(5, TimeUnit.SECONDS), "Connection must close");
            return output.toString(StandardCharsets.UTF_8);
        } finally {
            engine.stop();
        }
    }

    @Test
    void closesAcceptedSocketWhenShutdownHappensBeforeDispatch() throws IOException {
        ServerEngine engine = new ServerEngine(0, new RequestDispatcher());

        try (Socket clientSocket = new Socket()) {
            // Reproduce the ordering: accept, shutdown, then task submission.
            engine.stop();
            engine.dispatchClient(clientSocket);

            assertTrue(clientSocket.isClosed(), "Rejected connections must be closed");
        }
    }
}

package com.catcher.miniserver.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerEngineTest {
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

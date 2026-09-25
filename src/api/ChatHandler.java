package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Clock;
import models.Message;
import sync.Election;
import sync.MutualExclusion;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChatHandler implements HttpHandler {

    private final Clock clock;
    private final MutualExclusion mutex; // may be null until B finishes
    private final Election election;
    private final List<Message> messageLog = new ArrayList<>();

    public ChatHandler(Clock clock, MutualExclusion mutex, Election election) {
        this.clock = clock;
        this.mutex = mutex;
        this.election = election;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("POST".equals(method) && "/api/chat".equals(path)) {
                String body = new String(exchange.getRequestBody().readAllBytes());

                int senderId = extractInt(body, "sender_id");
                String text = extractString(body, "text");
                int lamport = extractInt(body, "lamport");
                int[] vector = extractIntArray(body, "vector");

                clock.updateOnReceive(lamport, vector);
                messageLog.add(new Message(senderId, text, clock.getLamportTime(), clock.getVectorClock()));
                messageLog.sort(Comparator.comparingInt(Message::getLamportTime));

                sendResponse(exchange, 200, "{\"status\":\"Message Received\"}");

            } else if ("POST".equals(method) && "/api/token".equals(path)) {
                if (mutex == null) {
                    sendResponse(exchange, 501, "{\"error\":\"Mutex not implemented yet\"}");
                } else {
                    mutex.receiveToken();
                    sendResponse(exchange, 200, "{\"status\":\"Token Handled\"}");
                }

            } else if ("POST".equals(method) && "/api/election".equals(path)) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                String type = extractString(body, "type");
                int senderId = extractInt(body, "sender_id");

                switch (type) {
                    case "ELECTION" -> election.handleElectionMessage(senderId);
                    case "OK" -> election.handleOkMessage();
                    case "COORDINATOR" -> election.handleCoordinatorMessage(senderId);
                }
                sendResponse(exchange, 200, "{\"status\":\"OK\"}");

            } else if ("POST".equals(method) && "/api/score".equals(path)) {
                if (mutex == null) {
                    sendResponse(exchange, 501, "{\"error\":\"Mutex not implemented yet\"}");
                } else {
                    mutex.requestCriticalSection();
                    sendResponse(exchange, 200, "{\"status\":\"Score update requested\"}");
                }

            } else if ("GET".equals(method) && "/api/scoreboard".equals(path)) {
                if (mutex == null) {
                    sendResponse(exchange, 501, "{\"error\":\"Mutex not implemented yet\"}");
                } else {
                    sendResponse(exchange, 200, mutex.getScoreboard().toString());
                }

            } else if ("GET".equals(method) && "/api/health".equals(path)) {
                sendResponse(exchange, 200, "{\"status\":\"ALIVE\"}");

            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    // --- Minimal hand-rolled JSON extraction (no external libraries allowed) ---

    private String extractString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        var m = java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private int extractInt(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(-?\\d+)";
        var m = java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private int[] extractIntArray(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\\[([^\\]]*)\\]";
        var m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (!m.find()) return new int[0];
        String[] parts = m.group(1).split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i].trim());
        }
        return result;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes();
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
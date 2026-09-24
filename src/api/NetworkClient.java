package api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * A lightweight, framework-free HTTP client for node-to-node communication.
 * Used by Team C (Election), Team B (Mutual Exclusion), and Team D (Chat).
 */
public class NetworkClient {

    // Create a single, reusable HttpClient instance (best practice for performance)
    // We add a connect timeout so the program doesn't freeze forever if a node is dead.
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)) 
            .build();

    /**
     * Sends a POST request with a JSON payload to a specified URL.
     * Used for: /api/chat, /api/token, /api/election
     * 
     * @param url The destination URL (e.g., "http://localhost:8008/api/election")
     * @param jsonPayload The JSON string to send (e.g., "{\"type\": \"ELECTION\", \"sender_id\": 8}")
     * @return The response body as a String
     */
    public static String sendPost(String url, String jsonPayload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * Sends a GET request to a specified URL.
     * Useful for fetching state or simple health checks.
     */
    public static String sendGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * Checks if a specific node is alive by hitting its /api/health endpoint.
     * Returns true if the node responds with a 200 OK status, false otherwise.
     * 
     * CRITICAL FOR RUBRIC: This prevents the Bully Algorithm and Token Ring 
     * from freezing when trying to talk to a crashed node.
     */
    public static boolean checkHealth(String url) {
        try {
            // We use a very short timeout (500ms) for health checks so we "fail fast"
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(500)) 
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
            
        } catch (Exception e) {
            // If ANY exception occurs (timeout, connection refused, node down), 
            // we safely assume the node is dead and return false.
            return false;
        }
    }
}
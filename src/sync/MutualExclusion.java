package sync;

import api.NetworkClient;
import java.util.List;

/**
 * Team B (now D): Distributed Mutual Exclusion using a Token Ring.
 * The token circulates node -> node in a fixed logical ring.
 * Whoever holds the token may safely update the shared scoreboard.
 */
public class MutualExclusion {

    private final int nodeId;
    private final List<Integer> ringOrder; // full ring order, e.g. [0,1,2,...,9]
    private boolean wantsToUpdateScore = false;
    private boolean hasToken;

    // Simple shared scoreboard state — replace/extend as needed
    private final java.util.Map<String, Integer> scoreboard = new java.util.concurrent.ConcurrentHashMap<>();

    public MutualExclusion(int nodeId, List<Integer> ringOrder, boolean startsWithToken) {
        this.nodeId = nodeId;
        this.ringOrder = ringOrder;
        this.hasToken = startsWithToken;
    }

    /** Called externally when this node wants to update the scoreboard. */
    public synchronized void requestCriticalSection() {
        this.wantsToUpdateScore = true;
        System.out.println("[Node " + nodeId + "] Requested critical section access.");
    }

    /** Called when this node receives the token over HTTP. */
    public synchronized void receiveToken() {
        hasToken = true;
        System.out.println("[Node " + nodeId + "] Token received.");

        if (wantsToUpdateScore) {
            enterCriticalSection();
            wantsToUpdateScore = false;
        }

        passToken();
    }

    /** The actual protected operation — only ever runs while holding the token. */
    private void enterCriticalSection() {
        System.out.println("[Node " + nodeId + "] >>> ENTERING critical section.");
        scoreboard.merge("node" + nodeId, 1, Integer::sum); // example scoreboard update
        System.out.println("[Node " + nodeId + "] Scoreboard now: " + scoreboard);
        System.out.println("[Node " + nodeId + "] <<< LEAVING critical section.");
    }

    /** Forwards the token to the next alive peer in the ring, skipping dead ones. */
    private void passToken() {
        int idx = ringOrder.indexOf(nodeId);
        int n = ringOrder.size();

        for (int attempt = 1; attempt <= n; attempt++) {
            int nextId = ringOrder.get((idx + attempt) % n);
            if (nextId == nodeId) break; // full loop back to self, nobody else alive

            String nextUrl = "http://localhost:" + (8000 + nextId);
            boolean alive = NetworkClient.checkHealth(nextUrl + "/api/health");

            if (alive) {
                String payload = "{\"token_holder\":" + nodeId + "}";
                try {
                    hasToken = false; // give it up before sending, avoids double-holding on retry
                    NetworkClient.sendPost(nextUrl + "/api/token", payload);
                    System.out.println("[Node " + nodeId + "] Token passed to Node " + nextId);
                    return;
                } catch (Exception e) {
                    System.out.println("[Node " + nodeId + "] Failed sending token to Node " + nextId + ", trying next.");
                }
            } else {
                System.out.println("[Node " + nodeId + "] Node " + nextId + " is dead, skipping.");
            }
        }

        // If we get here, no other node is reachable — keep the token ourselves.
        hasToken = true;
        System.out.println("[Node " + nodeId + "] No reachable peers — retaining token.");
    }
}
package sync;

import api.NetworkClient;
import java.util.List;

/**
 * Team C: Leader Election using the Bully Algorithm.
 * Rule: The node with the highest ID becomes the leader.
 */
public class Election {
    
    private int nodeId;
    private int currentLeaderId;
    private boolean isElectionInProgress = false;
    private boolean receivedOk = false;
    
    // Peer information needed to send messages
    private List<Integer> higherPeerIds;     // IDs of nodes strictly greater than this nodeId
    private List<String> higherPeerUrls;     // Corresponding URLs for those higher nodes
    private List<String> allPeerUrls;        // URLs of ALL nodes (used to broadcast the new leader)

    /**
     * Constructor to initialize the Election manager.
     * Team D will call this when starting the Node.
     */
    public Election(int nodeId, int initialLeaderId, List<Integer> higherPeerIds, 
                    List<String> higherPeerUrls, List<String> allPeerUrls) {
        this.nodeId = nodeId;
        this.currentLeaderId = initialLeaderId;
        this.higherPeerIds = higherPeerIds;
        this.higherPeerUrls = higherPeerUrls;
        this.allPeerUrls = allPeerUrls;
    }

    /**
     * 1. Initiates the Bully Election process.
     * Sends an ELECTION message to all nodes with a HIGHER ID.
     */
    public synchronized void startElection() {
        if (isElectionInProgress) return; // Prevent duplicate concurrent elections
        
        isElectionInProgress = true;
        receivedOk = false;
        System.out.println("[Node " + nodeId + "] Starting election... Contacting higher nodes.");

        // Send ELECTION message to all higher nodes asynchronously
        for (int i = 0; i < higherPeerIds.size(); i++) {
            String url = higherPeerUrls.get(i) + "/api/election";
            String payload = "{\"type\": \"ELECTION\", \"sender_id\": " + nodeId + "}";
            
            // Run in a new thread so we don't block the main application
            new Thread(() -> {
                try {
                    NetworkClient.sendPost(url, payload);
                } catch (Exception e) {
                    // Expected if the higher node is dead/crashed. We silently ignore.
                }
            }).start();
        }

        // Wait for an OK response from a higher node (Timeout: 500ms)
        try {
            wait(500); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // If the timeout expires and NO higher node replied with an OK...
        if (!receivedOk) {
            currentLeaderId = nodeId;
            System.out.println("[Node " + nodeId + "] No OK received. Declaring self as NEW LEADER.");
            broadcastCoordinator();
        }
        
        isElectionInProgress = false;
    }

    /**
     * 2. Handles an incoming ELECTION message from a node with a LOWER ID.
     */
    public synchronized void handleElectionMessage(int senderId) {
        if (senderId < nodeId) {
            System.out.println("[Node " + nodeId + "] Received ELECTION from lower node " + senderId + ". Sending OK.");
            
            // Send OK back to the sender
            // Note: Adjust the port logic (8000 + senderId) if your lab uses a different port mapping
            String senderUrl = "http://localhost:" + (8000 + senderId) + "/api/election"; 
            String okPayload = "{\"type\": \"OK\", \"sender_id\": " + nodeId + "}";
            
            new Thread(() -> {
                try { NetworkClient.sendPost(senderUrl, okPayload); } catch (Exception e) {}
            }).start();

            // Take over the election process since we are a higher node
            if (!isElectionInProgress) {
                startElection();
            }
        }
    }

    /**
     * 3. Handles an incoming OK message from a higher node.
     */
    public synchronized void handleOkMessage() {
        receivedOk = true;
        notifyAll(); // Wake up the waiting startElection() thread so it knows to stop
    }

    /**
     * 4. Handles an incoming COORDINATOR message (announcing the new leader).
     */
    public synchronized void handleCoordinatorMessage(int newLeaderId) {
        currentLeaderId = newLeaderId;
        isElectionInProgress = false;
        System.out.println("[Node " + nodeId + "] Acknowledges Node " + currentLeaderId + " as the new LEADER.");
        notifyAll();
    }

    /**
     * Helper: Broadcasts the new leader (COORDINATOR) to all other nodes.
     */
    private void broadcastCoordinator() {
        String payload = "{\"type\": \"COORDINATOR\", \"sender_id\": " + nodeId + "}";
        for (String url : allPeerUrls) {
            // Don't send the message to ourselves
            if (!url.contains(":" + (8000 + nodeId))) { 
                new Thread(() -> {
                    try { NetworkClient.sendPost(url + "/api/election", payload); } catch (Exception e) {}
                }).start();
            }
        }
    }

    /**
     * Getter for the current leader ID.
     */
    public int getCurrentLeaderId() {
        return currentLeaderId;
    }
}
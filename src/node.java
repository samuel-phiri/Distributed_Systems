import api.ChatHandler;
import api.NetworkClient;
import models.Clock;
import sync.Election;
import sync.MutualExclusion;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Node {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java Node <nodeId> <port>");
            System.exit(1);
        }

        int nodeId = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);

        List<Integer> peerIds = Arrays.asList(0,1,2,3,4,5,6,7,8,9);
        int totalNodes = peerIds.size();
        int initialLeaderId = totalNodes - 1;

        List<Integer> higherPeerIds = new ArrayList<>();
        List<String> higherPeerUrls = new ArrayList<>();
        List<String> allPeerUrls = new ArrayList<>();

        for (int id : peerIds) {
            String url = "http://localhost:" + (8000 + id);
            allPeerUrls.add(url);
            if (id > nodeId) {
                higherPeerIds.add(id);
                higherPeerUrls.add(url);
            }
        }

        Clock clock = new Clock(nodeId, totalNodes);
        Election election = new Election(nodeId, initialLeaderId, higherPeerIds, higherPeerUrls, allPeerUrls);

        // TODO: replace with real MutualExclusion once B finishes it
        MutualExclusion mutex = new MutualExclusion(nodeId, peerIds, nodeId == 0);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", new ChatHandler(clock, mutex, election));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Node " + nodeId + " running on port " + port);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            int leaderId = election.getCurrentLeaderId();
            if (leaderId == nodeId) return;
            String healthUrl = "http://localhost:" + (8000 + leaderId) + "/api/health";
            boolean alive = NetworkClient.checkHealth(healthUrl);
            if (!alive) {
                System.out.println("[Node " + nodeId + "] Leader " + leaderId + " unreachable — starting election.");
                new Thread(election::startElection).start();
            }
        }, 3, 3, TimeUnit.SECONDS);
    }
}
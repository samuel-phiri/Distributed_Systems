package models;

import java.util.Arrays;

public class Clock {

    private int lamportTime = 0;
    private final int[] vectorClock;
    private final int nodeId;

    public Clock(int nodeId, int totalNodes) {
        this.nodeId = nodeId;
        this.vectorClock = new int[totalNodes];
    }

    // Local event tick
    public synchronized void tick() {
        lamportTime++;
        vectorClock[nodeId]++;
    }

    // Update clocks when receiving a message
    public synchronized void updateOnReceive(
            int incomingLamport,
            int[] incomingVector) {

        // Lamport clock update
        lamportTime =
                Math.max(lamportTime, incomingLamport) + 1;

        // Vector clock update
        for (int i = 0; i < vectorClock.length; i++) {
            vectorClock[i] =
                    Math.max(vectorClock[i], incomingVector[i]);
        }

        // Increment this node's position
        vectorClock[nodeId]++;
    }

    public synchronized int getLamportTime() {
        return lamportTime;
    }

    public synchronized int[] getVectorClock() {
        return Arrays.copyOf(vectorClock, vectorClock.length);
    }
}
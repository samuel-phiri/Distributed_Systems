package models;

import java.util.Arrays;

/**
 * Represents a single chat message with its logical timestamps.
 */
public class Message {
    private final int senderId;
    private final String text;
    private final int lamportTime;
    private final int[] vectorClock;

    public Message(int senderId, String text, int lamportTime, int[] vectorClock) {
        this.senderId = senderId;
        this.text = text;
        this.lamportTime = lamportTime;
        this.vectorClock = Arrays.copyOf(vectorClock, vectorClock.length);
    }

    public int getSenderId() { return senderId; }
    public String getText() { return text; }
    public int getLamportTime() { return lamportTime; }
    public int[] getVectorClock() { return Arrays.copyOf(vectorClock, vectorClock.length); }

    @Override
    public String toString() {
        return "[L=" + lamportTime + ", V=" + Arrays.toString(vectorClock) + "] Node " + senderId + ": " + text;
    }
}
import models.Clock;
import java.util.Arrays;

public class ClockTest {

    public static void main(String[] args) {

        // Create a clock for Node 0
        // Total number of nodes = 3
        Clock clock = new Clock(0, 3);

        System.out.println("=== INITIAL CLOCK ===");

        System.out.println(
                "Lamport: " + clock.getLamportTime()
        );

        System.out.println(
                "Vector: " +
                Arrays.toString(clock.getVectorClock())
        );

        // Local event
        clock.tick();

        System.out.println("\n=== AFTER LOCAL EVENT ===");

        System.out.println(
                "Lamport: " + clock.getLamportTime()
        );

        System.out.println(
                "Vector: " +
                Arrays.toString(clock.getVectorClock())
        );

        // Simulate receiving a message
        int incomingLamport = 5;
        int[] incomingVector = {2, 4, 1};

        clock.updateOnReceive(
                incomingLamport,
                incomingVector
        );

        System.out.println(
                "\n=== AFTER RECEIVING MESSAGE ==="
        );

        System.out.println(
                "Lamport: " + clock.getLamportTime()
        );

        System.out.println(
                "Vector: " +
                Arrays.toString(clock.getVectorClock())
        );
    }
}
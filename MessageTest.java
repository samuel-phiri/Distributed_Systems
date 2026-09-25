import models.Message;
import java.util.Arrays;

public class MessageTest {

    public static void main(String[] args) {

        int[] vector = {1, 4, 2};

        Message message = new Message(
                1,
                "Hello",
                4,
                vector
        );

        System.out.println("=== MESSAGE ===");

        System.out.println(
                "Sender ID: " + message.getSenderId()
        );

        System.out.println(
                "Text: " + message.getText()
        );

        System.out.println(
                "Lamport: " + message.getLamportTime()
        );

        System.out.println(
                "Vector: " +
                Arrays.toString(message.getVectorClock())
        );

        System.out.println("\n" + message);
    }
}
import network.ReldatSocket;

import java.io.IOException;

public class Server {
    public static void main(String[] args) {
        int port = 0,
            windowSize = 0;

        // Parse inputs
        try {
            port = Integer.parseInt(args[0]);
            windowSize = Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.err.println("USAGE: ./reldat-server.sh PORT WINDOW_SIZE");
            System.exit(1);
        }

        // Create a new socket and listen on the port
        ReldatSocket sock = null;
        try {
            sock = new ReldatSocket(port, windowSize);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        System.out.printf("Window size set to %d\n", windowSize);
        System.out.printf("Server listening on %s\n", sock.getLocalSocketAddress());

        System.out.println();

        // Infinite loop for accepting and handling new connections
        while (true) {
            ReldatSocket conn = sock.accept();
            System.out.printf("Connection accepted from %s\n", conn.getRemoteSocketAddress());
        }
    }
}
import network.ReldatSocket;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.HashMap;

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
        String receivedBytes=null;
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
            while (conn.isConnected()) {
                try {
                    receivedBytes = new String(conn.receive(windowSize), "UTF-8");
                    conn.send(processMessage(receivedBytes).getBytes());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Connection Closed");
            System.out.printf("Server listening on %s\n", sock.getLocalSocketAddress());

            System.out.println();
        }
    }

    private static String processMessage(String message) {
        if (message != null) {
            String result = message.toUpperCase();
            return result;
        }
        return "ERR";
    }
}
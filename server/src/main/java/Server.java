import cs3251.ReldatSock;

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

        try {
            ReldatSock sock = new ReldatSock(port, windowSize);
            System.out.println(sock);
        } catch (IOException e) {
            System.err.println(e);
            System.exit(1);
        }
    }
}
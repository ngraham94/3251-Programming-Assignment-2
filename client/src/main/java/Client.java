import network.ReldatSocket;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class Client {
    public static void main(String[] args) {
        // Parse input args
        SocketAddress address = null;
        int windowSize = 0;
        try {
            String[] hostPort = args[0].split(":");
            address = new InetSocketAddress(hostPort[0], Integer.parseInt(hostPort[1]));
            windowSize = Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.err.println("USAGE: ./reldat-client.sh HOST:PORT WINDOW_SIZE");
            System.exit(1);
        }

        // Create a new socket and establish a connection
        ReldatSocket sock = null;
        try {
            sock = new ReldatSocket(windowSize);
            sock.connect(address);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}

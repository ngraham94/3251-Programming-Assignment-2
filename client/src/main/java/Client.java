import network.DisconnectException;
import network.ReldatSocket;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

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
            System.out.printf("Connected to %s\n", sock.getRemoteSocketAddress());
            System.out.printf("NOTE: the connection will automatically be dropped after "
                    + "%d seconds if no data is transferred\n", ReldatSocket.CONNECT_TIMEOUT / 1000);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        Scanner scanner = new Scanner(System.in);
        while (sock.isConnected()) {
            System.out.print("Command: ");
            String command = scanner.nextLine();

            if (command.equalsIgnoreCase("disconnect")) {
                sock.close();
                System.out.println("Connection disconnected");
            } else if (command.matches("^transform\\s.+$")) {
                String[] split = command.split("\\s+");
                String filename = split[1];

                byte[] data = null;
                byte[] length = null;
                try {
                    data = Files.readAllBytes(Paths.get(filename));
                    // Calculate length of data
                    length = ByteBuffer.allocate(4).putInt(data.length).array();
                } catch (IOException e) {
                    System.err.printf("Cannot read file %s\n", e.getMessage());
                    continue;
                }

                try {
                    // Send data
                    sock.send(length);
                    sock.send(data);

                    // Receive response
                    byte[] lengthBytes = sock.receive(4);
                    int resLength = ByteBuffer.wrap(lengthBytes).getInt();
                    byte[] response = sock.receive(resLength);

                    // Write response to file
                    String out = getNewFilename(filename);
                    System.out.println("Writing response to " + out);
                    try (FileOutputStream stream = new FileOutputStream(out)) {
                        stream.write(response);
                    }
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                } catch (DisconnectException e) {
                    System.out.println("Connection closed by server since it was idle");
                }

            } else {
                System.out.println("Invalid command");
            }
        }
    }

    private static String getNewFilename(String file) {
        final String suffix = "-received";

        int i = file.lastIndexOf(".");

        // Original name does not have extension
        if (i == -1) return file + suffix;

        return file.substring(0, i) + suffix + file.substring(i);
    }
}

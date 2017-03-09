package network;
import java.net.*;



/**
 * Created by Nick on 2/8/2017.
 */
public class smsclientUDP {
    public static void main(String args[]) throws Exception {

        int tries = 0;
        boolean received = false;
        if (args.length < 3 || args.length >=4) {//-------Check for the correct number of arguments
            System.out.println("Invalid number of arguments. Accepted arguments in order are\nAddress\nPort\nMessage File");
            return;
        }
        CommonMethods methods = new CommonMethods();//-------Import methods used by clients
        String textMessageFile = args[2];
        methods.readTextMessage(textMessageFile);//-------Read the text message file
        String textMessage = methods.getFullText();
        int port = Integer.parseInt(args[1]);
        InetAddress address = null;
        try {//--------Try to resolve the IP address. If fails, return
            address = InetAddress.getByName(args[0]);
        } catch (Exception e){
            System.out.println("Invalid hostname. Terminating.");

        }
        //<-----------------------Begin Connection Block 1----------------------->

        DatagramSocket socket = new DatagramSocket();
        byte[] buffer = new byte[2000];
        buffer = textMessage.getBytes();//-------Convert the text message to a byte array
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(packet);//-------Attempt to send the packet
        while (tries < 3 && !received) {//-------Wait for response
            try {
                socket.setSoTimeout(1500);//-------Set timeout for response
                socket.receive(packet);
                received = true;
            } catch (SocketTimeoutException e) {//-------After timeout, increment attempt counter and reset timeout.
                System.out.println("Timeout reached. Attempt " + (tries + 1) + " of 3. Retrying...");
                socket.send(packet);
                System.out.println("Packet Sent");
                tries++;
                received = false;
            }
        }
        if (tries == 3) {//-------After 3 attempts, terminate the program, otherwise continue
            System.out.println("Could not communicate with server. Failed 3 attempts to send message. Terminating program");
            return;
        }
        //<-----------------------End Connection Block 1----------------------->
        //<-----------------------Begin Connection Block 2----------------------->
        System.out.println(new String(buffer, 0, packet.getLength()));//-------Display the response and close the socket.
        socket.close();
        //<-----------------------End Connection Block 2----------------------->
    }
}

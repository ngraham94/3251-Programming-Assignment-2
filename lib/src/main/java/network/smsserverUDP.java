package network;
import java.net.*;


/**
 * Created by Nick on 2/8/2017.
 */
public class smsserverUDP {
    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length >=3) {
            System.out.println("Invalid number of arguments. Accepted arguments in order are\nAddress\nPort\nMessage File");
            return;
        }
        int portnum = Integer.parseInt(args[0]); //Parse the user supplied port number
        String spamWordsFile = args[1]; //args[1] is the filename of the spam words
        CommonMethods common = new CommonMethods();
        int flag = common.readWordFile(spamWordsFile);//Read the user supplied text file of spam words
        if (flag != 1) {//The file was not read successfully or was not found.
            return;
        }
        String textMessage = "";
        DatagramSocket socket;
        try {//-------Try to bind to the supplied port. If in use, return
            socket = new DatagramSocket(portnum);
        } catch (BindException e) {
            System.out.println("Specified port number is already in use. Terminating program");
            return;
        }
        byte[] buffer = new byte[2000];//-------Buffer to be used by incoming packet
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while(true) {
            //<-----------------------Begin Connection Block 1----------------------->
            socket.receive(packet);//--------Receive the packet from the client
            //<-----------------------End Connection Block 1----------------------->
            textMessage = new String(packet.getData(),0, packet.getLength());
            textMessage += "\n";
            String reply = common.processMessage(textMessage);
            buffer = reply.getBytes();
            System.out.println("Response: " + reply);
            //<-----------------------Begin Connection Block 2----------------------->
            InetAddress outAddress = packet.getAddress();//-------Get address of client
            int outPort = packet.getPort();//-------get port of client
            packet = new DatagramPacket(buffer, buffer.length, outAddress, outPort);
            socket.send(packet);//-------Send the response
            buffer = new byte[2000];
            packet = new DatagramPacket(buffer, buffer.length);//-------Clear the buffer for another client request
            //<-----------------------End Connection Block 2----------------------->
        }
    }
}

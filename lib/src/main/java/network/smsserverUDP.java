package network;
import java.net.*;
import java.util.Scanner;


/**
 * Created by Nick on 2/8/2017.
 */
public class smsserverUDP {
    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length >=3) {                                                                       //Too few Arguments
            System.out.println("Invalid number of arguments. Accepted arguments in order are\nPort\nMax Window Size");
            return;
        }

        //-----------------------------------Initialize variables-------------------------------------------------------
        boolean received = false;                                                                                       //boolean for successful receipt of connection request
        int tries = 0;                                                                                                  //variable for number of attempts to establish connection
        CommonMethods common = new CommonMethods();                                                                     //Contains methods shared between client and server
        boolean connected = false;                                                                                      //Boolean stating if connection was established
        byte[] incomingBuffer = new byte[Integer.parseInt(args[1])];                                                    //Buffer for incoming message at max  window size
        byte[] outgoingBuffer;                                                                                          //Buffer for outgoing message
        int portnum = Integer.parseInt(args[0]);                                                                        //Parse the user supplied port number
        //--------------------------------------------------------------------------------------------------------------
        // Begin listening on specified port
        DatagramSocket socket;
        try {//-------Try to bind to the supplied port. If in use, return
            socket = new DatagramSocket(portnum);
        } catch (BindException e) {
            System.out.println("Specified port number is already in use. Terminating program");
            return;
        }
        DatagramPacket packet = new DatagramPacket(incomingBuffer, incomingBuffer.length);
        while(true) {
            //<-----------------------Listen for connection request-----------------------------------------------------

            socket.receive(packet);                                                                                     //--------Receive the packet from the client

            //--------------------------------Begin block for connection establishment----------------------------------


            //----------------------------------------------------------------------------------------------------------
            //-------------------------------Text Transform Communication Block-----------------------------------------


            //----------------------------------------------------------------------------------------------------------
            //--------------------------------Close connection block----------------------------------------------------


            //----------------------------------------------------------------------------------------------------------
        }
    }
}

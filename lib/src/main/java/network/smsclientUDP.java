package network;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;


/**
 * Created by Nick on 2/8/2017.
 */
public class smsclientUDP {
    private static DatagramSocket clientSocket;

    public static void main(String args[]) throws Exception {
        //-----------------------------------Initialize variables-------------------------------------------------------
        boolean received = false;                                                                                       //boolean for successful receipt of connection request
        int tries = 0;                                                                                                  //variable for number of attempts to establish connection
        Scanner userInput = new Scanner(System.in);                                                                     //Scanner to read command passed in by user
        String command, keyword;                                                                                        //
        String[] commandWords;                                                                                          //List to hold words from command after splitting
        String filename;                                                                                                //Filename containing message to be transformed
        CommonMethods common = new CommonMethods();                                                                     //Contains methods shared between client and server
        boolean connected = false;                                                                                      //Boolean stating if connection was established
        byte[] incomingBuffer;                                                                                          //Buffer for incoming message
        byte[] outgoingBuffer;                                                                                          //Buffer for outgoing message
        //--------------------------------------------------------------------------------------------------------------
        if (args.length < 2 || args.length >=3) {                                                                       //Too few arguments
            System.out.println("Invalid number of arguments. Accepted arguments in order are\nAddress:Port\n" +
                    "Max Window Size");
            return;
        }
        String[] ipAndPort = args[0].split(":");                                                                        //Split ip address and portnumber into strings
        InetAddress destAddress = InetAddress.getByName(ipAndPort[0]);                                                  //Parse ip address given by user
        int portnum = Integer.parseInt(ipAndPort[2]);                                                                   //Parse the user supplied port number, the second index in ip and port array
        int maxWindow = Integer.parseInt(args[1]);                                                                      //Parse the maximum window size;
        incomingBuffer = new byte[maxWindow];                                                                           //initialize the incoming buffer
        //<-----------------------------------------Establish Connection Here-------------------------------------------

        connected = reldatProtocolHandshake(maxWindow, destAddress, portnum, incomingBuffer);

        //<-------------------------------------------------------------------------------------------------------------
        while (connected) {
            command = userInput.nextLine();
            commandWords = command.split(" ");
            commandWords[1] = commandWords[1].toLowerCase();
            keyword = commandWords[1];
            if (keyword.equals("transform")) {
                filename = commandWords[2];                                                                             //commandWords[2] is the filename of the message to transform
                //-----------------------------Insert Text File Parse and Send Code here--------------------------------

                //------------------------------------------------------------------------------------------------------
            } else if (keyword.equals("disconnect")) {
                //-------------------------------------Insert Disconnect Code here--------------------------------------

                //------------------------------------------------------------------------------------------------------
                clientSocket.close();
                return;
            } else {
                System.out.println("Unrecognized command.\nPossible options are 'disconnect' and 'transform F,\n" +
                        " where F is a file name.");
            }
        }
    }

    private static boolean reldatProtocolHandshake(int maxWindow, InetAddress destAddress, int portnum, byte[] byteBuffer){
        int tries = 0;
        boolean received = false;
        try {
            clientSocket = new DatagramSocket();
            //------------------------------Create connection initiation packet here------------------------------------

            /* byteBuffer = */

            //----------------------------------------------------------------------------------------------------------
            DatagramPacket packet = new DatagramPacket(byteBuffer, byteBuffer.length, destAddress, portnum);
            clientSocket.send(packet);//-------Attempt to send the packet
            while (tries < 3 && !received) {//-------Wait for response
                try {
                    clientSocket.setSoTimeout(1500);//-------Set timeout for response
                    clientSocket.receive(packet);
                    //----------------------------Parse server response here--------------------------------------------

                    //--------------------------------------------------------------------------------------------------
                    received = true;            //Update value based on acknowledgement packet from server
                } catch (SocketTimeoutException e) {//-------After timeout, increment attempt counter and reset timeout.
                    System.out.println("Timeout for connection establishment reached." +
                            " Attempt " + (tries + 1) + " of 3. Retrying...");
                    clientSocket.send(packet);
                    System.out.println("Connection establishment packet sent");
                    tries++;
                    received = false;
                }
            }
            if (tries == 3) {//-------After 3 attempts, terminate the program, otherwise continue
                System.out.println("Could not communicate with server. Failed 3 attempts to send message." +
                        " Terminating program");
                received = false;
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return received;
    }
}

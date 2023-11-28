package com.example.avr_ballbalancerapp;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

public class TCP_Client {
    //private static final String TAG = TCP_Client.class.getSimpleName();
    private String myIP_ADDRESS = "192.168.X.X"; //Mobile phone IP Address

    //private static String SERVER_IP = "192.168.10.125"; //server IP address ESP8266
    public static String SERVER_IP = "192.168.10.125"; //server IP address HERCULES
    //public static final String SERVER_IP = "192.168.10.166"; //server IP address Mobile2
    //private static final String SERVER_IP = "192.168.1.103"; //server IP Hytta Hercules
    //private static final int SERVER_PORT = 8077;
    //private static String SERVER_IP = "192.168.1.10"; //server IP Hytta Hercules
    //private static int SERVER_PORT = 8078;
    public  static int SERVER_PORT = 8078;
    public static String SERVER_PORT_STRING = "8078";

    // message to send to the server
    private String mServerMessage;
    // sends message received notifications
    private OnMessageReceived mMessageListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;
    // used to send messages
    private PrintWriter mBufferOut;
    // used to read messages from the server
    private InputStream mClientInputStream;
    private BufferedReader mBufferIn; //Nb, reads text only, not Byte Data e.g. from esp8266.



    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TCP_Client(OnMessageReceived listener) {
        getLocalIpAddress(); //Not in use for any special purpose at this point
        mMessageListener = listener;
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(final String message) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mBufferOut != null) {
                    Log.i("TCP_Client", "SendMessage: " + message);
                    mBufferOut.println(message);
                    if (mBufferOut != null) {
                        mBufferOut.flush();
                    }
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * Close the connection and release the members
     */
    public void stopClient() {

        mRun = false;
        Log.i("TCP_Client", "StopClient");
        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
        }

        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;
        mMessageListener.messageReceived("TCP_Client: stopClient called"); //Raises event - To outside activity listeners

    }

    public void run() { //Code to establish a connection from this client to the remote Server.

        mRun = true;

        try {
            //here you must put your server IP address.
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP); //Must know the Server IP address. This is also set by subrouting setServerIP_Address

            Log.i("TCP_Client", "C: Connecting to " + SERVER_IP);

            //create a socket to make the connection with the server
            Socket socket = new Socket(serverAddr, SERVER_PORT);

            try {

                //sends the message to the server
                //mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true); - Removed KHJ
                mBufferOut = new PrintWriter(socket.getOutputStream());

                //receives the message which the server sends back
                mClientInputStream = socket.getInputStream(); //Used to get exact byte-array.
                //mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                mBufferIn = new BufferedReader(new InputStreamReader(mClientInputStream));


                int charsRead = 0;
                char[] buffer = new char[2048]; //choose your buffer size if you need other than 1024
                int bytesRead = 0;
                byte[] byteBuffer = new byte[2048];

                //in this while the client listens for the messages sent by the server
                while (mRun) {

                    //Text input
                    //REMOVED 03.09.2021 charsRead = mBufferIn.read(buffer);
                    //mServerMessage = mBufferIn.readLine(); //REMOVED KHJ
                    //REMOVED 03.09.2021 mServerMessage = new String(buffer).substring(0, charsRead);

                    //Bytes
                    bytesRead =  mClientInputStream.read(byteBuffer);
                    byte[] buffer2 = new byte[bytesRead];
                    for(int g = 0; g < bytesRead; g++){
                        buffer2[g] = byteBuffer[g];
                    }

                    mServerMessage = new String(byteBuffer).substring(0, bytesRead); //ADDED 03.09.2021 - Creates string of the bytefuffer.

                    if (mServerMessage != null && mMessageListener != null) {
                        //call the method messageReceived from MyActivity class
                        Log.i("TCP_Client", "TCP_CLIENT ServerMsg: " + mServerMessage);
                        mMessageListener.messageReceived(mServerMessage); //Raises event - To outside activity listeners
                        mMessageListener.bytesReceived(buffer2); //Sends buffer as byte-array (for raw data messaging)
                        mServerMessage = null; //Resets msg string


                    }

                }

                Log.i("TCP_Client", "Server: Received Message: '" + mServerMessage + "'");

            } catch (Exception e) {
                Log.i("TCP_Client", "Exception TCP Error");
                //mMessageListener.messageReceived("TCP_CLIENT: TCP Error"); //Raises event - To outside activity listeners
                mMessageListener.errorEvent("ERROR: TCP Exception Error");
                socket.close();
            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                mMessageListener.errorEvent("SOCKET CLOSED"); //Raises event - To outside activity listeners
                socket.close();
                Log.i("TCP_Client", "TCP_CLIENT: Socket Close");
            }

        } catch (Exception e) {
            Log.i("TCP_Client", "C: Run Error");
            mMessageListener.errorEvent("ERROR: TCP RUN Error");
            //mMessageListener.messageReceived("TCP_CLIENT: Run Error"); //Raises event - To outside activity listeners
        }

    }


    //SUBROUTINES
    // GETS THE IP ADDRESS OF YOUR PHONE'S NETWORK
    public String getLocalIpAddress() {
        String inetAddr;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        inetAddr = inetAddress.getHostAddress();
                        myIP_ADDRESS = inetAddr;

                        return inetAddr;

                    }
                }
            }
        } catch (SocketException ex) {
            Log.i("Exception - ServerActivity", ex.toString());
            mMessageListener.errorEvent("Local IP Addr Error: " + ex.toString());
            return "Error getting local IP address";
            //mMessageListener.messageReceived("getLocalIPAddrass called"); //Raises event - To outside activity listeners
        }
        myIP_ADDRESS = "x.x.x.x";
        return null;
    }


    public String getServerIP_ADDRESS(){
        return  SERVER_IP;
    }
    public String getServerPORT() {
        return toString().valueOf(SERVER_PORT);
    }
    public void setServerIP_Address(String serverIP){
        SERVER_IP = serverIP;
    }
    public void setServerPORT(String value){
        SERVER_PORT_STRING = value;
        SERVER_PORT = Integer.parseInt(value);
    }

    //Declare the interface. The method messageReceived(String message) must be implemented in the Activity
    //class at on AsyncTask doInBackground
    public interface OnMessageReceived {
        public void messageReceived(String message);
        public void bytesReceived(byte[] bytes); //Used when data other than "character arrays (strings)" are sent.
        public void errorEvent(String message);
    }

}


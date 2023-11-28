package com.example.avr_ballbalancerapp;

import android.net.InetAddresses;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class TCP_ClientMain {
    /*
        TCP Client Class with thread Safe interfaces to manage Client to Server Socket Connections

        Class exposes three interfaces which are thread safe (i.e. can be used to update UI Elements etc):
        public void onServerMessageReceived(String messageFromServer);  --> String from server dispatched here
        public void onServerBytesReceived(byte[] bytesFromServer);      --> raw byte array from server dispatched here
        public void onConnectionError(String errorMsg);                 --> Any errors dispatched here

        Explanation:
        Two Thread's are maintained here, run with the following Runnables and Handlers:
            1. mTCPClientConnectionThread
                    - mTCP_ClientConnectionHandler (mTCP_ClientConnectionRunnable)
                        + Internal mTCP_ClientUI_Handler bound to UI Thread to dispatch incoming TCP Messages

            2. mTCPClientSendMessageThread
                   - mTCP_ClientSendMessageHandler (mTCP_ClientSendMessageRunnable)

     */
    //


    TCP_ClientClassInterface mTCP_ClientClassInterface; //Interface/listener to outside
    private static final String TAG = "TCP_ClientMain"; //For logcat

    //Server and client info
    private String myIP_ADDRESS = "192.168.X.X"; //Mobile phone IP Address
    private InetAddress myInetIP_ADDRESS;

    private String SERVER_IP = "192.168.10.130"; //server IP address HERCULES
    private String SERVER_PORT = "8078" ; //Port Nr.

    // message to send to the server
    private String mServerSendMessage;

    //SOCKET INFO
    // used to send messages
    private Socket mSocket;
    private PrintWriter mPrintWriter;

    private InputStream mInputStream; // used to read messages from the server
    private boolean mRun = false; // while this is true, the server will continue running
    private String mClientStringReceived; //msg received from server to client
    private BufferedReader mBufferedReader; //Nb, reads text only, not Byte Data e.g. from esp8266.

    //THREAD OBJECTS
    private TCP_ClientConnectionThread mTCP_ClientConnectionThread; //Thread for socket connection and incoming messages (Runnable TCP_ClientConnectionRunnable is sent to thread via mTCP_ClientConnectionHandler)
    private Handler mTCP_ClientConnectionHandler;
    private Handler TCP_ClientUI_Handler; //Handler bound to UI Looper (MainLooper). Used to dispatch interface messages to outside listeners (E.g. UI Updates)

    private TCP_ClientSendMessageThread mTCP_ClientSendMessageThread; //Uses TCP_ClientSendMessageRunnable to post message
    private Handler mTCP_ClientSendMessageHandler;
    private TCP_ClientSendMessageRunnable mTCP_ClientSendMessageRunnable;

    //CONSTRUCTOR
    public TCP_ClientMain(TCP_ClientClassInterface listener){
        getLocalIpAddress();
        mTCP_ClientClassInterface = listener; //This sends messages to outside class (On UI Thread)
        TCP_ClientUI_Handler = new Handler(Looper.getMainLooper()); //Binding Handler to main UI looper.

    }
    public void connect(){
        //Instantiating Threads
        mTCP_ClientConnectionThread = new TCP_ClientConnectionThread();
        mTCP_ClientConnectionThread.start(); //Starts thread with looper;
        SystemClock.sleep(50); //Short delay required to ensure looper has had time to be prepared on Thread.start

        mTCP_ClientSendMessageThread = new TCP_ClientSendMessageThread();
        mTCP_ClientSendMessageThread.start(); //Starts thread with Looper
        SystemClock.sleep(50); //Short delay required to ensure looper has had time to be prepared on Thread.start

        //Connecting to Server
        mTCP_ClientConnectionHandler = new Handler(mTCP_ClientConnectionThread.looper);
        TCP_ClientConnectionRunnable iTCP_ClientConnectRunnable = new TCP_ClientConnectionRunnable();
        mTCP_ClientConnectionHandler.post(iTCP_ClientConnectRunnable); //Connection
        SystemClock.sleep(100);
        mRun = true;
        sendStringToServer(myIP_ADDRESS + " - Connected"); //Sends string to server //TEST


    }
    public void disConnect(){

        mRun = false;
        Log.i("TCP_Client", "StopClient");
        if (mPrintWriter != null) {
            mPrintWriter.flush();
            mPrintWriter.close();
        }

        mBufferedReader = null;
        mPrintWriter = null;
        mServerSendMessage = null;
        mTCP_ClientClassInterface.onServerMessageReceived("TCP_Client: stopClient called"); //Raises event - To outside activity listeners

        mTCP_ClientSendMessageRunnable = null;
        mTCP_ClientSendMessageHandler = null;

        mTCP_ClientSendMessageThread.looper.quit();
        mTCP_ClientConnectionThread.looper.quit();




    }

    //GENERAL SETTERS -------------------------------
    public void setSERVER_IP(String server_IP) {
        this.SERVER_IP = server_IP;
    }

    public void setSERVER_PORT(String server_PORT) {
        this.SERVER_PORT = server_PORT;
    }

    //TCP ROUTINES -----------------------------------
    public void sendStringToServer(String message){
        /*
        This required conenction to have been established, and that TCP_ClientOutputThread is running with looper (new messages are posted to the thread handler via runnable)
         */
        mServerSendMessage = message;
        if (mTCP_ClientSendMessageRunnable == null)
        {
            Log.d(TAG, "sendStringToServer: INSTANTIATING Runnable and Handler");
            mTCP_ClientSendMessageRunnable = new TCP_ClientSendMessageRunnable(mServerSendMessage); //Prepares Runnable to be posted to handler
            mTCP_ClientSendMessageHandler = new Handler(mTCP_ClientSendMessageThread.looper);
            //TCP_ClientSendMessageRunnable iTCP_ClientSendMessageRunnable = new TCP_ClientSendMessageRunnable(mServerSendMessage); //Prepares Runnable to be posted to handler
        }

        Log.d(TAG, "sendStringToServer: message sent: " + mServerSendMessage);
        mTCP_ClientSendMessageRunnable.mMessage = mServerSendMessage;
        mTCP_ClientSendMessageHandler.post(mTCP_ClientSendMessageRunnable); //Sends the message via the Thread handler which posts it to the thread looper message queue
    }


    // INTERFACE ----------------------
    public interface TCP_ClientClassInterface{
        //Interface available for outside users to receive events)
        public void onServerMessageReceived(String messageFromServer);
        public void onServerBytesReceived(byte[] bytesFromServer);
        public void onConnectionError(String errorMsg);
    }
    // END INTERFACE ------------------


    //------------- THREADING INNER CLASSES ----------------------------------
    private class TCP_ClientConnectionThread extends Thread{
        /*
            Thread running to send messages to server (instantiated on every sent message)
            Requires a living "socket"
         */

        private static final String TAG = "TCP_ClientOutputThread";

        public Looper looper; //Not used here, as each message sent is sent on its own thread. Made public so that we can access the looper from outside

        @Override
        public void run() {
            Log.d(TAG, "run: Output Thread Started - Looper being prepared  ");
            Looper.prepare(); //Establishes looper bound to thread
            looper = Looper.myLooper(); //makes looper accessible to outside
            Looper.loop();
            Log.d(TAG, "run: Output Thread looper ended");

        }
    }
    //OUTPUT RUNNABLES
    private class TCP_ClientConnectionRunnable implements Runnable{
        /*
            Used to connect Socket to Server, and to maintain listener for incoming messages - Runnable injected to TCP_ClientOutputThread
         */
        private static final String TAG = "TCP_ClientConnectionRunnable";


        @Override
        public void run() {

            Log.d(TAG, "C: Connecting to " + SERVER_IP);
            mRun = true;

            //create a socket to make the connection with the server
            try {
                mSocket = new Socket(SERVER_IP, Integer.parseInt(SERVER_PORT));

                //----------------------------
                try
                {
                    mPrintWriter = new PrintWriter(mSocket.getOutputStream()); //OUTPUT

                    mInputStream = mSocket.getInputStream();
                    mBufferedReader = new BufferedReader(new InputStreamReader(mInputStream));
                    Log.d(TAG, "Connected To Server: " + SERVER_IP);

                    int charsRead = 0;
                    char[] buffer = new char[2048]; //choose your buffer size if you need other than 1024
                    int bytesRead = 0;
                    byte[] byteBuffer = new byte[2048];

                    //in this while the client listens for the messages sent by the server
                    while (mRun) {
                        Log.d(TAG, "mRun Entered:");
                        //Getting Raw Bytes
                        bytesRead =  mInputStream.read(byteBuffer);
                        byte[] buffer2 = new byte[bytesRead];
                        for(int g = 0; g < bytesRead; g++){
                            buffer2[g] = byteBuffer[g];
                        }

                        mClientStringReceived = new String(byteBuffer).substring(0, bytesRead); //ADDED 03.09.2021 - Creates string of the bytefuffer.

                        if (mClientStringReceived != null && mTCP_ClientClassInterface != null) {
                            //call the method messageReceived from MyActivity class
                            Log.d(TAG, "Inside mRun - msg received: " + mClientStringReceived);

                            //Interface messages sent with UI Handler
                            String msgString = mClientStringReceived; //Take's care of string, as it is set to "null" before Handler posts runnable to thread
                            TCP_ClientUI_Handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mTCP_ClientClassInterface.onServerMessageReceived(msgString); //Raises event - To outside activity listeners
                                    mTCP_ClientClassInterface.onServerBytesReceived(buffer2); //Sends buffer as byte-array (for raw data messaging)
                                    Log.d(TAG, "EVENTS POSTED TO TCP_ClientUI_Handler (To UI MainLooper): " + msgString);
                                }
                            });

                            mClientStringReceived = null; //Resets msg string (NB: String is reset before UI_Handler is posted)
                            Log.d(TAG, "mClientStringReceived set to null ");
                        }

                    }
                    Log.d(TAG, "mRun While Loop ended: ");
                }
                catch (Exception exception){
                    Log.d(TAG, "Exception - Error with socket - preparing to close mSocket" + SERVER_IP);
                    mSocket.close();
                    mRun = false;
                    TCP_ClientUI_Handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mTCP_ClientClassInterface.onConnectionError("Exception - error during socket mRun");
                            Log.d(TAG, "Exception: Error during socket mRun");

                        }
                    });
                }
                finally {
                    mSocket.close();
                    mRun = false;
                    TCP_ClientUI_Handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mTCP_ClientClassInterface.onConnectionError("finally - error during socket mRun");
                            Log.d(TAG, "finally: Error during socket mRun");
                        }
                    });
                    Log.d(TAG, "finally: Socket Closed due to error in mRun loop");
                }
                //---------------- mSocket Creation Errors
            } catch (IOException e) {
                Log.d(TAG, "IOException - Error preparing mSocket" + SERVER_IP);
                mRun = false; //No point in trying to listen to socket
                TCP_ClientUI_Handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTCP_ClientClassInterface.onConnectionError("IOException - Error During socket creation");
                        Log.d(TAG, "IO Exception - Error during socket creation");
                    }
                });
                e.printStackTrace();
            }
            finally {
                Log.d(TAG, "finally - Error preparing mSocket" + SERVER_IP);
                mRun = false; //No point in trying to listen to socket
                TCP_ClientUI_Handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTCP_ClientClassInterface.onConnectionError("finally - error during socket creation");
                        Log.d(TAG, "finally - Error during socket creation");
                    }
                });
            }

        }
    }

    //INPUT THREAD AND RUNNABLES
    //THREAD
    private class TCP_ClientSendMessageThread extends Thread{
        //Thread running with looper to receive incoming messages.
        private static final String TAG = "TCP_ClientInputThread";
        public Looper looper;
        public Handler handler;

        @Override
        public void run() {
            Log.d(TAG, "run: Input Thread Started - Looper being prepared ");

            Looper.prepare(); //Establishes looper bound to thread
            looper = Looper.myLooper(); //makes looper accessible to
            Looper.loop();

            Log.d(TAG, "run: Input Thread looper ended");
        }
    }
    private class TCP_ClientSendMessageRunnable implements Runnable{
        private static final String TAG = "TCP_ClientOutputRunnable";
        /*
            Runnable used to send message to Server - Runnable injected to TCP_ClientOutputThread
            Use: TCP_ClientOutputRunnable mOutRun = new TCP_ClientOutputRunnable("Hello");
            Runnable shall be posted to object of type TCP_ClientOutputThread
         */
        String mMessage;
        public TCP_ClientSendMessageRunnable(String outputStringMessage) //constructor
        {
            mMessage = outputStringMessage;
        }

        @Override
        public void run() {
            if (mPrintWriter != null) {

                Log.d(TAG, "SendMessage: " + mMessage);

                mPrintWriter.println(mMessage);
                if (mPrintWriter != null) {
                    mPrintWriter.flush(); //Flushes output
                }
                Log.d(TAG, "Msg Sent");
            };
        }
    }



    //DIV SUBROUTINES
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
            Log.d(TAG, "SocketException: " + ex.toString());

        }
        myIP_ADDRESS = "x.x.x.x";
        return null;
    }

}




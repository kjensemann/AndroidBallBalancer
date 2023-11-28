package com.example.avr_ballbalancerapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class TCPview extends View {

    Context mContext;
    long lastTime; //For timing purposes on touch events
    long delayTime = 100; //Used in "on-touch method to delay multiple touches).

    //ANIMATION THREADS
    BtnPressAnimThread btnPressAnimThread; //Looper thread to handle button press handlers
    Handler btnPressUI_Handler;
    Path mAnimpath = new Path();
    Paint mAnimPaint = new Paint();

    CircleMovementThread circleMovementThread1;
    CircleMovementThread circleMovementThread2;
    Handler circleAnimUI_Handler;

    //NEW TCP CLIENT
    private TCP_ClientMain mTCP_ClientMain;

    private int mTCP_ConnectPushNr = 0;
    private String mIP_ServerAddressString; //IP Address of listening server (e.g. ESP8266)
    private String mPortSring; //Usually "8078"
    private String mIP_myIPAddressString; //Phone IP Address (where app is running)

    //Canvas Declarations
    private int canvas_height;
    private int canvas_width;

    //External View Objects
    RelativeLayout parent;
    EditText editText_sendData_Local;

    //Drawing Variable Declarations
    Paint mPaintTextHeadingGrey = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mPaintTextNormalBoldGrey = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mPaintTextNormalGrey = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mPaintTextNormalBoldGreyItallic = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mPaintTextRed = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mPaintRect = new Paint(Paint.ANTI_ALIAS_FLAG);

    Rect mRect_IP_Address = new Rect();
    Rect mRect_PORT = new Rect();
    Rect mRect_TCP_Connection_Status = new Rect();

    Rect mRect_Connection_Info = new Rect();
    Rect mRect_TCP_CommandSendString = new Rect();
    Path mPath_TCP_CommandSend = new Path();
    Paint mPaint_TCP_CommandSend = new Paint();
    Rect mRect_TCP_CommandReceivedString = new Rect();
    Path mPath_TCP_CommandReceived = new Path();
    Rect mRect_TCP_BytesReceived = new Rect();
    Path mPath_TCP_BytesReceived = new Path();

    //Drawables
    //private Drawable mTCP_CommandSendRotateRightDrawable;
    //Rect mRect_TCP_CommandSendRotateRight = new Rect();
    //private Drawable mTCP_CommandSendRotateLeftDrawable;
    //Rect mRect_TCP_CommandSendRotateLeft = new Rect();

    private Drawable mTCP_ConnectionDrawable;
    Rect mRect_TCP_Connection = new Rect();

    String mLabel_AddressString;
    int labelStartLeft = 15;
    int txtStartLeft;
    int txt_VertDist = 20; //Also set in "OnChanged"

    private String mTCP_ConenctionStatus;
    private int mTCP_ConnectionStatusInt; //0 = not connected, 1=connected
    private String mTCP_SendString;
    private String mTCP_ReceivedString;
    private String mTCP_ReceivedErrorString;
    private String mTCP_ReceivedBytesString;
    private byte[] mTCP_ReceivedBytesArray;

    //CIRCLECONTROLLER VARIABLES
    private Path mCircleOrigoPath;
    private Paint mCircleOrigoPaint;
    private float mCircleOrigo_X;
    private float mCircleOrigo_Y;
    private Path mCircleOrigoPath1;
    private Path mCircleOrigoPath2;
    private BubbleProp mCircle1_BubbleProp; //Circle classes prepared for work within Threads
    private BubbleProp mCircle2_BubbleProp;

    private Paint mCircleSendPaint;
    private float mCircleSend_Radius;
    private float mCircleSend_X;
    private float mCircleSend_Y;

    private Paint mCircleReceivePaint;
    private float mCircleReceive_Radius;
    private float mCircleReceive_X;
    private float mCircleReceive_Y;

    private Path mConnectionPath; //Path Connecting all the circles!

    //Firebase Declarations
    private FirebaseDatabase mFbDb;
    private DatabaseReference mFbDbRef;
    private DatabaseReference mFbDbServerRef;
    private String mFbDbServerSelectedKey;


    //VIEW EVENTS / INTERFACE
    private TCPviewEventListenerInterface mTCPviewEventListenerInterface;

    public TCPview(Context context){
        super(context);
        mContext = context;
        init(null);
    }

    public TCPview(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(attrs);
    }


    private void init(@Nullable AttributeSet set)
    {
        btnPressAnimThread = new BtnPressAnimThread();
        btnPressAnimThread.start();
        circleMovementThread1 = new CircleMovementThread();
        circleMovementThread1.start();
        circleMovementThread2 = new CircleMovementThread();
        circleMovementThread2.start();

        btnPressUI_Handler = new Handler(Looper.getMainLooper()); //Instantiated UI Handler used to dispatch animation updates to view
        circleAnimUI_Handler = new Handler(Looper.getMainLooper());

        //SET UP TCP_Client
        mTCP_ClientMain = new TCP_ClientMain(new TCP_ClientMain.TCP_ClientClassInterface() {
            @Override
            public void onServerMessageReceived(String messageFromServer) {
                //this method calls the onProgressUpdate
                Log.d("TCP_Client", "Message From Server: " + messageFromServer);
                mTCP_ReceivedString = messageFromServer;
                mTCP_ConnectionStatusInt = 1;
                invalidate();
                mTCPviewEventListenerInterface.onServerMessageReceived(messageFromServer); //Triggers events - For outside listeners (e.g. in MainActivity)
            }

            @Override
            public void onServerBytesReceived(byte[] bytesFromServer) {
                //This is for handlign data received from e.g. ESP8266 other than text (Character data)
                mTCP_ReceivedBytesArray = bytesFromServer;
                mTCP_ReceivedBytesString = byteArrayToHex(bytesFromServer);
                mTCPviewEventListenerInterface.onServerBytesReceived(bytesFromServer);//Triggers event
                invalidate();
            }

            @Override
            public void onConnectionError(String errorMsg) {
                mTCP_ReceivedErrorString = errorMsg;
                mTCP_ConnectionStatusInt = 0;
                mTCP_ConnectionDrawable = getResources().getDrawable(R.drawable.connection_active_icon,null);
                mTCP_ConnectionDrawable.setBounds(mRect_TCP_Connection);
            }
        });
        mTCP_ClientMain.setSERVER_IP(mIP_ServerAddressString);
        mTCP_ClientMain.setSERVER_PORT("8078");
        mIP_myIPAddressString = mTCP_ClientMain.getLocalIpAddress();

        //END TCP CLIENT SETUP

        mTCP_ReceivedBytesString = "test";
        //Including Std Layout Objects
        editText_sendData_Local = new EditText(mContext);
        editText_sendData_Local.setX(10);
        editText_sendData_Local.setY(500);
        editText_sendData_Local.setVisibility(View.VISIBLE);
        editText_sendData_Local.setText("EditText");
        editText_sendData_Local.setBackgroundColor(Color.RED);
        //editText_sendData_Local = (EditText)findViewById(R.id.editTextIP_Address1);

        //parent.addView(editText_sendData_Local);

        //Custom Layout Objects
        mPaintTextHeadingGrey.setColor(Color.DKGRAY);
        mPaintTextHeadingGrey.setTextSize(45);
        mPaintTextNormalBoldGrey.setColor(Color.DKGRAY);
        mPaintTextNormalBoldGrey.setTextSize(40);
        mPaintTextNormalBoldGrey.setTypeface(Typeface.DEFAULT_BOLD);

        mPaintTextNormalGrey.setColor(Color.DKGRAY);
        mPaintTextNormalGrey.setTextSize(40);
        //mPaintTextNormalGrey.setTypeface(Typeface.DEFAULT_BOLD);

        mPaintTextNormalBoldGreyItallic.setColor(Color.DKGRAY);
        mPaintTextNormalBoldGreyItallic.setTextSize(40);
        mPaintTextNormalBoldGreyItallic.setTextSkewX(-0.25f); //Italic

        mPaintTextRed.setColor(Color.RED);
        mPaintTextRed.setTextSize(40);
        mPaintTextRed.setTypeface(Typeface.SANS_SERIF);

        mPaintRect.setColor(Color.GRAY);
        mPaintRect.setStyle(Paint.Style.FILL_AND_STROKE);

        mLabel_AddressString = "WiFi Controller";
        mIP_ServerAddressString = "192.168.70.224";
        mPortSring = "8078";
        mTCP_ConnectionStatusInt = 0;
        mTCP_SendString = "CMD1";
        mPaint_TCP_CommandSend.setColor(Color.DKGRAY);
        mPaint_TCP_CommandSend.setStyle(Paint.Style.STROKE);
        mPaint_TCP_CommandSend.setStrokeWidth(6);

        mTCP_ReceivedString = "Received Text from Server";
        mTCP_ReceivedBytesString = "Hex Bytes from Server, e.g. 00AB-00AC";

        //CIRCLECONTROLLER INITS
        mCircleOrigoPath = new Path();
        mCircleOrigoPaint = new Paint();
        mCircleOrigoPaint.setStrokeWidth(6);
        mCircleOrigoPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mCircleOrigoPaint.setAntiAlias(true);
        mCircleOrigoPaint.setColor(Color.DKGRAY);

        mCircle1_BubbleProp = new BubbleProp();
        mCircle1_BubbleProp.setBubble_diam(40);
        mCircle1_BubbleProp.getBubblePaint().setStrokeWidth(12);
        mCircle1_BubbleProp.getBubblePaint().setAntiAlias(true);
        mCircle1_BubbleProp.getBubblePaint().setColor(Color.GRAY);


        mCircle2_BubbleProp = new BubbleProp();
        mCircle2_BubbleProp.setBubble_diam(50);
        mCircle2_BubbleProp.getBubblePaint().setStrokeWidth(12);
        mCircle2_BubbleProp.getBubblePaint().setAntiAlias(true);
        mCircle2_BubbleProp.getBubblePaint().setARGB(255,161,198,87); //Green

        mCircleSend_Radius = 25;
        mCircleSendPaint = new Paint();
        mCircleSendPaint.setStrokeWidth(12);
        mCircleSendPaint.setAntiAlias(true);
        mCircleSendPaint.setARGB(255,161,192,87); //Produced Water Color - Green

        mCircleReceive_Radius = 25;
        mCircleReceivePaint = new Paint();
        mCircleReceivePaint.setStrokeWidth(12);
        mCircleReceivePaint.setAntiAlias(true);
        mCircleReceivePaint.setColor(Color.GRAY);

        mConnectionPath = new Path();
        mTCP_ConnectionDrawable = getResources().getDrawable(R.drawable.connection_inactive_icon,null);

        //Animation Objects:
        mAnimPaint.setARGB(255,161,192,87); //Produced Water Color - Green
        mAnimPaint.setStyle(Paint.Style.STROKE);
        mAnimPaint.setStrokeWidth(6);
        mAnimPaint.setAntiAlias(true);


        //FIREBASE INITS
        mFbDb = FirebaseDatabase.getInstance();
        mFbDbRef = mFbDb.getReference();
        mFbDbServerRef = mFbDb.getReference().child("Servers");
        mFbDbRef.child("Test").setValue("HI There TEST");

        //mIP_ServerAddressString; //Field: IP_Addr
        //mPortSring = dataSnapshot.getValue().toString(); //Field: PortNr
        //mFbDbServerRef.addListenerForSingleValueEvent(new ValueEventListener() {
        mFbDbServerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (snapshot.child("IsSelected").getValue().toString().equals("true")){

                        mFbDbServerSelectedKey = snapshot.getKey(); //This is the "key" for the Server, e.g. "Server1", "Server2" etc
                        mIP_ServerAddressString = snapshot.child("IP_Addr").getValue().toString();
                        mPortSring = snapshot.child("PortNr").getValue().toString();
                        invalidate();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //canvas.drawRect(mTestRect, mPaintText);
        //canvas.drawRect(mRect_IP_Address, mPaintRect);
        canvas.drawText(mLabel_AddressString, mRect_IP_Address.left+canvas_width*5/100, mRect_IP_Address.top - canvas_height * 6 / 100, mPaintTextHeadingGrey);
        canvas.drawText("SERVER IP", labelStartLeft, mRect_IP_Address.bottom, mPaintTextNormalBoldGrey);
        canvas.drawText(mIP_ServerAddressString, txtStartLeft, mRect_IP_Address.bottom, mPaintTextNormalGrey);
        canvas.drawText("PORT", labelStartLeft, mRect_PORT.bottom, mPaintTextNormalBoldGrey);
        canvas.drawText(mPortSring, txtStartLeft, mRect_PORT.bottom, mPaintTextNormalGrey);
        canvas.drawText("myIP", labelStartLeft, mRect_Connection_Info.bottom, mPaintTextNormalBoldGrey);
        canvas.drawText(mIP_myIPAddressString, txtStartLeft, mRect_Connection_Info.bottom, mPaintTextNormalGrey);
        canvas.drawText("OutData", labelStartLeft, mRect_TCP_CommandSendString.bottom, mPaintTextNormalBoldGrey);
        canvas.drawText(mTCP_SendString, txtStartLeft, mRect_TCP_CommandSendString.bottom, mPaintTextNormalGrey);
        canvas.drawText("InData", labelStartLeft, mRect_TCP_CommandReceivedString.bottom , mPaintTextNormalBoldGrey);
        canvas.drawText(mTCP_ReceivedString, txtStartLeft, mRect_TCP_CommandReceivedString.bottom, mPaintTextNormalBoldGreyItallic);
        canvas.drawText("InBytes",labelStartLeft, mRect_TCP_BytesReceived.bottom , mPaintTextNormalBoldGrey);
        canvas.drawText(mTCP_ReceivedBytesString, txtStartLeft, mRect_TCP_BytesReceived.bottom, mPaintTextNormalBoldGreyItallic);
        //canvas.drawRect(mRect_PORT,mPaintTextRed);

        //DRAW CIRCLECONTROL

        if (mTCP_ConnectionStatusInt == 1)
        {
            canvas.drawPath(mPath_TCP_CommandSend, mPaint_TCP_CommandSend);
            canvas.drawCircle(mCircleSend_X,mCircleSend_Y,mCircleSend_Radius,mCircleSendPaint);
            canvas.drawPath(mPath_TCP_CommandReceived,mPaint_TCP_CommandSend);
            canvas.drawCircle(mCircleReceive_X,mCircleReceive_Y,mCircleReceive_Radius,mCircleReceivePaint);
            canvas.drawPath(mConnectionPath, mCircleOrigoPaint);
        }

        canvas.drawPath(mCircle1_BubbleProp.getBubbleConnectingLinePath(), mCircleOrigoPaint);
        canvas.drawCircle(mCircle1_BubbleProp.getX_pos(),mCircle1_BubbleProp.getY_pos(),(float)mCircle1_BubbleProp.getBubble_diam()/2,mCircle1_BubbleProp.getBubblePaint());
        canvas.drawPath(mCircle2_BubbleProp.getBubbleConnectingLinePath(), mCircleOrigoPaint);
        canvas.drawCircle(mCircle2_BubbleProp.getX_pos(),mCircle2_BubbleProp.getY_pos(),(float)mCircle2_BubbleProp.getBubble_diam()/2,mCircle2_BubbleProp.getBubblePaint());

        canvas.drawCircle(mCircleOrigo_X, mCircleOrigo_Y, 30,mCircleOrigoPaint);

        //Draw Animations (Custom)
        canvas.drawPath(mAnimpath,mAnimPaint);

        //Draw drawables
        mTCP_ConnectionDrawable.draw(canvas);



    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        canvas_height = h;
        canvas_width = w;

        txtStartLeft = canvas_width*22/100;
        labelStartLeft = canvas_width/100;

        txt_VertDist = canvas_height*6/100;

        mRect_IP_Address.left = labelStartLeft;
        mRect_IP_Address.top = (int)h*20/100;
        mRect_IP_Address.right = mRect_IP_Address.left + (int)w*50/100;
        mRect_IP_Address.bottom = mRect_IP_Address.top + txt_VertDist;

        mRect_PORT.left = labelStartLeft;
        mRect_PORT.top = mRect_IP_Address.bottom + txt_VertDist;
        mRect_PORT.right = mRect_IP_Address.right;
        mRect_PORT.bottom = mRect_PORT.top + txt_VertDist;

        mRect_Connection_Info.left = labelStartLeft;
        mRect_Connection_Info.top = mRect_PORT.bottom + txt_VertDist;
        mRect_Connection_Info.right = mRect_Connection_Info.left + (int)w*35/100;
        mRect_Connection_Info.bottom = mRect_Connection_Info.top + txt_VertDist;

        mRect_TCP_CommandSendString.left = labelStartLeft;
        mRect_TCP_CommandSendString.top = mRect_Connection_Info.bottom + 2*txt_VertDist;
        mRect_TCP_CommandSendString.right = mRect_Connection_Info.right;
        mRect_TCP_CommandSendString.bottom = mRect_TCP_CommandSendString.top + txt_VertDist;

        mRect_TCP_CommandReceivedString.left = labelStartLeft;
        mRect_TCP_CommandReceivedString.top = mRect_TCP_CommandSendString.top+2*txt_VertDist;
        mRect_TCP_CommandReceivedString.right = mRect_TCP_CommandSendString.right;
        mRect_TCP_CommandReceivedString.bottom = mRect_TCP_CommandReceivedString.top + txt_VertDist;

        mRect_TCP_BytesReceived.left = labelStartLeft;
        mRect_TCP_BytesReceived.top = mRect_TCP_CommandReceivedString.top + 3*txt_VertDist;
        mRect_TCP_BytesReceived.right = mRect_TCP_CommandReceivedString.right;
        mRect_TCP_BytesReceived.bottom = mRect_TCP_BytesReceived.top + txt_VertDist;

        //Drawables
        mRect_TCP_Connection.left = mRect_IP_Address.right + (int)w*2/100; //
        mRect_TCP_Connection.top = mRect_IP_Address.top;
        mRect_TCP_Connection.bottom = mRect_PORT.top;
        mRect_TCP_Connection.right = mRect_TCP_Connection.left + (mRect_TCP_Connection.bottom-mRect_TCP_Connection.top);
        mTCP_ConnectionDrawable.setBounds(mRect_TCP_Connection);

        //CIRCLECONTROLLER INITS
        mCircleOrigo_X = canvas_width*65/100;
        mCircleOrigo_Y = canvas_height*25/100;

        mCircle1_BubbleProp.setX_pos(canvas_width  * 75/100);
        mCircle1_BubbleProp.setY_pos(canvas_height * 20/100);
        mCircle1_BubbleProp.setOrigo_x_pos(mCircleOrigo_X);
        mCircle1_BubbleProp.setOrigo_y_pos(mCircleOrigo_Y);
        mCircle2_BubbleProp.setX_pos(canvas_width *  80/100);
        mCircle2_BubbleProp.setY_pos(canvas_height * 30/100);
        mCircle2_BubbleProp.setOrigo_x_pos(mCircleOrigo_X);
        mCircle2_BubbleProp.setOrigo_y_pos(mCircleOrigo_Y);

        mPath_TCP_CommandSend.moveTo(mRect_TCP_CommandSendString.left, mRect_TCP_CommandSendString.bottom + 15);
        mPath_TCP_CommandSend.lineTo(canvas_width*70/100, mRect_TCP_CommandSendString.bottom + 15);
        mPath_TCP_CommandSend.lineTo(mCircleOrigo_X,mCircleOrigo_Y);
        mCircleSend_X = canvas_width*70/100;
        mCircleSend_Y = mRect_TCP_CommandSendString.bottom + 15;

        mPath_TCP_CommandReceived.moveTo(mRect_TCP_CommandReceivedString.left,mRect_TCP_CommandReceivedString.bottom + 15);
        mPath_TCP_CommandReceived.lineTo(canvas_width*80/100,mRect_TCP_CommandReceivedString.bottom + 15);
        mPath_TCP_CommandReceived.lineTo(mCircleOrigo_X,mCircleOrigo_Y);
        mCircleReceive_X = canvas_width*80/100;
        mCircleReceive_Y = mRect_TCP_CommandReceivedString.bottom + 15;

        mConnectionPath.moveTo(mRect_TCP_Connection.right + 5,mRect_TCP_Connection.bottom-mRect_TCP_Connection.height()/2);
        mConnectionPath.lineTo(mCircleOrigo_X,mCircleOrigo_Y);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        // Create a rectangle from the point of touch - THis rectangle is checked for intersection with other "rectangles", ref below.
        Rect touchPoint = new Rect((int)x-1,(int)y-1,(int)x+1,(int)y+1);

        if(lastTime < 0)
        {
            lastTime = System.currentTimeMillis(); //preventing from too speedy touches.
        }
        else
        {
            if(System.currentTimeMillis() - lastTime < delayTime) //how much time you decide
            {


                return true; //ignore this event, but still treat it as handled
            }
            else {
                delayTime = 50; //Resets delay-time
                lastTime = System.currentTimeMillis();

                if (touchPoint.intersect(mRect_IP_Address)) {
                    delayTime = 1000; //Ensures delay is made longer than normal (to avoid sudden disconnection by finger touch)
                    Log.d("BtnPressAnimRunnable", "onTouchEvent: Clicked");
                    startBtnPressAnim(mRect_IP_Address);

                    Snackbar.make(this, "SELECT NEW SERVER?", Snackbar.LENGTH_SHORT).setAction("YES", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent myIntent = new Intent(mContext, settingsActivity.class); //To select new server
                            mContext.startActivity(myIntent);

                        }
                    }).show();
                    //-------------------------------

                }
                else if (touchPoint.intersect(mRect_PORT)) {
                    delayTime = 1000; //Ensures delay is made longer than normal (to avoid sudden disconnection by finger touch)
                    startBtnPressAnim(mRect_PORT);
                    Snackbar.make(this, "SELECT NEW PORT?", Snackbar.LENGTH_SHORT).setAction("YES", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {


                        }
                    }).show();
                    //-------------------------------

                }
                else if (touchPoint.intersect(mRect_Connection_Info)) {
                    delayTime = 1000;//Ensures delay is made longer than normal (to avoid sudden disconnection by finger touch)
                    startCircle1MovementAnim(mCircle1_BubbleProp,3000);
                    Snackbar.make(this, "CONNECTION INFO?", Snackbar.LENGTH_SHORT).setAction("YES", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startCircle2MovementAnim(mCircle2_BubbleProp,3000);

                        }
                    }).show();
                }

                else if (touchPoint.intersect(mRect_TCP_Connection)) {

                    delayTime = 1000; //Ensures delay is made longer than normal (to avoid sudden disconnection by finger touch)
                    if (mTCP_ConnectionStatusInt == 0)
                    {
                        Toast.makeText(mContext, "ConnectIcon pressed", Toast.LENGTH_SHORT).show();
                        //Connects to server
                        mTCP_ClientMain.setSERVER_PORT("8078");
                        mTCP_ClientMain.setSERVER_IP(mIP_ServerAddressString);
                        mTCP_ClientMain.connect(); //Connects client to server

                        //if (mTCP_ConnectTask == null) {
                        //    mTCP_ConnectTask = new TCPview.ConnectTask();
                        //    mTCP_ConnectTask.execute(mIP_ServerAddressString); //Where is ipAddress ending up? Can this be used to set the ServerIP Address??
                        //}
                        //mIP_AddressString

                        mTCP_ConnectionStatusInt = 1;
                        mTCP_ConnectionDrawable = getResources().getDrawable(R.drawable.connection_active_icon,null);
                        mTCP_ConnectionDrawable.setBounds(mRect_TCP_Connection);
                        invalidate();
                    }
                    else if (mTCP_ConnectionStatusInt == 1)
                    {
                        Toast.makeText(mContext, "Disconnected", Toast.LENGTH_SHORT).show();
                        mTCP_ClientMain.disConnect(); //Disconnects client to server connection
                        //mTCP_Client.stopClient();
                        //mTCP_ConnectTask = null;
                        mTCP_ConnectionStatusInt = 0;
                        mTCP_ConnectionDrawable = getResources().getDrawable(R.drawable.connection_inactive_icon,null);
                        mTCP_ConnectionDrawable.setBounds(mRect_TCP_Connection);
                        invalidate();
                    }

                }
                else if (touchPoint.intersect(mRect_TCP_CommandSendString))
                {
                    delayTime = 1000; //Ensures delay is made longer than normal (to avoid sudden disconnection by finger touch)
                    startBtnPressAnim(mRect_TCP_CommandSendString);

                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle("SET COMMAND TEXT");

                    // Set up the input
                    final EditText input = new EditText(mContext);
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);
                    input.setText("CMD1"); //Include Code to select from list
                    builder.setView(input);

                    // Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mTCP_SendString = input.getText().toString();

                            mTCP_ClientMain.sendStringToServer(mTCP_SendString);
                            mTCPviewEventListenerInterface.onMessageSentToServer(mTCP_SendString); //Triggers event, notifies outside listeners that message has been sent to server (e.g. command sent to ESP8266)
                            //Thread fst = new Thread(new ServerThread());
                            //fst.start();
                            invalidate();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            //myPortInputString = "";
                        }
                    });

                    builder.show();
                    //-------------------------------
                }
            }
        }

        //invalidate();

        return true;
    }

    //UPDATE SELECTED SERVER
    public void updateServerInView(){
        //Updates server address when "settingsActivity" is resumed.
        mFbDbServerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (snapshot.child("IsSelected").getValue().toString().equals("true")){

                        mFbDbServerSelectedKey = snapshot.getKey(); //This is the "key" for the Server, e.g. "Server1", "Server2" etc
                        mIP_ServerAddressString = snapshot.child("IP_Addr").getValue().toString();
                        mPortSring = snapshot.child("PortNr").getValue().toString();
                        invalidate();
                    }

                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    //SUBROUTINES FOR EXTERNAL CLASSESS
    String sendTCP_StringToServer(String str){ //Routine can be used from External Views or objects to send messages/commands to TCP Server
        String status_str = "NOT OK";
        if (mTCP_ConnectionStatusInt == 1) //If connection is active
        {
            mTCP_ClientMain.sendStringToServer(str);
            mTCP_SendString = str;
            status_str = "OK";
            mTCPviewEventListenerInterface.onMessageSentToServer(str); //Notifies outside listeners that message has been sent to server (e.g. to ESP8266). Event is handled in "MainActivity".
            invalidate();
        }
        else
        {
            status_str = "NOT OK";
        }
        return status_str;
    }



    //TCP_View Interface -------------------------------------
    public void setTCP_ViewEventListener(TCPviewEventListenerInterface listener) {
        mTCPviewEventListenerInterface = listener;
    }

    public interface TCPviewEventListenerInterface {
        public void onServerMessageReceived(String msg); //Event raised when android receives message from server (e.g. from ESP8266)
        public void onServerBytesReceived(byte[] bytes);
        public void onMessageSentToServer(String msg); //Event raised for outside listeners when android app sends message to server (e.g. to ESP8266)

    }
    // END INTERFACE ------------------------------------------

    //ANIMATION CUSTOM THREADING -------------------------------------------------------------------
    private void startCircle1MovementAnim(BubbleProp bubbleProp, int duration_ms)
    {
        /*
            Animates sinusoidal path for circle
         */
        CircleMovementRunnable mCircleMovementRunnable = new CircleMovementRunnable(bubbleProp, duration_ms);
        Handler mCircleMovementHandler = new Handler(circleMovementThread1.looper);
        mCircleMovementHandler.post(mCircleMovementRunnable);
    }
    private void startCircle2MovementAnim(BubbleProp bubbleProp, int duration_ms)
    {
        /*
            Animates sinusoidal path for circle
         */
        CircleMovementRunnable mCircleMovementRunnable = new CircleMovementRunnable(bubbleProp, duration_ms);
        Handler mCircleMovementHandler = new Handler(circleMovementThread2.looper);
        mCircleMovementHandler.post(mCircleMovementRunnable);
    }

    private class CircleMovementThread extends Thread{
        private static final String TAG = "CircleMovement";
        Looper looper;

        @Override
        public void run() {
            Looper.prepare();
            looper = Looper.myLooper();
            Looper.loop();
        }
    }

    private class CircleMovementRunnable implements Runnable{

        private BubbleProp bblProp;
        private float mCircle_X_Original;
        private float mCircle_Y_Original;
        private double mBubbleDiamOriginal;
        private double mBubbleDiamNew; //Pulsating diameter
        private float mCircle_X_new;
        private float mCircle_Y_new;
        private double dx=0;
        private double dy=0;
        private double y_local=0;
        private double x_local=0;
        private int duration_ms;
        private int steps = 360;
        private double mFrequency;
        private double mAmplitude;



        public CircleMovementRunnable(BubbleProp bubbleProp, int duration_ms)
        {
            /*
            Makes circle follow a pulsate and follow a horisontal sinusoidal path and turn back
             */
            bblProp = bubbleProp; //Class in order to be able to change
            mCircle_X_Original = bubbleProp.getX_pos();
            mCircle_Y_Original = bubbleProp.getY_pos();
            mBubbleDiamOriginal = bubbleProp.getBubble_diam();
            this.duration_ms = duration_ms;
            dx = 2*Math.PI/steps*80;
            mFrequency = 0.12; //Frequency
            mAmplitude = 25;
        }

        @Override
        public void run() {

            for (int i = 0; i <= steps; i++) {
                SystemClock.sleep((int)(duration_ms/steps));

                if (i < (int)(steps/2))
                {
                    x_local += dx;
                    y_local = mAmplitude * Math.cos(x_local*mFrequency);

                    mBubbleDiamNew = mBubbleDiamOriginal + 10*Math.cos(x_local*mFrequency);

                    mCircle_X_new = (float)x_local + mCircle_X_Original;
                    mCircle_Y_new = (float)y_local + mCircle_Y_Original;
                }

                else if (i < steps)
                {
                    x_local -= dx;
                    y_local = mAmplitude * Math.cos(x_local*mFrequency);

                    mBubbleDiamNew = mBubbleDiamOriginal + 10*Math.cos(x_local*mFrequency);

                    mCircle_X_new = (float)x_local + mCircle_X_Original;
                    mCircle_Y_new = (float)y_local + mCircle_Y_Original;
                }

                else if (i == steps){
                    mCircle_X_new = mCircle_X_Original;
                    mCircle_Y_new = mCircle_Y_Original;
                    mBubbleDiamNew = mBubbleDiamOriginal;
                }

                circleAnimUI_Handler.post(new Runnable() {
                    @Override
                    public void run() {
                        bblProp.setBubble_diam(mBubbleDiamNew);
                        bblProp.setX_pos(mCircle_X_new);
                        bblProp.setY_pos(mCircle_Y_new);
                        invalidate();
                    }
                });

            }

        }
    }


    //Button/Rect Press Underlining animation---------------
    private void startBtnPressAnim(Rect btnRect){
       /*
        Draws Animated P00h Around Rectangle using "btnPressAnimThread"
        */
        BtnPressAnimRunnable btnPressAnimRunnable = new BtnPressAnimRunnable(btnRect,mCircleSendPaint);
        Handler btnPressAnimHandler = new Handler(btnPressAnimThread.looper); //Links Handler to Thread Looper. Thread instantiated in "init".
        btnPressAnimHandler.post(btnPressAnimRunnable); //Passes runnable to thread through the Handler.

    }

    private class BtnPressAnimThread extends Thread{
        /*
            Thread for building button press animation
         */
        private static final String TAG = "BtnPressAnimThread";
        public Looper looper;
        public Handler handler;

        @Override
        public void run() {
            Log.d(TAG, "Thread Started - Looper being prepared");
            Looper.prepare();
            looper = Looper.myLooper();
            Looper.loop();

        }
    }
    private class BtnPressAnimRunnable implements Runnable{
        private static final String TAG = "BtnPressAnimRunnable";
        private Rect originalRect;
        private Paint iPathPaint;
        private Path animPath;

        public BtnPressAnimRunnable(Rect originalRect, Paint requiredPathPaint) {
            this.originalRect = originalRect;
            this.iPathPaint = requiredPathPaint;
            animPath = new Path();

        }

        @Override
        public void run() {
            Log.d(TAG, "Run Start: ");
            animPath.moveTo(originalRect.left,originalRect.bottom+5);
            int h=1;
            int l=1;

            for (int i = 0; i <= 100; i++) {

                if (i < 100)
                {
                    animPath.lineTo(originalRect.left + originalRect.width()/91*i, originalRect.bottom+5);
                    l+=1;
                }


                SystemClock.sleep(5);
                Log.d(TAG, "loop nr: " + String.valueOf(i));
                //Dispatch New Path to UIHandler
                btnPressUI_Handler.post(new Runnable() {
                    @Override
                    public void run() {

                        mAnimpath = animPath;
                        invalidate();
                    }
                });

                if (i==100){
                    animPath.reset();
                    //Reset Path
                    btnPressUI_Handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mAnimpath = animPath;
                            invalidate();
                        }
                    });
                }

            }



        }
    }
    //END Button/Rect Press Underlining animation---------------

    //DIV FUNCTIONS
    public String byteArrayToHex(byte[] a) {
        StringBuffer sb = new StringBuffer(a.length * 2);
        char j;
        j=0;
        for(byte b: a)
        {
            sb.append(String.format("%02x", b));
            j+=1;

            if (j==2)
            {
                sb.append("-");
                j=0;
            }



        }

        return sb.toString();
    }

}


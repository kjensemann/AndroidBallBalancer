package com.example.avr_ballbalancerapp;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

public class ballBalancerCtrlView extends View {
    Context mContext;
    long lastTime; //For timing purposes on touch events
    long delayTime = 100; //Used in "on-touch method to delay multiple touches).

    //Canvas Declarations
    private int canvas_height;
    private int canvas_width;

    //View Objects
    Paint mPaintTextOrangeDark = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mPaintTextHeadingGrey = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mPaintTextNormalBoldGrey = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mPaintTextNormalGrey = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mPaintTextNormalGreen = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Rect mKi_Rect = new Rect();
    private double mKi_val = 0.1;
    private String mKi_val_str = "0.1";
    private Rect mKp_Rect = new Rect();
    private double mKp_val = 1.3;
    private String mKp_val_str = "1.3";
    private Rect mKd_Rect = new Rect();
    private double mKd_val = 0.7;
    private String mKd_val_str = "0.7";

    private Rect mStartCtrl_Rect = new Rect(); //Rect to Start / Stop controller
    private Drawable mStartCtrl_Drawable;
    private int mCtrlStatus = 0;

    private Rect mMoveUp_Rect = new Rect();
    private Drawable mMoveUp_Drawable;

    private Rect mMoveDown_Rect = new Rect();
    private Drawable mMoveDown_Drawable;

    private Rect mServoPosMax_Rect = new Rect();
    private Drawable mServoposMax_Drawable;
    private Rect mServoPosMin_Rect = new Rect();
    private Drawable mServoposMin_Drawable;
    private Rect mServoPosMid_Rect = new Rect();
    private Drawable mServoposMid_Drawable;

    private Drawable mFunctionDrawable;
    private BallBalancerCtrlViewEventListenerInterface mBallBalancerCtrlViewEventListener;

    //Threading and Animations
    Handler mUI_Handler = new Handler(Looper.getMainLooper());
    AnimThread mAnimThread = new AnimThread();

    public ballBalancerCtrlView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public ballBalancerCtrlView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init(){
        //Initializer
        mCtrlStatus = 0;
        //Custom Layout Objects
        mPaintTextOrangeDark.setColor(Color.rgb(255,159,0) );
        mPaintTextOrangeDark.setTextSize(45);
        mPaintTextOrangeDark.setTypeface(Typeface.DEFAULT_BOLD);
        mPaintTextHeadingGrey.setColor(Color.DKGRAY);
        mPaintTextHeadingGrey.setTextSize(45);
        mPaintTextNormalBoldGrey.setColor(Color.DKGRAY);
        mPaintTextNormalBoldGrey.setTextSize(40);
        mPaintTextNormalBoldGrey.setTypeface(Typeface.DEFAULT_BOLD);
        mPaintTextNormalGrey.setColor(Color.DKGRAY);
        mPaintTextNormalGrey.setTextSize(40);

        mPaintTextNormalGreen.setColor(Color.rgb(0,255,0));
        mPaintTextNormalGreen.setTextSize(45);
        mPaintTextNormalGreen.setTypeface(Typeface.DEFAULT_BOLD);
        //mPaintTextNormalGrey.setTypeface(Typeface.DEFAULT_BOLD);

        //RECT Init's - NB: Finally initialized in "onSizeChanged" !
        mKp_Rect.left = 10;
        mKp_Rect.top = 10;

        mKi_Rect.left = 10;
        mKi_Rect.top = 10;

        mKd_Rect.left = 10;
        mKd_Rect.top = 10;

        mStartCtrl_Rect.left = 10;
        mStartCtrl_Rect.top = 10;
        mStartCtrl_Drawable = getResources().getDrawable(R.mipmap.ic_launcher_foreground,null);

        mMoveUp_Rect.left = 10;
        mMoveUp_Rect.top = 10;
        mMoveUp_Drawable = getResources().getDrawable(R.drawable.outline_expand_less_black_24dp,null);

        mMoveDown_Rect.left = 10;
        mMoveDown_Rect.top = 10;
        mMoveDown_Drawable = getResources().getDrawable(R.drawable.outline_expand_more_black_24dp,null);

        mServoposMax_Drawable = getResources().getDrawable(R.drawable.max_pos_orange, null);
        mServoPosMax_Rect.left = 10;
        mServoPosMax_Rect.top = 10;
        mServoposMin_Drawable = getResources().getDrawable(R.drawable.min_pos_orange, null);
        mServoPosMin_Rect.left = 10;
        mServoPosMin_Rect.top = 10;mServoposMid_Drawable = getResources().getDrawable(R.drawable.mid_pos_orange, null);
        mServoPosMid_Rect.left = 10;
        mServoPosMid_Rect.top = 10;


        mAnimThread.start();
    }

    public double getKi_val() {
        return mKi_val;
    }

    public double getKp_val() {
        return mKp_val;
    }

    public double getKd_val() {
        return mKd_val;
    }

    //Draw methods
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawText("Kp: ",mKp_Rect.left,mKp_Rect.top+mPaintTextOrangeDark.getTextSize(),mPaintTextOrangeDark);
        canvas.drawText(Double.toString(mKp_val) ,mKp_Rect.left+70,mKp_Rect.top+mPaintTextOrangeDark.getTextSize(),mPaintTextNormalBoldGrey);
        canvas.drawText("Ki: ",mKi_Rect.left,mKi_Rect.top+mPaintTextOrangeDark.getTextSize(),mPaintTextOrangeDark);
        canvas.drawText(Double.toString(mKi_val) ,mKi_Rect.left+70,mKi_Rect.top+mPaintTextOrangeDark.getTextSize(),mPaintTextNormalBoldGrey);
        canvas.drawText("Kd: ",mKd_Rect.left,mKd_Rect.top+mPaintTextOrangeDark.getTextSize(),mPaintTextOrangeDark);
        canvas.drawText(Double.toString(mKd_val) ,mKd_Rect.left+70,mKd_Rect.top+mPaintTextOrangeDark.getTextSize(),mPaintTextNormalBoldGrey);

        //canvas.drawText("SetPoint",canvas_width/2,canvas_height-10,mPaintTextNormalGrey);


        if (mCtrlStatus == 0)
        {
            canvas.drawText("NOT RUNNING", mStartCtrl_Rect.left, mStartCtrl_Rect.bottom + 20, mPaintTextNormalGrey);
        }
        else if (mCtrlStatus == 1){
            canvas.drawText("RUNNING", mStartCtrl_Rect.left, mStartCtrl_Rect.bottom + 20, mPaintTextNormalGreen);
        }

        //StartControl Button
        mStartCtrl_Drawable.draw(canvas);

        //Manual Up and Down movers //ANIMATED BY THREADS
        mMoveDown_Drawable.draw(canvas);
        mMoveUp_Drawable.draw(canvas);

        mServoposMax_Drawable.draw(canvas);
        mServoposMin_Drawable.draw(canvas);
        mServoposMid_Drawable.draw(canvas);

    }



    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvas_height = h;
        canvas_width = w;

        //Kp, Ki and Kd Rects
        mKp_Rect.left = 60;
        mKp_Rect.top = canvas_height/10 * 1;
        mKp_Rect.right= mKp_Rect.left+canvas_width/6;
        mKp_Rect.bottom = mKp_Rect.top+canvas_height/5;

        mKi_Rect.left = mKp_Rect.left + canvas_width/10 * 0;//3;
        mKi_Rect.top = canvas_height/10 * 4;
        mKi_Rect.right= mKp_Rect.right;
        mKi_Rect.bottom = mKi_Rect.top+canvas_height/5;

        mKd_Rect.left = mKp_Rect.left + canvas_width/10 * 0;
        mKd_Rect.top = canvas_height/10 * 7;
        mKd_Rect.right= mKp_Rect.right;
        mKd_Rect.bottom = mKd_Rect.top+canvas_height/5;

        mMoveUp_Rect.left = canvas_width - canvas_width/8;
        mMoveUp_Rect.right = mMoveUp_Rect.left + canvas_height/2;
        mMoveUp_Rect.top = 10;
        mMoveUp_Rect.bottom = mMoveUp_Rect.top + canvas_height/2;
        mMoveUp_Drawable.setBounds(mMoveUp_Rect);

        mMoveDown_Rect.left = canvas_width - canvas_width/8;
        mMoveDown_Rect.right = mMoveDown_Rect.left + canvas_height/2;
        mMoveDown_Rect.top = mMoveUp_Rect.top+ canvas_height/2;
        mMoveDown_Rect.bottom = mMoveDown_Rect.top + canvas_height/2;
        mMoveDown_Drawable.setBounds(mMoveDown_Rect);

        mServoPosMax_Rect.left = mMoveUp_Rect.left - canvas_width/14;
        mServoPosMax_Rect.right = mServoPosMax_Rect.left + canvas_height / 3;
        mServoPosMax_Rect.top = mMoveUp_Rect.top;
        mServoPosMax_Rect.bottom = mServoPosMax_Rect.top + canvas_height / 3;
        mServoposMax_Drawable.setBounds(mServoPosMax_Rect);

        mServoPosMin_Rect.left = mMoveDown_Rect.left - canvas_width/14;
        mServoPosMin_Rect.right = mServoPosMin_Rect.left + canvas_height / 3;
        mServoPosMin_Rect.bottom = mMoveDown_Rect.bottom;
        mServoPosMin_Rect.top = mMoveDown_Rect.bottom- canvas_height / 3;
        mServoposMin_Drawable.setBounds(mServoPosMin_Rect);

        mServoPosMid_Rect.left = mServoPosMax_Rect.left;
        mServoPosMid_Rect.right = mServoPosMid_Rect.left + canvas_height / 3;
        mServoPosMid_Rect.top = mServoPosMax_Rect.bottom;
        mServoPosMid_Rect.bottom = mServoPosMid_Rect.top + canvas_height / 3;
        mServoposMid_Drawable.setBounds(mServoPosMid_Rect);

        mStartCtrl_Rect.left = mKp_Rect.right + 0;
        mStartCtrl_Rect.right = mStartCtrl_Rect.left + (int)(canvas_height*0.5);
        mStartCtrl_Rect.top = canvas_height/10;
        mStartCtrl_Rect.bottom = mStartCtrl_Rect.top + (int)(canvas_height*0.5);
        mStartCtrl_Drawable.setBounds(mStartCtrl_Rect);

        //Function Drawable

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

                if (touchPoint.intersect(mKp_Rect)) {
                    delayTime = 1000; //Ensures delay is made longer than normal (to avoid sudden disconnection by finger touch)

                    Snackbar.make(this, "Kp?", Snackbar.LENGTH_SHORT).setAction("YES", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //Intent myIntent = new Intent(mContext, settingsActivity.class); //To select new server
                            //mContext.startActivity(myIntent);

                        }
                    }).show();

                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle("SET Kp value");

                    // Set up the input
                    final EditText input = new EditText(mContext);
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    input.setText("1.3"); //Include Code to select from list
                    builder.setView(input);

                    // Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mKp_val_str = input.getText().toString();
                            mKp_val = Double.parseDouble(mKp_val_str);

                            mBallBalancerCtrlViewEventListener.onSendMessageToTCP_Server("SET_Kp_" + mKp_val_str + "_SET_Kp"); // Triggers event
                            //mTCPviewEventListenerInterface.onMessageSentToServer(mTCP_SendString); //Triggers event, notifies outside listeners that message has been sent to server (e.g. command sent to ESP8266)

                            invalidate();
                        }

                    //-------------------------------

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

                else if (touchPoint.intersect(mKi_Rect)) {
                    delayTime = 1000; //Ensures delay is made longer than normal (to avoid sudden disconnection by finger touch)
                    Snackbar.make(this, "KI?", Snackbar.LENGTH_SHORT).setAction("YES", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //Intent myIntent = new Intent(mContext, settingsActivity.class); //To select new server
                            //mContext.startActivity(myIntent);

                        }
                    }).show();

                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle("SET Ki value");

                    // Set up the input
                    final EditText input = new EditText(mContext);
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    input.setText("0.1"); //Include Code to select from list
                    builder.setView(input);

                    // Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mKi_val_str = input.getText().toString();
                            mKi_val = Double.parseDouble(mKi_val_str);

                            mBallBalancerCtrlViewEventListener.onSendMessageToTCP_Server("SET_Ki_" + mKi_val_str + "_SET_Ki"); // Triggers event

                            invalidate();
                        }

                        //-------------------------------

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
                    //-------------------------------

                }
                else if (touchPoint.intersect(mKd_Rect)) {
                    delayTime = 1000; //Ensures delay is made longer than normal (to avoid sudden disconnection by finger touch)
                    Snackbar.make(this, "KD?", Snackbar.LENGTH_SHORT).setAction("YES", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //Intent myIntent = new Intent(mContext, settingsActivity.class); //To select new server
                            //mContext.startActivity(myIntent);

                        }
                    }).show();
                    //-------------------------------
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle("SET Kd value");

                    // Set up the input
                    final EditText input = new EditText(mContext);
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    input.setText("0.7"); //Include Code to select from list
                    builder.setView(input);

                    // Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mKd_val_str = input.getText().toString();
                            mKd_val = Double.parseDouble(mKd_val_str);

                            mBallBalancerCtrlViewEventListener.onSendMessageToTCP_Server("SET_Kd_" + mKd_val_str + "_SET_Kd"); // Triggers event

                            invalidate();
                        }

                        //-------------------------------

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
                else if (touchPoint.intersect(mStartCtrl_Rect)) {
                    delayTime = 1000; //Ensures delay is made longer than normal (to avoid sudden disconnection by finger touch)
                    animateRectangleMovement(mStartCtrl_Rect, mStartCtrl_Drawable, 500);
                    if (mCtrlStatus == 0) {
                        Snackbar.make(this, "START Controller", Snackbar.LENGTH_SHORT).setAction("YES", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mBallBalancerCtrlViewEventListener.onSendMessageToTCP_Server("CTRL_START_CTRL");
                                mCtrlStatus = 1;
                                invalidate();
                            }
                        }).show();
                    }
                    else if (mCtrlStatus == 1) {
                        Snackbar.make(this, "STOP Controller", Snackbar.LENGTH_SHORT).setAction("YES", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mBallBalancerCtrlViewEventListener.onSendMessageToTCP_Server("CTRL_STOP_CTRL");
                                mCtrlStatus = 0;
                                invalidate();

                            }
                        }).show();
                    }
                }
                else if (touchPoint.intersect(mMoveUp_Rect)) {
                    delayTime = 100; //Ensures delay is set
                    animateRectangleMovement(mMoveUp_Rect, mMoveUp_Drawable, 100);
                    mBallBalancerCtrlViewEventListener.onSendMessageToTCP_Server("SRV_MOVEUP");

                }
                else if (touchPoint.intersect(mMoveDown_Rect)) {
                    delayTime = 100; //Ensures delay is set
                    animateRectangleMovement(mMoveDown_Rect, mMoveDown_Drawable,100);
                    mBallBalancerCtrlViewEventListener.onSendMessageToTCP_Server("SRV_MOVEDOWN");
                }
                else if (touchPoint.intersect(mServoPosMax_Rect)) {
                    delayTime = 200; //Ensures delay is set
                    animateRectangleMovement(mServoPosMax_Rect, mServoposMax_Drawable,200);
                    mBallBalancerCtrlViewEventListener.onSendMessageToTCP_Server("SERVOPOS_1900_SERVOPOS");
                }
                else if (touchPoint.intersect(mServoPosMin_Rect)) {
                    delayTime = 500; //Ensures delay is set
                    animateRectangleMovement(mServoPosMin_Rect, mServoposMin_Drawable,200);
                    mBallBalancerCtrlViewEventListener.onSendMessageToTCP_Server("SERVOPOS_1100_SERVOPOS");
                }
                else if (touchPoint.intersect(mServoPosMid_Rect)) {
                    delayTime = 200; //Ensures delay is set
                    animateRectangleMovement(mServoPosMid_Rect, mServoposMid_Drawable,200);
                    mBallBalancerCtrlViewEventListener.onSendMessageToTCP_Server("SERVOPOS_1582_SERVOPOS");
                }
                
            }
        }

        //invalidate();

        return true;

    }

    //TCP_View Interface -------------------------------------
    public void setBallBalancerCtrl_ViewEventListener(BallBalancerCtrlViewEventListenerInterface listener) {
        mBallBalancerCtrlViewEventListener = listener;
    }

    public interface BallBalancerCtrlViewEventListenerInterface {
        public void onSendMessageToTCP_Server(String msg); //Event raised for outside listeners when android app sends message to server (e.g. to ESP8266) (Eg. change in setpoint, Kp, Ki etc

    }

    // END INTERFACE ------------------------------------------

    //ANIMATION THREADS, HANDLERS AND RUNNABLES
    private void animateRectangleMovement(Rect myRect, Drawable myDrawable, int duration_ms){
        Handler animHandler = new Handler(mAnimThread.looper); //binds animHandler to mAnimThread's looper (Non-UI-thread)
        Runnable mAnimRunnable = new AnimVibrateRunnable(myRect, myDrawable, duration_ms);
        animHandler.post(mAnimRunnable);
    }

    private class AnimThread extends Thread{

        Looper looper;
        Handler handler;

        @Override
        public void run() {

            Looper.prepare();
            looper = Looper.myLooper();
            Looper.loop();

        }
    }

    private class AnimVibrateRunnable implements Runnable{

        Drawable originalDrawable;
        Drawable newDrawable;
        Rect originalDrawableRect;
        Rect newDrawableRect;
        int mDuration_ms;

        public AnimVibrateRunnable(Rect drawableRect, Drawable drawable, int duration_ms) {
            originalDrawable = drawable;
            originalDrawableRect = drawableRect;
            mDuration_ms = duration_ms;
            newDrawableRect = new Rect();
            newDrawableRect.top = originalDrawableRect.top;
            newDrawableRect.left = originalDrawableRect.left;
            newDrawableRect.bottom = originalDrawableRect.bottom;
            newDrawableRect.right = originalDrawableRect.right;
        }

        @Override
        public void run() {
            int k = 0;

            for (int i = 0; i <= 100; i++) {
                SystemClock.sleep(mDuration_ms/100);
                if (i<25){
                    k+=1;
                    newDrawableRect.top = originalDrawableRect.top + k;
                    newDrawableRect.bottom = originalDrawableRect.bottom + k;

                }
                else if (i < 75)
                {
                    k-=1;
                    newDrawableRect.top = originalDrawableRect.top + k;
                    newDrawableRect.bottom = originalDrawableRect.bottom + k;
                }
                else if (i<100)
                {
                    k+=1;
                    newDrawableRect.top = originalDrawableRect.top + k;
                    newDrawableRect.bottom = originalDrawableRect.bottom + k;
                }
                else if (i==100)
                {
                    newDrawableRect = originalDrawableRect;
                }

                mUI_Handler.post(new Runnable() {
                    @Override
                    public void run() {
                        originalDrawable.setBounds(newDrawableRect);
                        invalidate();
                    }
                });

            }


        }


    }

}


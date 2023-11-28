package com.example.avr_ballbalancerapp;

import android.graphics.Paint;
import android.graphics.Path;

public class BubbleProp
{

    /*
    Class to hold circle properties for use within thread animations
     */

    public BubbleProp() {
        //instantiating variables
        bubblePaint = new Paint();
        bubbleConnectingLinePath = new Path();

    }

    private float x_pos, y_pos; //



    private float origo_x_pos, origo_y_pos;
    private double x_pos_d_start, y_pos_d_start;
    private double x_pos_d, y_pos_d;

    private Paint bubblePaint;
    private Path bubbleConnectingLinePath;
    private double bubble_diam;

    public float getX_pos() {
        return x_pos;
    }

    public void setX_pos(float x_pos) {
        this.x_pos = x_pos;
    }

    public float getY_pos() {
        return y_pos;
    }

    public void setY_pos(float y_pos) {
        this.y_pos = y_pos;
    }

    public Paint getBubblePaint() {
        return bubblePaint;
    }
    public void setBubblePaint(Paint bubblePaint) {
        this.bubblePaint = bubblePaint;
    }

    public Path getBubbleConnectingLinePath() {
        bubbleConnectingLinePath.reset();
        bubbleConnectingLinePath.moveTo(origo_x_pos,origo_y_pos); //Starts Line at origo
        bubbleConnectingLinePath.lineTo(x_pos, y_pos); //moves to Circle1 center
        return bubbleConnectingLinePath;
    }
    public void setOrigo_x_pos(float origo_x_pos) {
        this.origo_x_pos = origo_x_pos;
    }
    public void setOrigo_y_pos(float origo_y_pos) {
        this.origo_y_pos = origo_y_pos;
    }


    public double getBubble_diam() {
        return bubble_diam;
    }

    public void setBubble_diam(double bubble_diam) {
        this.bubble_diam = bubble_diam;
    }
}

package com.example.avr_ballbalancerapp;

public class clsBallControllerData {

    private float Ki, Kd, Kp; //Integral, Derivative and Proportional constants.
    private float KV_Arr[], PV_Arr[];
    private float SP; //SetPoint
    private String dataNameStr;
    double[] PID_RawOutPutArray;
    double[] PV_RawOutPutArray;

    public void setPID_RawOutPutArray(double[] PID_RawOutPutArray) {
        this.PID_RawOutPutArray = PID_RawOutPutArray;
    }

    public void setPV_RawOutPutArray(double[] PV_RawOutPutArray) {
        this.PV_RawOutPutArray = PV_RawOutPutArray;
    }



    /* --------------
     Class to hold data received from ESP8266 over WiFi and to make it plottable.
        - PV Data, CV data and SetPoint kept here
        - Controller Parameters such as: Kd, Ki and Kp (derivative, integral and proportional)

        - Data items are made which can be plotted with MPandroidChart library

     ---------------- */



}

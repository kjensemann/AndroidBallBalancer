package com.example.avr_ballbalancerapp;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;

public class clsBallControllerData {
    /* --------------
     Class to hold data received from ESP8266 over WiFi and to make it plottable.
        - PV Data, CV data and SetPoint kept here
        - Controller Parameters such as: Kd, Ki and Kp (derivative, integral and proportional)

        - Data items are made which can be plotted with MPandroidChart library

     ---------------- */
    private float Ki, Kd, Kp; //Integral, Derivative and Proportional constants.
    private float KV_Arr[], PV_Arr[];
    private float SP; //SetPoint
    private String dataNameStr;
    private int dataItemNr;

    double[] PID_RawOutPutArray;
    float[] PID_OutPutArray;
    double[] PV_RawOutPutArray;
    float[] PV_OutPutArray;

    //MPChart Declarations
    private ArrayList<Entry> mpChart_PV_ValuesArrayList = new ArrayList<Entry>();
    private ArrayList<Entry> mpChart_PID_ValuesArrayList = new ArrayList<Entry>();

    public void setPID_RawOutPutArray(double[] PID_RawOutPutArray) {
        this.PID_RawOutPutArray = PID_RawOutPutArray;
        createPID_OutputArray(PID_RawOutPutArray);
        //Create Float Array which can be plotted
    }

    public void setPV_RawOutPutArray(double[] PV_RawOutPutArray) {
        this.PV_RawOutPutArray = PV_RawOutPutArray;
        //Create Float Array which can be plotted!
    }

    private void createPID_OutputArray(double[] dataDoubleArray){

        int dataLength;
        dataLength = dataDoubleArray.length;
        PID_OutPutArray = new float[dataLength];

        for (int i = 0; i < dataLength; i++) {
            float val = (float)dataDoubleArray[i];
            PID_OutPutArray[i]=val;
            mpChart_PID_ValuesArrayList.add(new Entry(i, val)); //Set2

        }

    }

    private void createPV_OutputArray(double[] dataDoubleArray){

        int dataLength;
        dataLength = dataDoubleArray.length;
        PV_OutPutArray = new float[dataLength];

        for (int i = 0; i < dataLength; i++) {
            //Set1 - PV = Distance - Sensor Calculation

            //PID_PVValuesArrayList.add(new Entry(lastPlotInt, val2)); //Set1 No calculation
            double dblVal1 = dataDoubleArray[i];
            double dblVal2 = dblVal1/1024*5; //Integer converted to a voltage value (Vref on atmega is 5V, approx)
            double PV_val_dbl = sharp_get_mm_from_volt(dblVal2);
            PV_OutPutArray[i] = (float)PV_val_dbl;

            mpChart_PV_ValuesArrayList.add(new Entry(i, PV_OutPutArray[i])); //Set1 - Calculated value
        }



    }

    private double sharp_get_mm_from_volt(double adc_voltage_input)
    {
        //GP2Y0A41SK0F - SHARP SHORT DISTANCE SENSOR LINEARIZATION ROUTINES
        //Function to calculate volts into mm
        // y = -2.658108E+01x5 + 2.585105E+02x4 - 9.788230E+02x3 + 1.823948E+03x2 - 1.742236E+03x + 7.928548E+02

        double ans, x5, x4, x3, x2, x1, x0;
        x5 = -26.58108f   * adc_voltage_input * adc_voltage_input * adc_voltage_input * adc_voltage_input * adc_voltage_input;
        x4 = 258.5105f    * adc_voltage_input * adc_voltage_input * adc_voltage_input * adc_voltage_input;
        x3 = -978.8230f   * adc_voltage_input * adc_voltage_input * adc_voltage_input;
        x2 = 1823.948f    * adc_voltage_input * adc_voltage_input;
        x1 = -1742.236f   * adc_voltage_input;
        x0 = 792.8548f;

        ans =  x5 + x4 + x3 + x2 + x1 + x0;

        return ans;  //Distance in mm from sensor
    }



}

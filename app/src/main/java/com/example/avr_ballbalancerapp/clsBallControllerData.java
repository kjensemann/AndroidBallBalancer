package com.example.avr_ballbalancerapp;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;

public class clsBallControllerData {

    private float Ki, Kd, Kp; //Integral, Derivative and Proportional constants.
    private float KV_Arr[], PV_Arr[];
    private float SP; //SetPoint
    private String dataNameStr;
    double[] PID_RawOutPutArray;
    double[] PV_RawOutPutArray;
    float[] PID_OutputArray;
    float[] PV_OutputArray;

    //MPChart Declarations
    private ArrayList<Entry> mpcPID_OutputValuesArrayList = new ArrayList<>();
    private ArrayList<Entry> mpcPV_OutputValuesArrayList = new ArrayList<>();

    public void setPID_RawOutPutArray(double[] PID_RawOutPutArray) {
        this.PID_RawOutPutArray = PID_RawOutPutArray;
        setPID_OutPutArray(PID_RawOutPutArray);
    }

    public void setPV_RawOutPutArray(double[] PV_RawOutPutArray) {
        this.PV_RawOutPutArray = PV_RawOutPutArray;
        setPV_OutPutArray(PV_RawOutPutArray);
    }

    private void setPID_OutPutArray(double[] myData){
        int dataLength;
        dataLength = myData.length;
        PID_OutputArray = new float[dataLength];

        for (int i = 0; i < dataLength; i++) {
            float val = (float)myData[i];
            PID_OutputArray[i]=val;
            //PID_OutputValuesArrayList.add(new Entry(lastPlotInt, val)); //Set2
            mpcPID_OutputValuesArrayList.add(new Entry(i, PID_OutputArray[i])); //Set1 - Calculated value
        }


    }

    private void setPV_OutPutArray(double[] myData){
        int dataLength;
        dataLength = myData.length;
        PV_OutputArray = new float[dataLength];
        for (int i = 0; i < dataLength; i++) {

            //Set1 - PV = Distance - Sensor Calculation
            float val2 = (float)myData[i];
            double PV_val_dbl;
            val2 = val2/1024*5; //Converts it to
            PV_val_dbl = sharp_get_mm_from_volt(val2);
            PV_OutputArray[i]=(float) PV_val_dbl;
            mpcPV_OutputValuesArrayList.add(new Entry(i, PV_OutputArray[i])); //Set1 - Calculated value


        }


    }

    /* --------------
     Class to hold data received from ESP8266 over WiFi and to make it plottable.
        - PV Data, CV data and SetPoint kept here
        - Controller Parameters such as: Kd, Ki and Kp (derivative, integral and proportional)

        - Data items are made which can be plotted with MPandroidChart library

     ---------------- */

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

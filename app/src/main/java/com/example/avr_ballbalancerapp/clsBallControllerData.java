package com.example.avr_ballbalancerapp;

import android.graphics.Color;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

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
    private double[] PID_RawOutPutArray;
    private double[] PV_RawOutPutArray;
    private float[] PID_OutputArray;
    private float[] PV_OutputArray;

    //Firebase Data
    private FirebaseDatabase mFbDb;
    private DatabaseReference mFbDbRef;
    private List<Float> PID_DataList;  //Used for sending data to firebase (arrays of data)
    private List<Float> PV_DataList;   //Used for sending data to firebase (arrays of data)

    //MPChart Declarations
    private LineData mpcLineData;
    private LineDataSet mpcPID_LineDataSet; //This holds the "plottable data" including info on the plotting properties
    private LineDataSet mpcPV_LineDataSet;
    private ArrayList<Entry> mpcPID_OutputValuesArrayList = new ArrayList<>(); //This holds all the datapoints with the values
    private ArrayList<Entry> mpcPV_OutputValuesArrayList = new ArrayList<>(); //This holds all the datapoints with the values.

    //Class Constructors
    public clsBallControllerData() {
        mFbDb = FirebaseDatabase.getInstance();
        mFbDbRef = mFbDb.getReference().child("balancerData");

    }

    //
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
            PID_DataList.add(val); //For export to firebase
            //PID_OutputValuesArrayList.add(new Entry(lastPlotInt, val)); //Set2
            mpcPID_OutputValuesArrayList.add(new Entry(i, PID_OutputArray[i])); //Set1 - Calculated value
        }
        //Create Plottable DataSet - MPChart
        mpcPID_LineDataSet = new LineDataSet(mpcPID_OutputValuesArrayList, "CV [PWM]");
        mpcPID_LineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        mpcPID_LineDataSet.setColor(ColorTemplate.getHoloBlue());
        mpcPID_LineDataSet.setCircleColor(ColorTemplate.getHoloBlue());
        mpcPID_LineDataSet.setLineWidth(2f);
        mpcPID_LineDataSet.enableDashedLine(0f,2f,0f);
        mpcPID_LineDataSet.setCircleRadius(3f);
        mpcPID_LineDataSet.setFillAlpha(65);
        mpcPID_LineDataSet.setFillColor(ColorTemplate.getHoloBlue());
        mpcPID_LineDataSet.setHighLightColor(Color.rgb(244, 117, 117));

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
            PV_DataList.add(PV_OutputArray[i]); //For export to firebase
            mpcPV_OutputValuesArrayList.add(new Entry(i, PV_OutputArray[i])); //Set1 - Calculated value


        }
        //CREATE PLOTTABLE DATASET - MPChart
        mpcPV_LineDataSet = new LineDataSet(mpcPV_OutputValuesArrayList,"PV [mm]");
        mpcPV_LineDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        mpcPV_LineDataSet.setColor(Color.RED);
        mpcPV_LineDataSet.setCircleColor(Color.RED);
        mpcPV_LineDataSet.setLineWidth(2f);
        mpcPV_LineDataSet.enableDashedLine(0f,2f,0f);
        mpcPV_LineDataSet.setCircleRadius(3f);
        mpcPV_LineDataSet.setFillAlpha(65);
        mpcPV_LineDataSet.setFillColor(Color.RED);
        mpcPV_LineDataSet.setDrawCircleHole(true);
        mpcPV_LineDataSet.setHighLightColor(Color.rgb(244, 117, 117));
    }

    public LineData getMPCLineData() {


        mpcLineData = new LineData(mpcPID_LineDataSet, mpcPV_LineDataSet);

        return mpcLineData;
    }

    public void exportDataToFirebase(){

            mFbDbRef.child("PV_Values").setValue(PV_DataList);
            mFbDbRef.child("PID_Values").setValue(PID_DataList);

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

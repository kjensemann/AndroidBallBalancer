package com.example.avr_ballbalancerapp;

import android.graphics.Color;
import android.util.Log;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class clsBallControllerData {

    /* --------------
     Class to hold data received from ESP8266 over WiFi and to make it plottable.
        - PV Data, CV data and SetPoint kept here
        - Controller Parameters such as: Kd, Ki and Kp (derivative, integral and proportional)

        - Data items are made which can be plotted with MPandroidChart library

     ---------------- */
    private String mBallCtrlDataName;

    private double PID_Kp;
    private double PID_Ki;
    private double PID_Kd; //Integral, Derivative and Proportional constants.
    private double PID_SetPoint; //SetPoint
    private Duration durationOfDataCollection;
    private double durationOfDataCollectionInSeconds; // s
    private double frequencyOfDataCollectionInMilliseconds; //ms

    private String dataNameStr;
    private double[] PID_RawOutPutArray;
    private double[] PV_RawOutPutArray;
    private float[] PID_OutputArray;
    private float[] PV_OutputArray;


    //Firebase Data
    private FirebaseDatabase mFbDb;
    private DatabaseReference mFbDbRef;
    private List<Float> PID_DataList = new ArrayList<>();  //Used for sending data to firebase (arrays of data)
    private List<Float> PV_DataList = new ArrayList<>();   //Used for sending data to firebase (arrays of data)

    //MPChart Declarations
    private ScatterData mpcScatterData;
    private ScatterDataSet mpcPID_ScatterDataSet; //This holds the "plottable data" including info on the plotting properties
    private ScatterDataSet mpcPV_ScatterDataSet;
    private ScatterDataSet mpcSetPointScatterDataSet;
    private ArrayList<Entry> mpcPID_OutputValuesArrayList = new ArrayList<>(); //This holds all the datapoints with the values
    private ArrayList<Entry> mpcPV_OutputValuesArrayList = new ArrayList<>(); //This holds all the datapoints with the values.
    private ArrayList<Entry> mpcSetPointValuesArrayList = new ArrayList<>();

    //Class Constructors
    public clsBallControllerData() {
        mFbDb = FirebaseDatabase.getInstance();
        mFbDbRef = mFbDb.getReference().child("balancerData");

        mBallCtrlDataName = "DefaultDataName"; //If changed, data will be written to new tagged place in Firebase

        mpcPID_OutputValuesArrayList = new ArrayList<>();
        mpcPV_OutputValuesArrayList = new ArrayList<>();
    }

    //Class Fields
    public String getBallCtrlDataName() {
        return mBallCtrlDataName;
    }

    public void setBallCtrlDataName(String ballCtrlDataName) {
        this.mBallCtrlDataName = ballCtrlDataName;
    }
    public double getPID_SetPoint() {
        return PID_SetPoint;
    }

    public void setPID_SetPoint(double PID_SetPoint) {
        this.PID_SetPoint = PID_SetPoint;
    }

    public double getPID_Kp() {
        return PID_Kp;
    }
    public void setPID_Kp(double PID_Kp) {
        this.PID_Kp = PID_Kp;
    }

    public double getPID_Ki() {
        return PID_Ki;
    }
    public void setPID_Ki(double PID_Ki) {
        this.PID_Ki = PID_Ki;
    }

    public double getPID_Kd() {
        return PID_Kd;
    }
    public void setPID_Kd(double PID_Kd) {
        this.PID_Kd = PID_Kd;
    }

    public double getDurationOfDataCollectionInSeconds() {
        return durationOfDataCollectionInSeconds;
    }

    public void setDurationOfDataCollection(Duration durationOfDataCollection) {
        durationOfDataCollectionInSeconds = durationOfDataCollection.toNanos()/1e9;
        frequencyOfDataCollectionInMilliseconds = (durationOfDataCollectionInSeconds-0.1)/((double)PID_OutputArray.length)*1000; //I subtract the delays (-0.1 S = 100ms) added inside the MCU before the control starts.

        this.durationOfDataCollection = durationOfDataCollection;
    }


    public void setPID_RawOutPutArray(double[] PID_RawOutPutArray) {
        this.PID_RawOutPutArray = PID_RawOutPutArray;
        setPID_OutPutArray(PID_RawOutPutArray);
    }

    public void setPV_RawOutPutArray(double[] PV_RawOutPutArray) {
        this.PV_RawOutPutArray = PV_RawOutPutArray;
        setPV_OutPutArray(PV_RawOutPutArray);
    }

    private void setPID_OutPutArray(double[] myData){
        //PID=Control Variable = Servo_position
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
        //mpcPID_OutputValuesArrayList.sort(new EntryXComparator());
        //Create Plottable DataSet - MPChart
        mpcPID_ScatterDataSet = new ScatterDataSet(mpcPID_OutputValuesArrayList, "CV [mm]");
        mpcPID_ScatterDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        mpcPID_ScatterDataSet.setColor(ColorTemplate.getHoloBlue());
        mpcPID_ScatterDataSet.setHighLightColor(Color.rgb(244, 117, 117));
        mpcPID_ScatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        mpcPID_ScatterDataSet.setScatterShapeHoleRadius(2f);
        mpcPID_ScatterDataSet.setScatterShapeSize(3f);
        mpcPID_ScatterDataSet.setDrawValues(false);

    }

    public void setPID_OutPutArrayFromFirebase(List<Double> myData){
        //PID=Control Variable = Servo_position
        int dataLength;
        dataLength = myData.size();
        PID_OutputArray = new float[dataLength];
        List<Double> rawList = myData;

        List<Double> PID2_Values = new ArrayList<>();
        //CONVERSION LONG TO DOUBLE
        if (rawList != null) {

            for (Object item : rawList) {
                if (item instanceof Long) {
                    PID2_Values.add(((Long) item).doubleValue());
                } else if (item instanceof Double) {
                    PID2_Values.add((Double) item);
                }
            }
            // Print the converted PID_Values
            for (Double value : PID2_Values) {
                Log.d("Firebase", "PID Value: " + value);
            }
        } else {
            Log.e("Firebase", "PID_Values is null or not properly structured.");
        }

        //END CONVERSION

        for (int i = 0; i < dataLength; i++) {
            float val = (float)PID2_Values.get(i).doubleValue();
            PID_OutputArray[i]=val;
            PID_DataList.add(val); //For export to firebase
            //PID_OutputValuesArrayList.add(new Entry(lastPlotInt, val)); //Set2
            mpcPID_OutputValuesArrayList.add(new Entry(i, PID_OutputArray[i])); //Set1 - Calculated value
        }
        //mpcPID_OutputValuesArrayList.sort(new EntryXComparator());
        //Create Plottable DataSet - MPChart
        mpcPID_ScatterDataSet = new ScatterDataSet(mpcPID_OutputValuesArrayList, "CV [mm]");
        mpcPID_ScatterDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        mpcPID_ScatterDataSet.setColor(ColorTemplate.getHoloBlue());
        mpcPID_ScatterDataSet.setHighLightColor(Color.rgb(244, 117, 117));
        mpcPID_ScatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        mpcPID_ScatterDataSet.setScatterShapeHoleRadius(2f);
        mpcPID_ScatterDataSet.setScatterShapeSize(3f);
        mpcPID_ScatterDataSet.setDrawValues(false);

    }

    public ScatterDataSet getMpcPID_ScatterDataSet() {
        return mpcPID_ScatterDataSet;
    }
    public ScatterDataSet getMpcPV_ScatterDataSet() {
        return mpcPV_ScatterDataSet;
    }
    public ScatterDataSet getMpcSetPoint_ScatterDataSet() {
        return mpcSetPointScatterDataSet;
    }

    private void setPV_OutPutArray(double[] myData){
        //PV = Process Variable (mm avstand til sensor)
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
            mpcSetPointValuesArrayList.add(new Entry(i, (float)PID_SetPoint));


        }
        //CREATE PLOTTABLE DATASET - MPChart
        //mpcPV_OutputValuesArrayList.sort(new EntryXComparator());
        mpcPV_ScatterDataSet = new ScatterDataSet(mpcPV_OutputValuesArrayList,"PV [mm]");
        mpcPV_ScatterDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        mpcPV_ScatterDataSet.setColor(Color.RED);
        mpcPV_ScatterDataSet.setHighLightColor(Color.rgb(244, 117, 117));
        mpcPV_ScatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        mpcPV_ScatterDataSet.setScatterShapeHoleRadius(2f);
        mpcPV_ScatterDataSet.setScatterShapeSize(3f);
        mpcPV_ScatterDataSet.setDrawValues(false);

        //Prepares SetPoint Dataset
        mpcSetPointScatterDataSet = new ScatterDataSet(mpcSetPointValuesArrayList, "SetPoint [mm]");
        mpcSetPointScatterDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        mpcSetPointScatterDataSet.setColor(Color.RED);
        mpcSetPointScatterDataSet.setHighLightColor(Color.rgb(244, 117, 117));
        mpcSetPointScatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        mpcSetPointScatterDataSet.setScatterShapeHoleRadius(1f);
        mpcSetPointScatterDataSet.setScatterShapeSize(2f);
        mpcSetPointScatterDataSet.setDrawValues(false);
    }

    public void setPV_OutPutArrayFromFirebase(List<Double> myData){
        //PV = Process Variable (mm avstand til sensor)
        int dataLength;
        dataLength = myData.size();
        PV_OutputArray = new float[dataLength];
        for (int i = 0; i < dataLength; i++) {

            //Set1 - PV = Distance - Sensor Calculation
            float val2 = (float)myData.get(i).doubleValue();
            PV_OutputArray[i]=(float) val2;
            PV_DataList.add(PV_OutputArray[i]); //For export to firebase
            mpcPV_OutputValuesArrayList.add(new Entry(i, PV_OutputArray[i])); //Set1 - Calculated value
            mpcSetPointValuesArrayList.add(new Entry(i, (float)PID_SetPoint));


        }
        //CREATE PLOTTABLE DATASET - MPChart
        //mpcPV_OutputValuesArrayList.sort(new EntryXComparator());
        mpcPV_ScatterDataSet = new ScatterDataSet(mpcPV_OutputValuesArrayList,"PV [mm]");
        mpcPV_ScatterDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        mpcPV_ScatterDataSet.setColor(Color.RED);
        mpcPV_ScatterDataSet.setHighLightColor(Color.rgb(244, 117, 117));
        mpcPV_ScatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        mpcPV_ScatterDataSet.setScatterShapeHoleRadius(2f);
        mpcPV_ScatterDataSet.setScatterShapeSize(3f);
        mpcPV_ScatterDataSet.setDrawValues(false);

        //Prepares SetPoint Dataset
        mpcSetPointScatterDataSet = new ScatterDataSet(mpcSetPointValuesArrayList, "SetPoint [mm]");
        mpcSetPointScatterDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        mpcSetPointScatterDataSet.setColor(Color.RED);
        mpcSetPointScatterDataSet.setHighLightColor(Color.rgb(244, 117, 117));
        mpcSetPointScatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        mpcSetPointScatterDataSet.setScatterShapeHoleRadius(1f);
        mpcSetPointScatterDataSet.setScatterShapeSize(2f);
        mpcSetPointScatterDataSet.setDrawValues(false);
    }

    public ScatterData getMpcScatterData() {


        mpcScatterData = new ScatterData(mpcSetPointScatterDataSet,mpcPV_ScatterDataSet,mpcPID_ScatterDataSet);
        mpcScatterData.setValueTextColor(Color.WHITE);
        mpcScatterData.setValueTextSize(9f);
        mpcScatterData.notifyDataChanged();

        return mpcScatterData;
    }

    public void exportDataToFirebase(String dataName){
            mBallCtrlDataName = dataName;
            mFbDbRef.child(mBallCtrlDataName).child("PV_Values").setValue(PV_DataList);
            mFbDbRef.child(mBallCtrlDataName).child("PID_Values").setValue(PID_DataList);
            mFbDbRef.child(mBallCtrlDataName).child("PID_Kp").setValue(PID_Kp);
            mFbDbRef.child(mBallCtrlDataName).child("PID_Ki").setValue(PID_Ki);
            mFbDbRef.child(mBallCtrlDataName).child("PID_Kd").setValue(PID_Kd);
            mFbDbRef.child(mBallCtrlDataName).child("PID_SetPoint").setValue(PID_SetPoint);
            mFbDbRef.child(mBallCtrlDataName).child("durationOfDataCollectionInSeconds").setValue(durationOfDataCollectionInSeconds); //All samples
            mFbDbRef.child(mBallCtrlDataName).child("frequencyOfDataCollectionInMilliseconds").setValue(frequencyOfDataCollectionInMilliseconds); //Calculated (

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

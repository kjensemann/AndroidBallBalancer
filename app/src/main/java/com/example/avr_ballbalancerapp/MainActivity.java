package com.example.avr_ballbalancerapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private TCPview mTCPview;
    private String mTCP_MessageReceived;
    private String mTCP_MessageSent;

    private ballBalancerCtrlView mBalancerCtrlView;

    //Firebase Declarations
    private FirebaseDatabase mFbDb;
    private DatabaseReference mFbDbRef;

    //MPandroidChart
    private ScatterChart scatterChart;
    private XAxis xAxis;
    private YAxis yLeftAxis;
    private YAxis yRightAxis;
    Description chartDescription;

    private int dataLengthInt= 40; ;//this nr is dynamic, moduluse rounded down so that the nr is divisible by 4. It holds the Size of incoming data array (e.g 1000 bytes)4
    private int dataLengthMinInt = 400;
    private int thisPlotInt = 0;
    private int maxPlotInt = 0;
    private Instant timestampControlStartTime; //Set when "CTRL_START_CTRL" is sent to MCU.
    private Instant timestampControlEndTime;  //Set when data is received back.
    private Duration timestampControlDuration; //Calculated the "duration". Which can be used to calculate the approximate time between datapoints.
    private int newSetpoint = 130; //Start-Condition, same as AVR.
    private IDataSet<Entry> iDataSet;
    private ScatterDataSet PV_LineDataSet; //Process Variable (e.g. mm distance)
    private ScatterDataSet CV_LineDataSet; //Control Variable
    private ScatterDataSet set1, set2;

    private SeekBar setPointSeekBar;
    private TextView tvSetPoint;

    //MP_PlotValues
    private ArrayList<Entry> PID_OutputValuesArrayList = new ArrayList<>();
    private ArrayList<Entry>  PID_PVValuesArrayList = new ArrayList<>();

    private float mPID_Kp_MCU, mPID_Ki_MCU, mPID_Kd_MCU;

    //clsBallControllerDataObjects - Which are stored and can be plotted
    private clsBallControllerData mBallControllerDataSelected;
    private List<clsBallControllerData> mBallControllerDataObjectList = new ArrayList<>();

    //TEST NEW TCP CLIENT
    private Button newTCP_Button;
    private TCP_ClientMain newTCP_ClientMain;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        //FIREBASE
        mFbDb = FirebaseDatabase.getInstance();
        mFbDbRef = mFbDb.getReference();
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = currentDateTime.format(formatter);
        timestampControlStartTime = Instant.now(); //NB: Important to set this variable, or else the "duration calculation" will crash the app.

        //Testing
        mFbDbRef.child("Test2").setValue("AVR WriteTest");
        mFbDbRef.child("Test").setValue("HI There TESTING2 Kristian: " + formattedDateTime);
        //mFbDbRef.child("balancerData").child("ConnectionIP").child("IP02").setValue("Hello - " + formattedDateTime);

        // GET DATA FROM FIREBASE
        sPopulateBallControllerDataObjectListFromFireBase();


        //End testing - delete at some point


        //VIEWS
        mTCP_MessageReceived = "No TCP Message Received";
        mTCP_MessageSent = "No TCP Message Sent";

        //balancerCtrlView
        mBalancerCtrlView = findViewById(R.id.ballBalancerCtrlView);
        mBalancerCtrlView.setBallBalancerCtrl_ViewEventListener(new ballBalancerCtrlView.BallBalancerCtrlViewEventListenerInterface() {
            @Override
            public void onSendMessageToTCP_Server(String msg) {
                if (msg.contains("CTRL_START_CTRL")){
                    //Notes down timestamp
                    timestampControlStartTime = Instant.now(); //Used to calc duration of 1000 datapoints (or however long).
                }
                mTCPview.sendTCP_StringToServer(msg); //Sends string to TCP server, e.g. "SET_Kp_0.2_Set_Kp"
            }

            @Override
            public void onButtonPressedEvent(String buttonTagStr) {
                if (buttonTagStr.contains("PlotNext")){

                    maxPlotInt = mBallControllerDataObjectList.size();
                    thisPlotInt += 1;
                    if (thisPlotInt >= maxPlotInt){
                        thisPlotInt = 0;
                    }
                    else {

                    }

                    mBallControllerDataSelected = mBallControllerDataObjectList.get(thisPlotInt);
                    sPlotData(mBallControllerDataSelected);
                    mBalancerCtrlView.setBallControllerData(mBallControllerDataSelected);
                }
                else if (buttonTagStr.contains("PlotPrev")){
                    maxPlotInt = mBallControllerDataObjectList.size();
                    thisPlotInt -= 1;
                    if (thisPlotInt < 0){
                        thisPlotInt = maxPlotInt-1;
                    }
                    else {

                    }

                    mBallControllerDataSelected = mBallControllerDataObjectList.get(thisPlotInt);
                    sPlotData(mBallControllerDataSelected);
                    mBalancerCtrlView.setBallControllerData(mBallControllerDataSelected);
                }
            }

        });

        //TCP_View
        mTCPview = findViewById(R.id.TCPview);
        mTCPview.setTCP_ViewEventListener(new TCPview.TCPviewEventListenerInterface() {

            @Override
            public void onServerMessageReceived(String msg) {
                //Decode Message
                if (msg.length() >= dataLengthInt-1) //From Controller --> To plot behaviour of controller
                {

                }
                else if (msg.contains("ACQ_DISTANCE"))
                {

                }
                else if (msg.contains("Kp"))
                {
                    //Code to set Kp, so that we can store the value

                }
                else if (msg.contains("Kd"))
                {
                    //Code to set Kp, so that we can store the value
                }
                else if (msg.contains("Ki"))
                {
                    //Code to set Kp, so that we can store the value
                }

                Snackbar snackbar2 = Snackbar.make(findViewById(R.id.TCPview),"Msg Length: " + String.valueOf(msg.length()) + " Received",Snackbar.LENGTH_LONG);
                snackbar2.show();

            }

            @Override
            public void onServerBytesReceived(byte[] bytes) {

                if (bytes.length >= dataLengthMinInt-1) //From Controller --> To plot behaviour of controller
                {
                    timestampControlEndTime = Instant.now();
                    timestampControlDuration = Duration.between(timestampControlStartTime,timestampControlEndTime);

                    dataLengthInt = bytes.length;
                    int dataLengthIntReminder = dataLengthInt % 4;
                    if (dataLengthIntReminder == 0) {

                    } else if (dataLengthIntReminder <= 2) {
                        dataLengthInt = dataLengthInt - dataLengthIntReminder; // Round down
                    } else {
                        //dataLengthInt = dataLengthInt + (4 - dataLengthIntReminder); // Round up
                    }

                    //StringToByteArray
                    byte[] byteArr = bytes;
                    double[] ansDbl = new double[dataLengthInt/2];
                    int k = 0;

                    for (int i = 0; i < dataLengthInt; i = i+2) //Takes every two consecutive bytes and joins them to one 16bit unsigned char.
                    {
                        char short1,short2, ans;

                        short1 = 0x0000;
                        short2 = 0x0000;
                        short1 = (char)((byteArr[i] & 0xFF) << 8); //Left shifts byte by 8 bits into unsigned char variable (char is by default unsigned in android)
                        short2 = (char)(byteArr[i+1] & 0xFF);
                        ans = (char)(short1 | short2); //adjoins short1 and short2 into final variable (NB: Android char = 16bits, for handlig of only positive numbers)

                        ansDbl[k]= (double)ans;
                        k++;
                    }

                    Snackbar snackbar = Snackbar.make(findViewById(R.id.TCPview),"Byte Length: " + String.valueOf(bytes.length) + " Received",Snackbar.LENGTH_LONG);
                    snackbar.show();

                    double[] PID_OutPutArray = new double[dataLengthInt/4];
                    double[] PID_PV_Array = new double[dataLengthInt/4];

                    k=0;
                    for (int i = 0; i < dataLengthInt/2; i = i+2) //Goes through andDbl array of length datalength/2
                    {

                        PID_OutPutArray[k] = ansDbl[i];
                        PID_PV_Array[k]    = ansDbl[i+1];

                        k+=1;
                        //Get PID OutPut Values - Servo Pos
                        //Get PID PV Values - Distance
                    }
                    //Get PID OutPut Values - Servo Pos
                    //Get PID PV Values - Distance

                    //Set up Object mBallControllerData, and add to global collection of such objects
                    mBallControllerDataSelected = new clsBallControllerData();
                    mBallControllerDataObjectList.add(mBallControllerDataSelected);
                    mBallControllerDataSelected.setPID_Kp(mBalancerCtrlView.getKp_val());
                    mBallControllerDataSelected.setPID_Ki(mBalancerCtrlView.getKi_val());
                    mBallControllerDataSelected.setPID_Kd(mBalancerCtrlView.getKd_val());
                    mBallControllerDataSelected.setPID_SetPoint((float)newSetpoint);
                    mBallControllerDataSelected.setPID_RawOutPutArray(PID_OutPutArray);     //Creates plottable float arrays and prepares MPChart data objects which can be plotted.
                    mBallControllerDataSelected.setPV_RawOutPutArray(PID_PV_Array);         //Creates plottable float arrays and prepares MPChart data objects which can be plotted.
                    mBallControllerDataSelected.setDurationOfDataCollection(timestampControlDuration);

                    //PLOTS THE DATA IN mBallControllerDataSelected
                    sPlotData(mBallControllerDataSelected);

                }
            }

            @Override
            public void onMessageSentToServer(String msg) {

            }


        });

        //MP_lineChart
        scatterChart = (ScatterChart)findViewById(R.id.mpChart);

        CustomMarkerView customMarkerView = new CustomMarkerView(this, R.layout.layout_custom_marker_view);
        scatterChart.setMarker(customMarkerView); //Responsible to show value of selected point (see class - CustomMarkerView)


        //lineChart.setBackgroundColor();
        //lineChart.getDescription().setEnabled(false);

        //Linechart - X-axis description
        scatterChart.getDescription().setEnabled(true);
        chartDescription = scatterChart.getDescription();
        chartDescription.setText("Approx 20ms Intervals");
        chartDescription.setTextSize(10f);
        chartDescription.setXOffset(20);
        chartDescription.setYOffset(5);

        scatterChart.getXAxis().setEnabled(true);
        xAxis = scatterChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMaximum((float)dataLengthInt/4);
        xAxis.setAxisMinimum(0f);
        xAxis.enableGridDashedLine(0f, 10f, 0f); //LineLength 0f = no line


        scatterChart.getAxisLeft().setEnabled(true); //SET1 - PV_Values
        yLeftAxis = scatterChart.getAxisLeft();
        yLeftAxis.setAxisMaximum(500f); //310 mm MAX
        yLeftAxis.setAxisMinimum(0f);  // 25 mm MIN
        yLeftAxis.enableGridDashedLine(0f,5f, 0f); //LineLength 0f = no line (only dots)
        yLeftAxis.setTextColor(Color.RED);

        scatterChart.getAxisRight().setEnabled(true); //SET2 - Output_Values
        yRightAxis = scatterChart.getAxisRight();
        yRightAxis.setAxisMaximum(1500f);
        yRightAxis.setAxisMinimum(0f);
        yRightAxis.enableGridDashedLine(5f,7f, 0f);
        yRightAxis.setTextColor(ColorTemplate.getHoloBlue());

        scatterChart.setTouchEnabled(true);

        //scatterChart.setDragEnabled(true);
        scatterChart.setDragDecelerationEnabled(true);
        scatterChart.setDragDecelerationFrictionCoef(0.9f);

        scatterChart.setScaleEnabled(true);
        scatterChart.setScaleXEnabled(true);
        scatterChart.setScaleYEnabled(false);
        scatterChart.setDrawGridBackground(false);
        scatterChart.setHighlightPerDragEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
        scatterChart.setPinchZoom(true);
        scatterChart.animateX(1500);

        Legend l = scatterChart.getLegend();
        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setTypeface(Typeface.DEFAULT);
        l.setTextSize(12f);
        l.setTextColor(Color.BLACK);
        //l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        //l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setXOffset(35f);
        l.setYOffset(10f);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(true);

        scatterChart.notifyDataSetChanged();
        scatterChart.invalidate();

        scatterChart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartLongPressed(MotionEvent me) {
                Snackbar.make(scatterChart, "Aqcuire Data?", Snackbar.LENGTH_SHORT).setAction("YES", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        //CAN DELETE BELOW - TESTING!!
                        clsKrisMath krisMath = new clsKrisMath();

                        double[] x_vect = new double[mBallControllerDataSelected.getMpcPID_ScatterDataSet().getEntryCount()];
                        double[] y_vect = new double[mBallControllerDataSelected.getMpcPV_ScatterDataSet().getEntryCount()];

                        int entry_int = 0;

                        ArrayList<Entry> dataSetEntryArrayList = new ArrayList<>();
                        dataSetEntryArrayList.addAll(mBallControllerDataSelected.getMpcPV_ScatterDataSet().getValues());

                        for (entry_int = 0;entry_int < mBallControllerDataSelected.getMpcPV_ScatterDataSet().getEntryCount();entry_int+=1) {

                            //PID_OutputValuesArrayList.add(new Entry(lastPlotInt, val)); //Set2
                            Entry thisEntry = new Entry();
                            thisEntry = mBallControllerDataSelected.getMpcPV_ScatterDataSet().getEntryForIndex(entry_int);
                            x_vect[entry_int] = (double)thisEntry.getX();
                            y_vect[entry_int] = (double)thisEntry.getY();
                        }

                        double x_val;
                        x_val = 92.5;

                        double[] ans = krisMath.khjCubicSplineEngDetails(x_val,x_vect,y_vect); //computes estimated value at given X.

                        Toast.makeText(scatterChart.getContext(), "x_val: " + String.valueOf(x_val) + " has y-val: " + String.format("%.3f",ans[0]) + " found in: " + String.format("%.0f",ans[1]) + " iterations", Toast.LENGTH_SHORT).show();


                    }
                }).show();

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {

                //TEST
                AlertDialog.Builder builder = new AlertDialog.Builder(scatterChart.getContext());
                builder.setTitle("Export to firebase - Input DataName");

                // Set up the input
                final EditText input = new EditText(scatterChart.getContext());
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String mText = input.getText().toString();
                        mBallControllerDataSelected.exportDataToFirebase(mText);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
                String showSeconds;
                showSeconds = String.valueOf(mBallControllerDataSelected.getDurationOfDataCollectionInSeconds());
                //TEST END

            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {

            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {

            }
        });

        //End scatterChart

        //SeekBar
        setPointSeekBar = (SeekBar) findViewById(R.id.seekBar_setPoint);
        //Range Required: 70 --> 200mm
        int rangeHigh = 200;
        int rangeLow = 70;
        double range_step = 1.0;
        setPointSeekBar.setMax((int)((double)(rangeHigh-rangeLow)/range_step));
        setPointSeekBar.setProgress(newSetpoint-rangeLow,true);

        setPointSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                newSetpoint = rangeLow + i;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Snackbar snackbar = Snackbar.make(findViewById(R.id.TCPview),"Update SetPoint to: " + String.valueOf(newSetpoint) + " mm",Snackbar.LENGTH_LONG).setAction("UPDATE", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mTCPview.sendTCP_StringToServer("SETPOINT_" + String.valueOf(newSetpoint) + "_SETPOINT");
                        tvSetPoint.setText("SetPoint: " + String.valueOf(newSetpoint) + " mm");

                    }
                });
                snackbar.show();
            }
        });

        tvSetPoint = (TextView) findViewById(R.id.tv_setpoint);
        tvSetPoint.setText("SetPoint: " + String.valueOf(newSetpoint) + " mm");

    }

    @Override
    protected void onRestart() {
        super.onRestart();

    }

    private void UpdateSetpointOnGraph(double newSetpoint)
    {


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

    //TESTING SECTION
    private void sPopulateBallControllerDataObjectListFromFireBase(){

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("balancerData");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot balancerDataSnapshot : snapshot.getChildren())
                    {
                        String dataKey = balancerDataSnapshot.getKey();
                        Double PID_Kd = balancerDataSnapshot.child("PID_Kd").getValue(Double.class);
                        Double PID_Ki = balancerDataSnapshot.child("PID_Ki").getValue(Double.class);
                        Double PID_Kp = balancerDataSnapshot.child("PID_Kp").getValue(Double.class);
                        Double setPoint = balancerDataSnapshot.child("PID_SetPoint").getValue(Double.class);
                        List<Double> PID_Values = (List<Double>) balancerDataSnapshot.child("PID_Values").getValue();
                        List<Double> PV_Values = (List<Double>) balancerDataSnapshot.child("PV_Values").getValue();
                        Log.d("Firebase", "Hobbies: " + PV_Values);

                        clsBallControllerData newData = new clsBallControllerData();
                        newData.setBallCtrlDataName(dataKey);
                        newData.setPID_Kd(PID_Kd);
                        newData.setPID_Ki(PID_Ki);
                        newData.setPID_Kp(PID_Kp);
                        newData.setPID_SetPoint(setPoint);

                        newData.setPV_OutPutArrayFromFirebase(PV_Values);
                        newData.setPID_OutPutArrayFromFirebase(PID_Values);

                        mBallControllerDataObjectList.add(newData); //Populates the collection with data from firebase. Dataitems can be pltted using.

                    }

                    thisPlotInt = 0;
                    mBallControllerDataSelected = mBallControllerDataObjectList.get(thisPlotInt);
                    sPlotData(mBallControllerDataSelected);
                }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("Firebase", "Error: " + error.getMessage());
            }
        });

    }

    private void sPlotData(clsBallControllerData dataToPlot){

        //Adjust y-axes
        scatterChart.getAxisLeft().setAxisMaximum(dataToPlot.getMpcPV_ScatterDataSet().getYMax()*1.2f);
        scatterChart.getAxisLeft().setAxisMinimum(0);
        scatterChart.getAxisRight().setAxisMaximum(dataToPlot.getMpcPID_ScatterDataSet().getYMax()*1.2f);
        scatterChart.getAxisRight().setAxisMinimum(0);

        xAxis.setAxisMaximum((float)dataToPlot.getMpcPV_ScatterDataSet().getEntryCount()+10); //I need this, ele datalengthInt = 0

        //Adjusts x-axis view when new data comes in.
        scatterChart.setData(dataToPlot.getMpcScatterData());
        scatterChart.getScatterData().notifyDataChanged();
        scatterChart.notifyDataSetChanged();
        scatterChart.animateX(500);
        scatterChart.invalidate();

    }


}
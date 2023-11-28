package com.example.avr_ballbalancerapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private TCPview mTCPview;
    private String mTCP_MessageReceived;
    private String mTCP_MessageSent;

    private ballBalancerCtrlView mBalancerCtrlView;

    //Firebase Declarations
    private FirebaseDatabase mFbDb;
    private DatabaseReference mFbDbRef;

    //MPandroidChart
    private LineChart lineChart;
    private XAxis xAxis;
    private YAxis yLeftAxis;
    private YAxis yRightAxis;
    Description chartDescription;

    private int dataLengthInt = 2000; //Size of incoming data array (e.g 1000 bytes)
    private int lastPlotInt = 0; //I.e. is incremented each time new data arrives
    private int newSetpoint = 130; //Start-Condition, same as AVR.
    private IDataSet<Entry> iDataSet;
    private LineDataSet PV_LineDataSet; //Process Variable (e.g. mm distance)
    private LineDataSet CV_LineDataSet; //Control Variable
    private LineDataSet set1, set2, SP_LineDataSet;

    private SeekBar setPointSeekBar;
    private TextView tvSetPoint;

    //MP_PlotValues
    private ArrayList<Entry> PID_OutputValuesArrayList = new ArrayList<>();
    private ArrayList<Entry>  PID_PVValuesArrayList = new ArrayList<>();
    private ArrayList<Entry> PID_SPValuesArrayList = new ArrayList<>();

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

        mFbDbRef.child("Test2").setValue("HI There Test from AVR");
        mFbDbRef.child("Test").setValue("HI There TESTING2 Kristian: " + formattedDateTime);

        //VIEWS
        mTCP_MessageReceived = "No TCP Message Received";
        mTCP_MessageSent = "No TCP Message Sent";

        //balancerCtrlView
        mBalancerCtrlView = (ballBalancerCtrlView)findViewById(R.id.ballBalancerCtrlView);
        mBalancerCtrlView.setBallBalancerCtrl_ViewEventListener(new ballBalancerCtrlView.BallBalancerCtrlViewEventListenerInterface() {
            @Override
            public void onSendMessageToTCP_Server(String msg) {
                mTCPview.sendTCP_StringToServer(msg); //Sends string to TCP server, e.g. "SET_Kp_0.2_Set_Kp"
            }
        });

        //TCP_View
        mTCPview = (TCPview)findViewById(R.id.TCPview);
        mTCPview.setTCP_ViewEventListener(new TCPview.TCPviewEventListenerInterface() {

            @Override
            public void onServerMessageReceived(String msg) {
                //Decode Message
                if (msg.length() >= dataLengthInt-1) //From Controller --> To plot behaviour of controller
                {

                }
                else if (msg.contains("ACQ_DISTANCE")){

                }
                else if (msg.length() == 40) //Test
                {

                    //StringToByteArray
                    byte[] byteArr = msg.getBytes();
                    double[] ansDbl = new double[20];
                    int k = 0;

                    for (int i = 0; i < 40; i = i+2)
                    {
                        char short1,short2, ans;
                        short1 = 0x0000;
                        short2 = 0x0000;
                        short1 = (char)((byteArr[i] & 0xFF) << 8); //Left shifts byte by 8 bits into short variable
                        short2 = (char)(byteArr[i+1] & 0xFF);
                        ans = (char)(short1 | short2); //adjoins short1 and short2 into final variable

                        ansDbl[k]= (double)ans;
                        k++;
                    }


                    Snackbar snackbar = Snackbar.make(findViewById(R.id.TCPview),"Msg Length: " + String.valueOf(msg.length()) + " Received",Snackbar.LENGTH_LONG);
                    snackbar.show();

                    double[] PID_OutPutArray = new double[10];
                    double[] PID_PV_Array = new double[10];
                    k=0;
                    for (int i = 0; i < 20; i = i+2)
                    {

                        PID_OutPutArray[k] = ansDbl[i]*(lastPlotInt+1);
                        PID_PV_Array[k]    = ansDbl[i+1]*(lastPlotInt+2);
                        //Get PID OutPut Values - Servo Pos
                        //Get PID PV Values - Distance
                        PID_PVValuesArrayList.add(new Entry(lastPlotInt, (float)PID_OutPutArray[k])); //Set1 - Calculated value
                        PID_OutputValuesArrayList.add(new Entry(lastPlotInt, (float)PID_PV_Array[k])); //Set1 - Calculated value
                        k++;
                        lastPlotInt++;
                    }
                    //Get PID OutPut Values - Servo Pos
                    //Get PID PV Values - Distance


                    setData(100, 100);
                    lineChart.invalidate();


                }
                Snackbar snackbar2 = Snackbar.make(findViewById(R.id.TCPview),"Msg Length: " + String.valueOf(msg.length()) + " Received",Snackbar.LENGTH_LONG);
                snackbar2.show();

            }

            @Override
            public void onServerBytesReceived(byte[] bytes) {

                if (bytes.length >= dataLengthInt-1) //From Controller --> To plot behaviour of controller
                {

                    //StringToByteArray
                    byte[] byteArr = bytes;
                    double[] ansDbl = new double[dataLengthInt/2];
                    int k = 0;

                    for (int i = 0; i < dataLengthInt; i = i+2) //Takes every two consecutive bytes and joins them to one 16bit usigned char.
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

                    for (int i = 0; i < dataLengthInt/4; i++) {
                        float val = (float)PID_OutPutArray[i];
                        PID_OutputValuesArrayList.add(new Entry(lastPlotInt, val)); //Set2

                        //Set1 - PV = Distance - Sensor Calculation
                        float val2 = (float)PID_PV_Array[i];
                        //PID_PVValuesArrayList.add(new Entry(lastPlotInt, val2)); //Set1 No calculation

                        double PV_val_dbl1;
                        val2 = val2/1024*5;
                        PV_val_dbl1 = sharp_get_mm_from_volt((double)val2);
                        PID_PVValuesArrayList.add(new Entry(lastPlotInt, (float)PV_val_dbl1)); //Set1 - Calculated value

                        lastPlotInt++;

                    }

                    //Adjusts x-axis view when new data comes in.

                    setData(100, 100);

                    lineChart.invalidate();

                }
            }

            @Override
            public void onMessageSentToServer(String msg) {

            }


        });

        //MP_lineChart
        lineChart = (LineChart)findViewById(R.id.mpChart);

        CustomMarkerView customMarkerView = new CustomMarkerView(this, R.layout.layout_custom_marker_view);
        lineChart.setMarker(customMarkerView); //Responsible to show value of selected point (see class - CustomMarkerView)


        //lineChart.setBackgroundColor();
        //lineChart.getDescription().setEnabled(false);

        //Linechart - X-axis description
        lineChart.getDescription().setEnabled(true);
        chartDescription = lineChart.getDescription();
        chartDescription.setText("20ms Intervals");
        chartDescription.setTextSize(10f);
        chartDescription.setXOffset(20);
        chartDescription.setYOffset(5);

        lineChart.getXAxis().setEnabled(true);
        xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMaximum((float)dataLengthInt/4);
        xAxis.setAxisMinimum(0f);
        xAxis.enableGridDashedLine(0f, 10f, 0f); //LineLength 0f = no line


        lineChart.getAxisLeft().setEnabled(true); //SET1 - PV_Values
        yLeftAxis = lineChart.getAxisLeft();
        yLeftAxis.setAxisMaximum(310f); //310 mm MAX
        yLeftAxis.setAxisMinimum(25f);  // 25 mm MIN
        yLeftAxis.enableGridDashedLine(0f,5f, 0f); //LineLength 0f = no line (only dots)
        yLeftAxis.setTextColor(ColorTemplate.getHoloBlue());

        lineChart.getAxisRight().setEnabled(true); //SET2 - Output_Values
        yRightAxis = lineChart.getAxisRight();
        yRightAxis.setAxisMaximum(2500f);
        yRightAxis.setAxisMinimum(500f);
        yRightAxis.enableGridDashedLine(5f,7f, 0f);
        yRightAxis.setTextColor(Color.RED);

        lineChart.setTouchEnabled(true);

        //lineChart.setDragEnabled(true);
        lineChart.setDragDecelerationEnabled(true);
        lineChart.setDragDecelerationFrictionCoef(0.9f);

        lineChart.setScaleEnabled(true);
        lineChart.setScaleXEnabled(true);
        lineChart.setScaleYEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setHighlightPerDragEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
        lineChart.setPinchZoom(true);
        lineChart.animateX(1500);

        Legend l = lineChart.getLegend();
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

        lineChart.invalidate();

        lineChart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartLongPressed(MotionEvent me) {
                Snackbar.make(lineChart, "Aqcuire Data?", Snackbar.LENGTH_SHORT).setAction("YES", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clsKrisMath krisMath = new clsKrisMath();

                        double[] x_vect = new double[PV_LineDataSet.getEntryCount()];
                        double[] y_vect = new double[PV_LineDataSet.getEntryCount()];

                        int entry_int = 0;

                        ArrayList<Entry> dataSetEntryArrayList = new ArrayList<>();
                        dataSetEntryArrayList.addAll(PV_LineDataSet.getValues());

                        for (entry_int = 0;entry_int < PV_LineDataSet.getEntryCount();entry_int+=1) {

                            //PID_OutputValuesArrayList.add(new Entry(lastPlotInt, val)); //Set2
                            Entry thisEntry = new Entry();
                            thisEntry = PV_LineDataSet.getEntryForIndex(entry_int);
                            x_vect[entry_int] = (double)thisEntry.getX();
                            y_vect[entry_int] = (double)thisEntry.getY();
                        }

                        double x_val;
                        x_val = 92.5;

                        double[] ans = krisMath.khjCubicSplineEngDetails(x_val,x_vect,y_vect); //computes estimated value at given X.

                        Toast.makeText(lineChart.getContext(), "x_val: " + String.valueOf(x_val) + " has y-val: " + String.format("%.3f",ans[0]) + " found in: " + String.format("%.0f",ans[1]) + " iterations", Toast.LENGTH_SHORT).show();


                    }
                }).show();

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {

                Snackbar.make(lineChart, "Extend X-axis to 0?", Snackbar.LENGTH_SHORT).setAction("YES", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        xAxis.setAxisMinimum(0); //Ensures we can see the entire plot
                        lineChart.invalidate();
                        lineChart.setScaleMinima(0f, 0f);
                        lineChart.fitScreen();
                        lineChart.invalidate();
                    }
                }).show();

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

        //End LineChart

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

    private void setData(int count, float range) {

        //Adjusting X-Axis for incoming data
        if (xAxis.getAxisMaximum() < (float)lastPlotInt){
            xAxis.setAxisMinimum(xAxis.getAxisMaximum()-(float)dataLengthInt/4);
            xAxis.setAxisMaximum(xAxis.getAxisMaximum() + (float)dataLengthInt/4);
        }

        if (lineChart.getData() != null && lineChart.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) lineChart.getData().getDataSetByIndex(0);
            set2 = (LineDataSet) lineChart.getData().getDataSetByIndex(1);

            Collections.sort(PID_PVValuesArrayList, new EntryXComparator());
            set1.setValues(PID_PVValuesArrayList);

            Collections.sort(PID_OutputValuesArrayList, new EntryXComparator());
            set2.setValues(PID_OutputValuesArrayList);

            lineChart.getData().notifyDataChanged();
            lineChart.notifyDataSetChanged();
         } else {
            // create a dataset and give it a type.
            set1 = new LineDataSet(PID_PVValuesArrayList, "PROCESS VARIABLE"); //BLUE
            PV_LineDataSet = set1;
            set1.setAxisDependency(YAxis.AxisDependency.LEFT);
            set1.setColor(ColorTemplate.getHoloBlue());
            set1.setCircleColor(ColorTemplate.getHoloBlue());
            set1.setLineWidth(2f);
            set1.enableDashedLine(0f,2f,0f);
            set1.setCircleRadius(3f);
            set1.setFillAlpha(65);
            set1.setFillColor(ColorTemplate.getHoloBlue());
            set1.setHighLightColor(Color.rgb(244, 117, 117));
            set1.setDrawCircleHole(true);
            //set1.setFillFormatter(new MyFillFormatter(0f));
            //set1.setDrawHorizontalHighlightIndicator(false);
            //set1.setVisible(false);
            //set1.setCircleHoleColor(Color.WHITE);

            // create a dataset and give it a type
            set2 = new LineDataSet(PID_OutputValuesArrayList, "CV [PWM]");
            CV_LineDataSet = set2;
            set2.setAxisDependency(YAxis.AxisDependency.RIGHT);
            set2.setColor(Color.RED);
            set2.setCircleColor(Color.RED);
            set2.setLineWidth(2f);
            set2.enableDashedLine(0f,2f,0f);
            set2.setCircleRadius(3f);
            set2.setFillAlpha(65);
            set2.setFillColor(Color.RED);
            set2.setDrawCircleHole(true);
            set2.setHighLightColor(Color.rgb(244, 117, 117));
            //set2.setFillFormatter(new MyFillFormatter(900f));



            // create a data object with the data sets
            LineData data = new LineData(set1, set2);//,SP_LineDataSet);

            data.setValueTextColor(Color.WHITE);
            data.setValueTextSize(9f);

            // set data
            lineChart.setData(data);
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

    //TESTING SECTION



}
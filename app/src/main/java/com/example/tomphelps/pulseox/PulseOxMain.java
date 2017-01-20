package com.example.tomphelps.pulseox;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tomphelps.pulseox.PulseOxProcessing.PpgExtractor;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class PulseOxMain extends AppCompatActivity {
    private CountDownTimer timer;
    private MicManager micManager = new MicManager();
    private boolean soundFlag = false;
    private OutputGenerator outputGenerator = new OutputGenerator(200, 1, .5);
    private BurstSignalGenerator burstSignalGenerator;
    private SquareSignalGen squareSignalGen = new SquareSignalGen(200, 1, .5);
    public static LineChart lineChart;
    Timer chartTimer = new Timer();
    FrameLayout frameLayout;


    private static TextView hrText;
    private static TextView spo2Text;
    private static RingBuffer spO2Buffer;
    private static RingBuffer hrBuffer;
    private static Chart irChart;
    private static int handleCount=0;
    public static Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            if(handleCount<100){
                handleCount++;
            }
            else {
                irChart.add((double) msg.obj);
            }
        }
    };
    public static Handler dataHandler = new Handler(){
        public void handleMessage(Message msg){
            double heartRate =0;
            switch (msg.what){

                case 0:
                    spO2Buffer.add((double)msg.obj);
                    break;
                case 1:
                    heartRate=(double)msg.obj;
                    break;
            }
            double spo2 = spO2Buffer.averageBuffer()*-29.97+124.5;
            spo2=spo2>100?100:spo2;
            hrText.setText(String.format("%.0f",heartRate)+" bpm");
            spo2Text.setText(String.format("%.0f",spo2)+"%");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse_ox_main);
        frameLayout = (FrameLayout) findViewById(R.id.layoutFrame);
        frameLayout.addView(getLayoutInflater().inflate(R.layout.content_pulse_ox_main, null));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setUpFreqSlider();
        micManager.context=this.getApplicationContext();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String output;
                if (soundFlag) {
                    output = "Signals turned off";
                } else {
                    output = "Signals turned on";
                }
                Snackbar.make(view, output, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                startSampling();
            }
        });
    }

    private boolean noNullFlag=false;
    public void noNullTestStart(View view){
        String output="";
        updateValues();
        if(!noNullFlag){
            PlaySine.start();
            updateValues();
            squareSignalGen.start();
            micManager.recordData("NoNullTestWave");
            micManager.phoneTestFlag=true;
            noNullFlag=true;
            output="Test Started";
        }
        else{
            try{
                PlaySine.stop();
                squareSignalGen.stop();
                micManager.stopRecording();
                noNullFlag=false;
                micManager.stopData();
                micManager.phoneTestFlag=false;
                output="Test Stopped";
            }
            catch (Exception e){
            }
        }
        Snackbar.make(view, output, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }
    private boolean normalTestFlag=false;
    public void normalTestStart(View view){
        updateValues();
        String output="";
        if(!normalTestFlag){
            PlaySine.start();
            updateValues();
            output="Test Started";
            outputGenerator.start();
            micManager.phoneTestFlag=true;
            micManager.recordData("NormalTestWave");
            normalTestFlag=true;
        }
        else{
            try{
                PlaySine.stop();
                output="Test Stopped";
                outputGenerator.stop();
                normalTestFlag=false;
                micManager.stopRecording();
                micManager.stopData();
                micManager.phoneTestFlag=false;
            }
            catch (Exception e){
            }
        }
        Snackbar.make(view, output, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }
    private void updateValues(){
        EditText freqText= (EditText) findViewById(R.id.pulseOxFreqEditBox);
        double freq = Double.parseDouble(freqText.getText().toString());
        EditText dutyCycleText= (EditText) findViewById(R.id.dutyCycleEditbox);
        double dutycycle = Double.parseDouble(dutyCycleText.getText().toString());
        EditText amplitudeText= (EditText) findViewById(R.id.amplitudeEditBox);
        double amplitdueText = Double.parseDouble(amplitudeText.getText().toString());
        outputGenerator.updateValues(freq, amplitdueText, dutycycle);
        squareSignalGen.updateValues(freq,amplitdueText,dutycycle);
    }

    private void startSampling() {

        if (!soundFlag) {
//            burstSignalGenerator.start();
            updateValues();;
//            micManager.recordData();
            micManager.startRecording(false);
            outputGenerator.start();

//            micManager.processPPG(true);
            handleCount=0;
            PlaySine.start();
            soundFlag=true;
//            startDataPlotting();
            frameLayout.removeAllViews();

            frameLayout.addView(getLayoutInflater().inflate(R.layout.content_pulse_ox_plotting, null));
            irChart = new Chart(this, (LinearLayout) findViewById(R.id.irChart), "ir", Color.parseColor("#FF24BDFF"));
            spo2Text = (TextView)findViewById(R.id.spo2_text);
            hrText = (TextView)findViewById(R.id.hr_text);
            hrBuffer = new RingBuffer(20);
            spO2Buffer= new RingBuffer(20);

        } else {

            try {
                PlaySine.stop();
                burstSignalGenerator.stop();
            }catch (Exception e){
            }
            try {
                outputGenerator.stop();
            }catch (Exception e){
            }
            soundFlag=false;
//            DataManager.PrintVals();
//            DataManager.clearFields();
//            timer.cancel();
//            chartTimer.cancel();
            try {
                micManager.stopRecording();
//                micManager.stopData();
//                micManager.stopPPGRecording();
            }catch (Exception e){

            }
            frameLayout.removeAllViews();
            frameLayout.addView(getLayoutInflater().inflate(R.layout.content_pulse_ox_main,null));
            setUpFreqSlider();

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pulse_ox_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public void setUpFreqSlider() {
        //sets up the freq slider.
        int startFeq = 14000;
        int initProg = 2000;
        final int freqOffset = startFeq-initProg;
        SeekBar seekBar = (SeekBar) findViewById(R.id.sineFreqSeekBar);
        final TextView freqView = (TextView) findViewById(R.id.sineFreqText);
        seekBar.setMax(6000);//was 6k
        seekBar.setProgress(initProg);
        freqView.setText("Frequency: " + (freqOffset + initProg));
        PlaySine.adjustFreq(startFeq);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                System.out.println("Current freq: " + (freqOffset + progress));
                freqView.setText( (freqOffset + progress)+" Hz");
                PlaySine.adjustFreq(freqOffset + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }
    private void prepData() {
        // create a data object with the datasets
        LineData data = PpgExtractor.data;
        System.out.println("Data size:"+data.getDataSetCount());
        // set data
        lineChart.setData(data);
        lineChart.notifyDataSetChanged();
        setUpChart();
    }
    private void setUpChart2(){

        // enable description text
        lineChart.getDescription().setEnabled(true);

        // enable touch gestures
        lineChart.setTouchEnabled(true);

        // enable scaling and dragging
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        lineChart.setPinchZoom(true);

        // set an alternative background color
        lineChart.setBackgroundColor(Color.LTGRAY);

        LineData data = new LineData();
        LineDataSet dataSet;
        dataSet = new LineDataSet(null, "ppg");
        dataSet.disableDashedLine();
        dataSet.setColor(Color.parseColor("#ff0091ff"));
        dataSet.setCircleColor(Color.parseColor("#ff0091ff"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleSize(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setValueTextSize(0);
        dataSet.setFillAlpha(100);
        dataSet.setFillColor(Color.parseColor("#ff0091ff"));
        dataSet.setDrawFilled(true);
        dataSet.addEntry(new Entry(0,0));
        data.addDataSet(dataSet);
        lineChart.setData(data);
//
//        LineData data = new LineData();
//        data.setValueTextColor(Color.WHITE);
//
//        // add empty data
//        lineChart.setData(data);

        // get the legend (only possible after setting data)
        XAxis xl = lineChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
    }
    private void setUpChart(){
        System.out.println("GOT TO SET UP");
        lineChart.setDrawGridBackground(false);
        // no description text

        // enable value highlighting
//        lineChart.setHighlightEnabled(true);

        // enable touch gestures
        lineChart.setTouchEnabled(true);

        // enable scaling and dragging
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        // lineChart.setScaleXEnabled(true);
        // lineChart.setScaleYEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
        lineChart.setPinchZoom(true);
//        lineChart.setVisibleXRangeMaximum(xVals.size());

        // set an alternative background color

        // create a custom MarkerView (extend MarkerView) and specify the layout
        // to use for it

        // set the marker to the chart

        // x-axis limit line

        XAxis xAxis = lineChart.getXAxis();
//        lineChart.setVisibleXRangeMaximum(1000);
//        lineChart.moveViewTo();
        xAxis.disableGridDashedLine();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setDrawGridLines(false);
        YAxis leftAxis = lineChart.getAxisLeft();
        //leftAxis.setYOffset(20f);
        leftAxis.disableGridDashedLine();

        // limit lines are drawn behind data (and not on top)
        leftAxis.setDrawLimitLinesBehindData(true);
        lineChart.getAxisRight().setEnabled(false);
        LineData data = new LineData();
        LineDataSet dataSet;
        dataSet = new LineDataSet(null, "ppg");
        dataSet.disableDashedLine();
        dataSet.setColor(Color.parseColor("#ff0091ff"));
        dataSet.setCircleColor(Color.parseColor("#ff0091ff"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleSize(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setValueTextSize(0);
        dataSet.setFillAlpha(100);
        dataSet.setFillColor(Color.parseColor("#ff0091ff"));
        dataSet.setDrawFilled(true);
        dataSet.addEntry(new Entry(0,0));
        data.addDataSet(dataSet);
        lineChart.setData(data);
    }

}

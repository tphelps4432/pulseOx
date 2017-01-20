package com.example.tomphelps.pulseox.PulseOxProcessing;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.os.Handler;

import com.example.tomphelps.pulseox.ParallelWriter;
import com.example.tomphelps.pulseox.PulseOxMain;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;


public class PpgExtractor {
    private ArrayList<Double> ppgRed = new ArrayList<Double>();
    private ArrayList<Double> ppgIR = new ArrayList<Double>();
    private ArrayList<Double> resultsList = new ArrayList<Double>();
    private ArrayList<Double> HRList = new ArrayList<Double>();
    private LowPassFilter redLPF = new LowPassFilter();
    private LowPassFilter irLPF = new LowPassFilter();
    private LineDataSet dataSet = new LineDataSet(null,"PPG");
    public static LineData data = new LineData();
    private int inputCount=0;

    private PpgProcessor processor = new PpgProcessor();

    private int operationCount = 0;
    private double sum = 0;
    private int pointCount = 0;
    private boolean diffFlag = false;
    private Handler handler;
    private Handler dataHandler;

    double diff = 0.0;
    ArrayList<Double> buffer = new ArrayList<Double>();
    int diffSkip = 20;
    int skipThresh = 50;
    double redSigVal = 0;
    double irSigVal = 0;

    public ArrayList<Double> getRedPpg() {
        return ppgRed;
    }

    public ArrayList<Double> getIRPpg() {
        return ppgIR;
    }

    public ArrayList<Double> getResultsList(){return resultsList;}

    public ArrayList<Double> getHRList(){return HRList;}



    int processCounter = 0;
    int windowSize = 500;

    double previousValue = 0;
    double previousDiff = 0;
    public PpgExtractor(){
        setUpDataSet();
        handler = PulseOxMain.handler;
        dataHandler = PulseOxMain.dataHandler;
    }
    public void processFilteredInput(double filteredInput) {
//		double filteredInput= preFilter.filterValue(input);
        double diff = filteredInput - previousValue;
//        if (recordRawSignal)
//            rawDataWriter.append(filteredInput + ",\r\n");
//		System.out.println(diff*previousDiff);
        if (diff * previousDiff < 0) {
            diffSkip = 0;
            diffFlag = true;
            operationCount++;
            operationCount = operationCount % 4;
        } else {
            diffSkip++;
        }
        if (diffFlag) {
            diffFlag = false;
            switch (operationCount) {
                case 0:
                    redSigVal = filteredInput;
                    break;
                case 1:
                    double val = -redLPF.filterValue(redSigVal-filteredInput);
                    ppgRed.add(val);
                    handler.obtainMessage(1, val).sendToTarget();
                    break;
                case 2:
                    irSigVal = filteredInput;
                    break;
                case 3:
                    ppgIR.add(-irLPF.filterValue(irSigVal - filteredInput));
                    processCounter++;
                    if (processCounter > windowSize) {
                        processCounter = 0;
                        double result = processor.getSp02(ppgIR.subList(ppgIR.size() - windowSize, ppgIR.size() - 1), ppgRed.subList(ppgRed.size() - windowSize, ppgRed.size() - 1));
                        HRList.add(processor.getHeartRate(true));
                        dataHandler.obtainMessage(0,result).sendToTarget();
                        dataHandler.obtainMessage(1,processor.getHeartRate(false)).sendToTarget();
                        System.out.println(processor.getHeartRate(false
                        ));
                        System.out.println(result);
                        resultsList.add(result);
                    }
                    break;
            }
        }
        previousDiff = diff;
        previousValue = filteredInput;
    }
    public void setUpDataSet(){
        dataSet = new LineDataSet(null, "pH Values");
        dataSet.disableDashedLine();
        dataSet.setColor(Color.parseColor("#ff0091ff"));
        dataSet.setCircleColor(Color.parseColor("#ff0091ff"));
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircleHole(true);
        dataSet.setValueTextSize(0);
        dataSet.setFillAlpha(100);
        dataSet.setFillColor(Color.parseColor("#ff0091ff"));
        dataSet.setDrawFilled(true);
        data.addDataSet(dataSet);
    }

}

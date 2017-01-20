package com.example.tomphelps.pulseox.PulseOxProcessing;

import android.support.v4.util.CircularArray;


import com.example.tomphelps.pulseox.RingBuffer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import static java.lang.Math.abs;


public class PpgProcessor {
    double irMean = 0;
    double redMean = 0;
    int redCrossings;
    int irCrossings;
    int firstCross = 0;
    int lastCross = 0;
    int lastI = 0;
    private RingBuffer crossBuffer = new RingBuffer(5);
    private RingBuffer sampleBuffer = new RingBuffer(5);

    //HR 2.0
    private SummaryStatistics hrStats = new SummaryStatistics();
    private RingBuffer hrSpacingBuffer = new RingBuffer(10);
    private int hrVar1 = 0;
    private int hrVar2 = 0;
    private int counter = 0;
    private double maxDel = 0;
    double prevRed = 0;
    int startUpSkip = 0;

    private PolynomialCurveFitter fitter = PolynomialCurveFitter.create(3);

    public double getHeartRate(boolean runTest) {

        double result = (redCrossings -1) / 2.0 / (lastCross - firstCross) * 200.0 * 60.0;
        double val = crossBuffer.averageBuffer() / 2 / sampleBuffer.averageBuffer() * 200 * 60;
//

        if (runTest) {
            crossBuffer.add(redCrossings);
//            sampleBuffer.add(lastCross - firstCross);
			sampleBuffer.add(500);
            double test = crossBuffer.averageBuffer() / 2 / sampleBuffer.averageBuffer() * 200 * 60;
//			System.out.println("Delta Samples: " + (lastCross - firstCross) + ", crossings: " + (irCrossings + redCrossings) / 2);
//			System.out.println("Standard HR: " + result + " Test HR:" + test);
        }
        double test = crossBuffer.averageBuffer() / 2 / sampleBuffer.averageBuffer() * 200 * 60;

        return test;
    }

    public double getSp02(List<Double> ir, List<Double> red) {
        irMean = 0;
        redMean = 0;
        firstCross = 0;
        lastCross = 0;
        irCrossings = 0;
        redCrossings = 0;
        SummaryStatistics hrStats = new SummaryStatistics();
        WeightedObservedPoints irPoints = new WeightedObservedPoints();
        WeightedObservedPoints redPoints = new WeightedObservedPoints();
        for (int i = 0; i < ir.size(); i++) {
            irPoints.add(i, ir.get(i));
            redPoints.add(i, red.get(i));
            redMean += red.get(i);
            hrStats.addValue(red.get(i));
            irMean += ir.get(i);
        }
//        redMean /= red.size();
        irMean /= ir.size();
        redMean = hrStats.getMax()-(hrStats.getMax()-hrStats.getMin())/2.0;
        double[] coeffIR = fitter.fit(irPoints.toList());
        double[] coeffRed = fitter.fit(redPoints.toList());
        SummaryStatistics irStats = new SummaryStatistics();
        SummaryStatistics redStats = new SummaryStatistics();
        double redVal;
        double irVal;
        double irTemp = ir.get(0);
        double redTemp = red.get(0);
        lastI = 0;
        for (int i = 0; i < ir.size(); i++) {
            //HR 2.0
//            if (startUpSkip>2) {
//                double del = abs(red.get(i) - prevRed);
////                System.out.println("Del: "+del+" Max Del:" + maxDel);
//                if (maxDel < del &&prevRed!=0)
//                    maxDel = del;
//                if (del > 0.6* maxDel) {
//                    if (abs(counter - hrVar2) > 50) {
//                        hrStats.addValue(counter);
//                        hrSpacingBuffer.add(counter);
//                        hrVar2 = counter;
//                        System.out.println(counter);
//                        counter = 0;
//
//                    }
//                }
//                counter++;
//                prevRed = red.get(i);
//            }
//			irVal = ir.get(i)-(Math.pow(i, 3)*coeffIR[3]+Math.pow(i, 2)*coeffIR[2]+i*coeffIR[1]+coeffIR[0]);
            irVal = ir.get(i) - irMean;
//			redVal=red.get(i)-(Math.pow(i, 3)*coeffRed[3]+Math.pow(i, 2)*coeffRed[2]+i*coeffRed[1]+coeffRed[0]);
            redVal = red.get(i) - redMean;
//			System.out.println(irVal);
            irStats.addValue(ir.get(i));
            redStats.addValue(red.get(i));
            if (irTemp * irVal < 0) {
                if (irCrossings == 0) {
                    firstCross = i;
                }
                irCrossings++;
                lastCross = lastCross + i - lastI;
                lastI = i;
            }
            if (redTemp * redVal < 0) {
                redCrossings++;
            }
//			irFit.add(irF);
//			irWindow.add(irVal);
            lastCross = red.size();
            irTemp = irVal;
            redTemp = redVal;
        }

        System.out.println("Crossings: " + redCrossings);
        double pk2pkRed = redStats.getMax() - redStats.getMin();
        double pk2pkIR = irStats.getMax() - irStats.getMin();
        double redStd = redStats.getStandardDeviation();
        double irStd = irStats.getStandardDeviation();
        double result = (redStd / redMean) / (irStd / irMean);
//        double result = (pk2pkRed/redMean)/(pk2pkIR/irMean);
        //red less than IR because we already took the negitive to flip the waveforms.
        return (redMean < irMean) ? (result) : (1 / result);
    }
}

package com.example.tomphelps.pulseox.PulseOxProcessing;

import com.example.tomphelps.pulseox.ParallelWriter;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

/**
 * Created by Tom Phelps on 11/20/2016.
 */

public class PpgWritingThread implements Runnable {
    private BlockingQueue<InputItem> queue;
    private PpgExtractor extractor = new PpgExtractor();
    private PreFilter preFilter = new PreFilter();

    @Override
    public void run() {
        queue=new LinkedBlockingQueue<InputItem>();
        InputItem tempItem;
        try {
            while((tempItem=queue.take()).flag){
                extractor.processFilteredInput(preFilter.filterValue(tempItem.val));

            }
            System.out.println("Thread Ended");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop(){
        try {
            InputItem item = new InputItem();
            item.flag=false;
            item.val=0;
            queue.put(item);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void add(double input){
        try {
            InputItem item = new InputItem();
            item.val=input;
            item.flag=true;
            queue.put(item);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public ArrayList<Double> getRedPPG(){
        return extractor.getRedPpg();
    }
    public ArrayList<Double> getIRPPG(){
        return extractor.getIRPpg();
    }
    public ArrayList<Double> getSp02Results(){
        return extractor.getResultsList();
    }
    public ArrayList<Double> getHRList(){
        return extractor.getHRList();
    }
    private static class InputItem {
        double val;
        boolean flag;
    }
}

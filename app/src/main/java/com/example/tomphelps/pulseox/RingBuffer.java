package com.example.tomphelps.pulseox;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Created by Tom Phelps on 12/20/2016.
 */

public class RingBuffer {
    private double[] buffer;
    private int tail;
    private int head;
    public RingBuffer(int n){
        head = 0;
        tail = 0;
        buffer = new double[n];
        for(int i=0;i<n;i++){
            buffer[i]=0;
        }
    }
    public void add(double input) {
        buffer[head++]=input;
        head = head % buffer.length;
    }

    public double averageBuffer(){
        double result=0;
        int count = 0;
        for(int i=0;i<buffer.length;i++){
            result+=buffer[i];
            count +=buffer[i]==0?0:1;
        }
        return result/count;
    }

}

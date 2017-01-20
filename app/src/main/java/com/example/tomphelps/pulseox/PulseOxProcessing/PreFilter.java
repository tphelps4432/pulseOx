package com.example.tomphelps.pulseox.PulseOxProcessing;

import java.util.ArrayList;


public class PreFilter {
	private double[] b={1  ,   3  ,   3   ,  1};
	private double[] a={ 1.0000  , -2.8006   , 2.6206 ,  -0.8191};
	private  ArrayList<Double> x = new ArrayList<Double>();
	private  ArrayList<Double> y = new ArrayList<Double>();
	
	int valuesCount=0;
	double temp;
	public double filterValue(double input){
		if(valuesCount<a.length){
			valuesCount++;
		}
		double xval=0;
		double yval=0;
		x.add(input);
		for(int i=1;i<valuesCount;i++){
			xval += Math.pow(10, -6)*b[i]*x.get(x.size()-i);
			if(y.size()>i){
				yval+=a[i]*y.get(y.size()-i);
			}
		}

		temp=xval-yval;
		y.add(temp);

		if(x.size()>8){
			x.remove(0);
		}
		if(y.size()>8){
			y.remove(0);
		}
		return temp;
	}
}

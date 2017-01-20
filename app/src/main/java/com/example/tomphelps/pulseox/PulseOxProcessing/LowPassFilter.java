package com.example.tomphelps.pulseox.PulseOxProcessing;

import java.util.ArrayList;


public class LowPassFilter {
	
	private double[] a={1,-5.63588091549025,13.2451045717617,-16.6141701561356,11.7312467919334,-4.42097102910448,0.694671323775998};
	private double[] b={9.16782505466784*Math.pow(10, -9),5.50069503280071*Math.pow(10, -8),1.37517375820018*Math.pow(10,-7),1.83356501093357*Math.pow(10,-7),1.37517375820018*Math.pow(10,-7),5.50069503280071*Math.pow(10,-8),9.16782505466784*Math.pow(10,-9)};
	private  ArrayList<Double> x = new ArrayList<Double>();
	private  ArrayList<Double> y = new ArrayList<Double>();
	
	int valuesCount=0;
	double temp;
	public double filterValue(double input){
		if(valuesCount<7){
			valuesCount++;
		}
		double xval=0;
		double yval=0;
		x.add(input);
		for(int i=1;i<valuesCount;i++){
			xval += b[i]*x.get(x.size()-i);
			if(y.size()>i){
				yval+=a[i]*y.get(y.size()-i);
			}
		}

		temp=xval-yval;
		y.add(temp);

		if(x.size()>9){
			x.remove(0);
		}
		if(y.size()>9){
			y.remove(0);
		}

		return temp;
	}
	
}

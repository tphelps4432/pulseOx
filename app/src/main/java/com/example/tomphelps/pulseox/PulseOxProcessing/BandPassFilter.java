package com.example.tomphelps.pulseox.PulseOxProcessing;

import java.util.ArrayList;


public class BandPassFilter {
	
	private double[] a={1,	-7.33586755993981,	23.5706460281748,	-43.3293109056529,	49.8461567019532,	-36.7489873493053,	16.9566602526450,	-4.47723397912580,	0.517936811277656};
	private double[] b={0.000182309060503069*Math.pow(10, -4),	0,	-0.000729236242012277*Math.pow(10, -4),	0,	0.00109385436301842*Math.pow(10, -4),	0,	-0.000729236242012277*Math.pow(10, -4),	0,	0.000182309060503069*Math.pow(10, -4)};
	private  ArrayList<Double> x = new ArrayList<Double>();
	private  ArrayList<Double> y = new ArrayList<Double>();
	
	int valuesCount=0;
	double temp;
	public double filterValue(double input){
		if(valuesCount<9){
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
		if(x.size()>10){
			x.remove(0);
		}
		if(y.size()>10){
			y.remove(0);
		}
		return temp;
	}
	
}

package com.example.tomphelps.pulseox;

import java.util.ArrayList;

/**
 * Created by Tom Phelps on 7/31/2015.
 */
public class NotchFilter {
    private double sampleRate = 44100;
    private double bW = 5000 / sampleRate;
    private double actualFreq = PlaySine.getFreq();
    private ArrayList<Double> x = new ArrayList();
    private ArrayList<Double> y = new ArrayList<>();
    private double freq = actualFreq / sampleRate;

    private double R = 0;
    private double K = 0;
    private double a[] = null;
    private double b[] = null;

    public void updateValues() {
        actualFreq = PlaySine.getFreq();
        freq = actualFreq / sampleRate;
        System.out.println("Filtering: " + actualFreq + " Hz");
        R = 1 - 3 * bW;
        K = (1 - 2 * Math.cos(2 * Math.PI * freq) + R * R) / (2 - 2 * Math.cos(2 * Math.PI * freq));
        double aTemp[] = {K, -2 * K * Math.cos(2 * Math.PI * freq), K};
        a = aTemp;
        double bTemp[] = {2 * R * Math.cos(2 * Math.PI * freq), -R * R};
        b = bTemp;
        y = new ArrayList<>();
        x = new ArrayList<>();
    }

    public double filterValue(double input) {
        x.add(0, input);
        if (x.size() > 4)
            x.remove(3);
        double result = 0;
        switch (x.size()) {
            case 0:
                break;
            case 1:
                System.out.println("Size of the x array: "+x.size()+"val at x(0): "+x.get(0));
                result = a[0] * x.get(0);
                break;
            case 2:
                result = a[0] * x.get(0) + a[1] * x.get(1) + y.get(0) * b[0];
                break;
            default:
                result = a[0] * x.get(0) + a[1] * x.get(1) + x.get(2) * a[2] + y.get(0) * b[0] + y.get(1) * b[1];
                break;

        }
        y.add(0, result);
        if (y.size() > 4)
            y.remove(3);
        return result;
    }

}

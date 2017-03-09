package com.asymmetrik.nifi.standard.processors.util;

import java.io.Serializable;

public class MomentAggregator implements Serializable {
    private static final long serialVersionUID = 1L;

    private double min;
    private double max;
    private double mean;
    private double m2;
    private double sum;
    private int n;

    public MomentAggregator() {
        reset();
    }

    public synchronized void addValue(double x) {
        min = x < min ? x : min;
        max = x > max ? x : max;
        sum += x;
        n++;
        double delta = x - mean;
        mean += delta / n;
        m2 += delta * (x - mean);
    }

    public synchronized double getMin() {
        return min;
    }

    public synchronized double getMax() {
        return max;
    }

    public synchronized double getMean() {
        return mean;
    }

    public synchronized double getSum() {
        return sum;
    }

    public synchronized double getVariance() {
        return n < 2 ? 0.0 : m2 / (n - 1);
    }

    public synchronized double getStandardDeviation() {

        return n < 2 ? 0.0 : Math.sqrt(m2 / (n - 1));
    }

    public synchronized int getN() {
        return n;
    }

    public final synchronized void reset() {
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
        mean = 0.0;
        m2 = 0.0;
        sum = 0.0;
        n = 0;
    }
}

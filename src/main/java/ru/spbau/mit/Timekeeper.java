package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Timekeeper {
    private long sum = 0, startTime = 0;
    private int count;
    private final Logger LOG = LogManager.getLogger(this);

    public Timekeeper() {
    }

    public void start() {
        ++count;
        startTime = System.currentTimeMillis();
    }

    public void finish() {
        sum += System.currentTimeMillis() - startTime;
    }

    public long getSum() {
        return sum;
    }

    public double average() {
        return (double) sum / count;
    }
}

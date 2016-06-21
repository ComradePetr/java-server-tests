package ru.spbau.mit.servertests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class Timekeeper {
    private long sum = 0;
    private final ArrayList<Long> startTime = new ArrayList<>();
    private final Logger log = LogManager.getLogger(this);

    public synchronized int start() {
        startTime.add(System.currentTimeMillis());
        return startTime.size() - 1;
    }

    public synchronized void finish(int id) {
        sum += System.currentTimeMillis() - startTime.get(id);
    }

    public long getSum() {
        return sum;
    }

    public double average() {
        return (double) sum / startTime.size();
    }
}

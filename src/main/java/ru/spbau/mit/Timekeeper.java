package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class Timekeeper {
    private long sum = 0;
    private ArrayList<Long> startTime = new ArrayList<>();
    private final Logger LOG = LogManager.getLogger(this);

    synchronized public int start() {
        startTime.add(System.currentTimeMillis());
        return startTime.size() - 1;
    }

    synchronized public void finish(int id) {
        sum += System.currentTimeMillis() - startTime.get(id);
    }

    public long getSum() {
        return sum;
    }

    public double average() {
        LOG.info("{}",startTime.size());
        return (double) sum / startTime.size();
    }
}

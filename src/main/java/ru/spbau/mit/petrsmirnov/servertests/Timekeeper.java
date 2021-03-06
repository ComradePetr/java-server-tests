package ru.spbau.mit.petrsmirnov.servertests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

/**
 * Класс группы секундомеров, позволяющий:
 * 1) запустить новый секундомер (выдаётся его id),
 * 2) остановить секундомер по его id,
 * 3) посчитать среднее время, измеренное секундомерами, либо сумму их измерений.
 */
public class Timekeeper {
    public static final double NO_FINISHED_TIMEKEEPERS = -1;
    private final ArrayList<Long> startTime = new ArrayList<>();
    private final Logger log = LogManager.getLogger(this);

    private long sum = 0, finishedCount = 0;

    public synchronized int start() {
        startTime.add(System.currentTimeMillis());
        return startTime.size() - 1;
    }

    public synchronized void finish(int id) {
        sum += System.currentTimeMillis() - startTime.get(id);
        ++finishedCount;
    }

    public long getSum() {
        return sum;
    }

    public double getAverage() {
        return finishedCount > 0 ? (double) sum / finishedCount : NO_FINISHED_TIMEKEEPERS;
    }
}

/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.util.metrics;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Statistics {
    private final ConcurrentHashMap<String, Metric> metricMap = new ConcurrentHashMap<>();

    public static final long PER_SECOND = 1000;
    public static final long PER_MINUTE = 60000;

    public static Statistics getGlobal()
    {
        return StatisticsStorage.globalStatistics;
    }

    private static class StatisticsStorage {
        final static Statistics globalStatistics = new Statistics();
    }

    public PerformanceMetric createPerformanceMetric(String itemName, long interval)
    {
        PerformanceMetric metric = (PerformanceMetric)
                metricMap.computeIfAbsent(itemName, s -> new PerformanceMetric(itemName, interval));

        metric.clean();

        return metric;
    }

    public Metric setProgress(String itemName, long current, long total)
    {
        Metric metric = metricMap.computeIfAbsent(itemName, s -> new Metric(itemName));

        metric.setTotal(total);
        metric.setCurrent(current);

        return metric;
    }

    public Collection<Metric> getMetrics()
    {
        return metricMap.values();
    }

    public Metric getProgressMetric(String itemName)
    {
        return metricMap.computeIfAbsent(itemName, s -> new Metric(itemName));
    }

    public void removeItem(String itemName)
    {
        metricMap.remove(itemName);
    }

    public class Metric {
        final String namespace;
        final String name;

        volatile long total;
        final AtomicLong current = new AtomicLong(0);
        volatile Object value = null;

        public Metric(String name) {
            int findDot = name.lastIndexOf('.');

            if (findDot != -1) {
                this.namespace = name.substring(0, findDot);
                this.name = name.substring(findDot + 1);
            } else {
                this.namespace = "";
                this.name = name;
            }
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getCurrent() {
            return current.get();
        }

        public void setCurrent(long current) {
            this.current.set(current);
        }

        public long addCurrent(long dCurrent) {
            return this.current.addAndGet(dCurrent);
        }

        public long inc() {
            return this.current.incrementAndGet();
        }

        public Object getValue() {
            return value;
        }

        public String getName() {
            return name;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public String toString()
        {
            return name + " : " + ((value == null) ? ("" + current.get() + ((total >= 0) ? ("/" + total) : "")) : value.toString());
        }
    }

    public class PerformanceMetric extends Metric
    {
        private long lastMeasured;
        private long lastValue;
        private long speed;
        private final long multiplier;

        public PerformanceMetric(String name, long multiplier) {
            super(name);
            this.multiplier = multiplier;
            lastMeasured = System.currentTimeMillis();
            this.total = -1;
        }

        private void measure() {
            long currentTime  = System.currentTimeMillis();
            long currentValue = getCurrent();
            speed = (currentValue - lastValue) * multiplier / (currentTime - lastMeasured);
            lastValue = currentValue;
            lastMeasured = currentTime;
        }

        public void clean() {
            setCurrent(0);
            speed = 0;
            lastMeasured = System.currentTimeMillis();
            lastValue = 0;
        }

        private long getSpeed() {
            if ((System.currentTimeMillis() - lastMeasured) > multiplier) {
                measure();
            }

            return speed;
        }

        @Override
        public String toString() {
            return super.toString() + " (v=" + getSpeed() + ")";
        }
    }
}

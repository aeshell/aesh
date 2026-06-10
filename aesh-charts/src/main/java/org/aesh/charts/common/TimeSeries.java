package org.aesh.charts.common;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A time-indexed data series where the X-axis represents timestamps.
 */
public class TimeSeries {

    private final String name;
    private final List<Long> timestamps; // epoch millis
    private final List<Double> values;
    private String color;
    private LineStyle lineStyle = LineStyle.SOLID;

    public TimeSeries(String name) {
        this.name = name;
        this.timestamps = new ArrayList<>();
        this.values = new ArrayList<>();
    }

    /**
     * Create from arrays.
     */
    public static TimeSeries of(String name, long[] timestamps, double[] values) {
        TimeSeries ts = new TimeSeries(name);
        for (int i = 0; i < Math.min(timestamps.length, values.length); i++) {
            ts.add(timestamps[i], values[i]);
        }
        return ts;
    }

    /**
     * Create from Instant list.
     */
    public static TimeSeries of(String name, List<Instant> timestamps, List<Double> values) {
        TimeSeries ts = new TimeSeries(name);
        for (int i = 0; i < Math.min(timestamps.size(), values.size()); i++) {
            ts.add(timestamps.get(i).toEpochMilli(), values.get(i));
        }
        return ts;
    }

    /**
     * Create from typed objects using accessor functions.
     */
    public static <T> TimeSeries from(String name, List<T> data,
            java.util.function.ToLongFunction<T> timestampAccessor,
            java.util.function.ToDoubleFunction<T> valueAccessor) {
        TimeSeries ts = new TimeSeries(name);
        for (T item : data) {
            ts.add(timestampAccessor.applyAsLong(item), valueAccessor.applyAsDouble(item));
        }
        return ts;
    }

    public void add(long epochMillis, double value) {
        timestamps.add(epochMillis);
        values.add(value);
    }

    public void addInstant(Instant time, double value) {
        add(time.toEpochMilli(), value);
    }

    public String name() {
        return name;
    }

    public int size() {
        return timestamps.size();
    }

    public long timestampAt(int index) {
        return timestamps.get(index);
    }

    public double valueAt(int index) {
        return values.get(index);
    }

    public List<Long> timestamps() {
        return Collections.unmodifiableList(timestamps);
    }

    public List<Double> values() {
        return Collections.unmodifiableList(values);
    }

    public long startTime() {
        return timestamps.isEmpty() ? 0 : timestamps.get(0);
    }

    public long endTime() {
        return timestamps.isEmpty() ? 0 : timestamps.get(timestamps.size() - 1);
    }

    public double minValue() {
        return values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
    }

    public double maxValue() {
        return values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
    }

    /**
     * Convert to a DataSeries where X = epoch millis as doubles.
     */
    public DataSeries toDataSeries() {
        DataSeries ds = new DataSeries(name);
        for (int i = 0; i < size(); i++) {
            ds.add(timestamps.get(i), values.get(i));
        }
        ds.color(color);
        ds.lineStyle(lineStyle);
        return ds;
    }

    public String color() {
        return color;
    }

    public TimeSeries color(String color) {
        this.color = color;
        return this;
    }

    public LineStyle lineStyle() {
        return lineStyle;
    }

    public TimeSeries lineStyle(LineStyle lineStyle) {
        this.lineStyle = lineStyle;
        return this;
    }
}

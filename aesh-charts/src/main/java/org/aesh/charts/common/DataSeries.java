package org.aesh.charts.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A named collection of (x, y) data points with color and style.
 */
public class DataSeries {

    private final String name;
    private final List<Double> xValues;
    private final List<Double> yValues;
    private String color; // ANSI color string
    private LineStyle lineStyle = LineStyle.SOLID;

    public DataSeries(String name) {
        this.name = name;
        this.xValues = new ArrayList<>();
        this.yValues = new ArrayList<>();
    }

    public DataSeries(String name, double[] xValues, double[] yValues) {
        this(name);
        for (int i = 0; i < Math.min(xValues.length, yValues.length); i++) {
            this.xValues.add(xValues[i]);
            this.yValues.add(yValues[i]);
        }
    }

    public DataSeries(String name, List<Double> xValues, List<Double> yValues) {
        this.name = name;
        this.xValues = new ArrayList<>(xValues);
        this.yValues = new ArrayList<>(yValues);
    }

    /**
     * Create a series from typed objects using accessor functions.
     */
    public static <T> DataSeries from(String name, List<T> data,
            java.util.function.ToDoubleFunction<T> xAccessor,
            java.util.function.ToDoubleFunction<T> yAccessor) {
        DataSeries series = new DataSeries(name);
        for (T item : data) {
            series.add(xAccessor.applyAsDouble(item), yAccessor.applyAsDouble(item));
        }
        return series;
    }

    /**
     * Create a series with sequential x-values (0, 1, 2, ...).
     */
    public static DataSeries ofValues(String name, double... values) {
        DataSeries series = new DataSeries(name);
        for (int i = 0; i < values.length; i++) {
            series.add(i, values[i]);
        }
        return series;
    }

    public void add(double x, double y) {
        xValues.add(x);
        yValues.add(y);
    }

    public String name() {
        return name;
    }

    public List<Double> xValues() {
        return Collections.unmodifiableList(xValues);
    }

    public List<Double> yValues() {
        return Collections.unmodifiableList(yValues);
    }

    public int size() {
        return xValues.size();
    }

    public double xAt(int index) {
        return xValues.get(index);
    }

    public double yAt(int index) {
        return yValues.get(index);
    }

    public double xMin() {
        return xValues.stream().mapToDouble(Double::doubleValue).min().orElse(0);
    }

    public double xMax() {
        return xValues.stream().mapToDouble(Double::doubleValue).max().orElse(0);
    }

    public double yMin() {
        return yValues.stream().mapToDouble(Double::doubleValue).min().orElse(0);
    }

    public double yMax() {
        return yValues.stream().mapToDouble(Double::doubleValue).max().orElse(0);
    }

    public String color() {
        return color;
    }

    public DataSeries color(String color) {
        this.color = color;
        return this;
    }

    public LineStyle lineStyle() {
        return lineStyle;
    }

    public DataSeries lineStyle(LineStyle lineStyle) {
        this.lineStyle = lineStyle;
        return this;
    }
}

package org.aesh.charts.common;

/**
 * A horizontal reference line drawn across the chart at a fixed Y value.
 * <p>
 * Used for threshold lines, baselines, targets, and other reference values.
 * <p>
 * Example:
 *
 * <pre>
 * chart.addHorizontalLine(HorizontalLine.at(80.0)
 *         .label("threshold")
 *         .color(ANSI.RED_TEXT)
 *         .dashed(true));
 * </pre>
 */
public class HorizontalLine {

    private final double yValue;
    private String label;
    private String color;
    private boolean dashed = true;

    private HorizontalLine(double yValue) {
        this.yValue = yValue;
    }

    /**
     * Create a horizontal line at the given Y value.
     */
    public static HorizontalLine at(double yValue) {
        return new HorizontalLine(yValue);
    }

    public HorizontalLine label(String label) {
        this.label = label;
        return this;
    }

    public HorizontalLine color(String color) {
        this.color = color;
        return this;
    }

    public HorizontalLine dashed(boolean dashed) {
        this.dashed = dashed;
        return this;
    }

    public double yValue() {
        return yValue;
    }

    public String label() {
        return label;
    }

    public String color() {
        return color;
    }

    public boolean dashed() {
        return dashed;
    }
}

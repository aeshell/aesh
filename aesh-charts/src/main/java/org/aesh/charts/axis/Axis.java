package org.aesh.charts.axis;

import org.aesh.charts.canvas.Canvas;
import org.aesh.charts.common.ChartStyle;
import org.aesh.charts.common.Scale;

/**
 * Renders a numeric axis with labels and tick marks.
 */
public class Axis {

    private double min;
    private double max;
    private String label;
    private Scale scale = Scale.LINEAR;
    private int tickCount = 5;
    private String formatPattern;

    public Axis() {
    }

    public Axis min(double min) {
        this.min = min;
        return this;
    }

    public Axis max(double max) {
        this.max = max;
        return this;
    }

    public Axis label(String label) {
        this.label = label;
        return this;
    }

    public Axis scale(Scale scale) {
        this.scale = scale;
        return this;
    }

    public Axis tickCount(int tickCount) {
        this.tickCount = tickCount;
        return this;
    }

    public Axis format(String pattern) {
        this.formatPattern = pattern;
        return this;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public String label() {
        return label;
    }

    public Scale scale() {
        return scale;
    }

    /**
     * Auto-set min/max from data range, rounding to nice tick-aligned bounds.
     * Uses a "nice number" algorithm that picks tick intervals from
     * {1, 2, 2.5, 5, 10} * 10^n, then aligns min/max to those intervals.
     * This produces tight, readable axis bounds like 0-20 or 0-25 instead
     * of excessive padding like -1 to 30 (#585).
     */
    public void autoRange(double dataMin, double dataMax) {
        if (dataMin == dataMax) {
            this.min = dataMin - 1;
            this.max = dataMax + 1;
            return;
        }
        double range = dataMax - dataMin;
        // Compute a nice tick interval for the desired number of ticks
        double roughInterval = range / (tickCount - 1);
        double niceInterval = niceNum(roughInterval, false);
        // Align min/max to the tick interval
        this.min = Math.floor(dataMin / niceInterval) * niceInterval;
        this.max = Math.ceil(dataMax / niceInterval) * niceInterval;
    }

    /**
     * Map a data value to a normalized position (0.0 = min, 1.0 = max).
     */
    public double normalize(double value) {
        if (scale == Scale.LOGARITHMIC) {
            if (value <= 0 || min <= 0)
                return 0;
            double logMin = Math.log10(min);
            double logMax = Math.log10(max);
            if (logMax == logMin)
                return 0.5;
            return (Math.log10(value) - logMin) / (logMax - logMin);
        }
        double range = max - min;
        if (range == 0)
            return 0.5;
        return (value - min) / range;
    }

    /**
     * Map a normalized position (0.0-1.0) back to a data value.
     */
    public double denormalize(double normalized) {
        if (scale == Scale.LOGARITHMIC) {
            double logMin = Math.log10(min);
            double logMax = Math.log10(max);
            return Math.pow(10, logMin + normalized * (logMax - logMin));
        }
        return min + normalized * (max - min);
    }

    /**
     * Generate tick values for this axis.
     */
    public double[] tickValues() {
        double[] ticks = new double[tickCount];
        for (int i = 0; i < tickCount; i++) {
            double t = (double) i / (tickCount - 1);
            ticks[i] = denormalize(t);
        }
        return ticks;
    }

    /**
     * Format a tick value as a label string.
     */
    public String formatTick(double value) {
        if (formatPattern != null) {
            return String.format(formatPattern, value);
        }
        if (Math.abs(value) >= 1000) {
            return String.format("%.0f", value);
        } else if (Math.abs(value) >= 1) {
            return String.format("%.1f", value);
        } else {
            return String.format("%.2f", value);
        }
    }

    /**
     * Compute the width needed for Y-axis labels (max label width + 1 for tick mark).
     */
    public int labelWidth() {
        int maxWidth = 0;
        for (double tick : tickValues()) {
            maxWidth = Math.max(maxWidth, formatTick(tick).length());
        }
        return maxWidth + 1; // +1 for tick mark
    }

    /**
     * Draw a left Y-axis on the canvas.
     *
     * @param canvas the canvas
     * @param x x position of the axis line
     * @param yTop top y position
     * @param yBottom bottom y position
     * @param style chart style
     */
    public void drawYAxis(Canvas canvas, int x, int yTop, int yBottom, ChartStyle style) {
        int height = yBottom - yTop;
        canvas.verticalLine(x, yTop, height + 1, style.verticalLine());

        int labelWidth = labelWidth();
        double[] ticks = tickValues();
        for (int i = 0; i < ticks.length; i++) {
            double norm = normalize(ticks[i]);
            int y = yBottom - (int) (norm * height);
            if (y >= yTop && y <= yBottom) {
                canvas.set(x, y, style.teeRight());
                String tickLabel = formatTick(ticks[i]);
                int labelX = x - tickLabel.length();
                if (labelX >= 0) {
                    canvas.writeString(labelX, y, tickLabel);
                }
            }
        }

        // Draw label vertically if provided
        if (label != null && label.length() > 0) {
            int labelX = x - labelWidth - 1;
            if (labelX >= 0) {
                int labelY = yTop + (height - label.length()) / 2;
                for (int i = 0; i < label.length() && labelY + i <= yBottom; i++) {
                    if (labelY + i >= yTop) {
                        canvas.set(labelX, labelY + i, label.charAt(i));
                    }
                }
            }
        }
    }

    /**
     * Draw a bottom X-axis on the canvas.
     *
     * @param canvas the canvas
     * @param xLeft left x position
     * @param xRight right x position
     * @param y y position of the axis line
     * @param style chart style
     */
    public void drawXAxis(Canvas canvas, int xLeft, int xRight, int y, ChartStyle style) {
        int width = xRight - xLeft;
        canvas.horizontalLine(xLeft, y, width + 1, style.horizontalLine());
        canvas.set(xLeft, y, style.cornerBottomLeft());

        double[] ticks = tickValues();
        for (int i = 0; i < ticks.length; i++) {
            double norm = normalize(ticks[i]);
            int tickX = xLeft + (int) (norm * width);
            if (tickX >= xLeft && tickX <= xRight) {
                canvas.set(tickX, y, style.teeUp());
                String tickLabel = formatTick(ticks[i]);
                int labelStart = tickX - tickLabel.length() / 2;
                if (labelStart >= 0 && y + 1 < canvas.height()) {
                    canvas.writeString(labelStart, y + 1, tickLabel);
                }
            }
        }

        // Draw label centered below tick labels
        if (label != null && label.length() > 0 && y + 2 < canvas.height()) {
            int labelStart = xLeft + (width - label.length()) / 2;
            if (labelStart >= 0) {
                canvas.writeString(labelStart, y + 2, label);
            }
        }
    }

    /**
     * Compute a "nice" number close to the given value.
     * If {@code round} is true, rounds to the nearest nice number;
     * otherwise, takes the ceiling (next larger nice number).
     * <p>
     * Nice numbers are 1, 2, 2.5, 5, 10 multiplied by a power of 10.
     * This is the standard algorithm used by Heckbert (Graphics Gems, 1990)
     * for readable axis tick intervals.
     */
    static double niceNum(double value, boolean round) {
        if (value == 0)
            return 0;
        double sign = value < 0 ? -1 : 1;
        value = Math.abs(value);
        double exponent = Math.floor(Math.log10(value));
        double fraction = value / Math.pow(10, exponent);
        double nice;
        if (round) {
            if (fraction < 1.5)
                nice = 1;
            else if (fraction < 3)
                nice = 2;
            else if (fraction < 7)
                nice = 5;
            else
                nice = 10;
        } else {
            if (fraction <= 1)
                nice = 1;
            else if (fraction <= 2)
                nice = 2;
            else if (fraction <= 5)
                nice = 5;
            else
                nice = 10;
        }
        return sign * nice * Math.pow(10, exponent);
    }
}

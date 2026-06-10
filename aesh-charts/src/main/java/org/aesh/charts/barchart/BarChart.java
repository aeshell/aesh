package org.aesh.charts.barchart;

import java.util.ArrayList;
import java.util.List;

import org.aesh.charts.canvas.BlockEncoder;
import org.aesh.charts.canvas.Canvas;
import org.aesh.charts.common.ChartStyle;

/**
 * A bar chart that displays values as horizontal or vertical bars.
 * <p>
 * Example:
 *
 * <pre>
 * BarChart chart = BarChart.builder()
 *         .width(40).height(15)
 *         .orientation(Orientation.VERTICAL)
 *         .build();
 * chart.addBar("Q1", 42.5, ANSI.GREEN_TEXT);
 * chart.addBar("Q2", 38.1, ANSI.RED_TEXT);
 * System.out.println(chart.render());
 * </pre>
 */
public class BarChart {

    private final int width;
    private final int height;
    private final ChartStyle style;
    private final Orientation orientation;
    private final boolean showValues;
    private final List<Bar> bars = new ArrayList<>();

    private BarChart(Builder builder) {
        this.width = builder.width;
        this.height = builder.height;
        this.style = builder.style;
        this.orientation = builder.orientation;
        this.showValues = builder.showValues;
    }

    public static Builder builder() {
        return new Builder();
    }

    public BarChart addBar(String label, double value) {
        return addBar(label, value, null);
    }

    public BarChart addBar(String label, double value, String color) {
        bars.add(new Bar(label, value, color));
        return this;
    }

    /**
     * Render the bar chart to a multi-line string.
     */
    public String render() {
        if (bars.isEmpty())
            return "";

        if (orientation == Orientation.VERTICAL) {
            return renderVertical();
        } else {
            return renderHorizontal();
        }
    }

    private String renderVertical() {
        double maxValue = bars.stream().mapToDouble(b -> b.value).max().orElse(1);
        if (maxValue == 0)
            maxValue = 1;

        // Layout: Y-axis labels (left) + bars + gap + labels (bottom)
        String maxLabel = formatValue(maxValue);
        int yAxisWidth = maxLabel.length() + 1;
        int barAreaWidth = width - yAxisWidth;
        int barWidth = Math.max(1, (barAreaWidth - bars.size()) / bars.size());
        int gap = 1;
        int plotHeight = height - 2; // bottom row for labels

        Canvas canvas = new Canvas(width, height);

        // Draw Y-axis
        canvas.verticalLine(yAxisWidth - 1, 0, plotHeight, style.verticalLine());
        canvas.set(yAxisWidth - 1, plotHeight, style.cornerBottomLeft());
        canvas.horizontalLine(yAxisWidth - 1, plotHeight, barAreaWidth + 1, style.horizontalLine());

        // Y-axis labels (0 and max)
        String zeroLabel = formatValue(0);
        canvas.writeString(yAxisWidth - 1 - zeroLabel.length(), plotHeight - 1, zeroLabel);
        canvas.writeString(yAxisWidth - 1 - maxLabel.length(), 0, maxLabel);

        // Draw bars
        for (int i = 0; i < bars.size(); i++) {
            Bar bar = bars.get(i);
            int barX = yAxisWidth + i * (barWidth + gap);
            double fraction = bar.value / maxValue;
            int barHeight = (int) (fraction * plotHeight);

            // Use block elements for sub-cell resolution at the top
            double fractionalHeight = fraction * plotHeight;
            int fullCells = (int) fractionalHeight;
            double remainder = fractionalHeight - fullCells;

            // Draw full cells
            char fillChar = style == ChartStyle.ASCII ? '#' : BlockEncoder.FULL_BLOCK;
            for (int row = 0; row < fullCells && row < plotHeight; row++) {
                int y = plotHeight - 1 - row;
                for (int col = 0; col < barWidth; col++) {
                    canvas.set(barX + col, y, fillChar, bar.color);
                }
            }

            // Draw partial top cell
            if (remainder > 0 && fullCells < plotHeight) {
                int y = plotHeight - 1 - fullCells;
                char topChar = style == ChartStyle.ASCII ? '.' : BlockEncoder.forFraction(remainder);
                for (int col = 0; col < barWidth; col++) {
                    canvas.set(barX + col, y, topChar, bar.color);
                }
            }

            // Value label above bar
            if (showValues) {
                String valStr = formatValue(bar.value);
                int valX = barX + (barWidth - valStr.length()) / 2;
                int valY = plotHeight - 1 - barHeight - 1;
                if (valY >= 0) {
                    canvas.writeString(valX, valY, valStr);
                }
            }

            // Bar label below axis
            if (height - 1 < canvas.height()) {
                String label = bar.label;
                if (label.length() > barWidth + gap) {
                    label = label.substring(0, barWidth);
                }
                int labelX = barX + (barWidth - label.length()) / 2;
                canvas.writeString(labelX, plotHeight + 1, label);
            }
        }

        return canvas.render();
    }

    private String renderHorizontal() {
        double maxValue = bars.stream().mapToDouble(b -> b.value).max().orElse(1);
        if (maxValue == 0)
            maxValue = 1;

        int maxLabelWidth = bars.stream().mapToInt(b -> b.label.length()).max().orElse(3);
        int barAreaWidth = width - maxLabelWidth - 2; // -2 for separator and space

        Canvas canvas = new Canvas(width, bars.size());

        for (int i = 0; i < bars.size(); i++) {
            Bar bar = bars.get(i);
            // Label
            String label = String.format("%" + maxLabelWidth + "s", bar.label);
            canvas.writeString(0, i, label);
            canvas.set(maxLabelWidth, i, style.verticalLine());

            // Bar
            double fraction = bar.value / maxValue;
            int barLength = (int) (fraction * barAreaWidth);
            char fillChar = style == ChartStyle.ASCII ? '#' : BlockEncoder.FULL_BLOCK;
            int barStart = maxLabelWidth + 1;
            for (int col = 0; col < barLength; col++) {
                canvas.set(barStart + col, i, fillChar, bar.color);
            }

            // Value at end of bar
            if (showValues) {
                String valStr = " " + formatValue(bar.value);
                canvas.writeString(barStart + barLength, i, valStr);
            }
        }

        return canvas.render();
    }

    private static String formatValue(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format("%.1f", value);
    }

    // --- Inner types ---

    private static class Bar {
        final String label;
        final double value;
        final String color;

        Bar(String label, double value, String color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    public static class Builder {
        private int width = 40;
        private int height = 15;
        private ChartStyle style = ChartStyle.UNICODE;
        private Orientation orientation = Orientation.VERTICAL;
        private boolean showValues = true;

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder style(ChartStyle style) {
            this.style = style;
            return this;
        }

        public Builder orientation(Orientation orientation) {
            this.orientation = orientation;
            return this;
        }

        public Builder showValues(boolean show) {
            this.showValues = show;
            return this;
        }

        public BarChart build() {
            return new BarChart(this);
        }
    }
}

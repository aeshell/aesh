package org.aesh.charts.sparkline;

import java.util.ArrayList;
import java.util.List;

import org.aesh.charts.canvas.BlockEncoder;
import org.aesh.charts.common.ChartStyle;

/**
 * A compact inline data visualization using block elements.
 * <p>
 * Example output:
 *
 * <pre>
 * ▁▃█▂▅▆▁█▃
 * </pre>
 * <p>
 * Usage:
 *
 * <pre>
 * Sparkline spark = Sparkline.builder()
 *         .width(20)
 *         .height(1)
 *         .build();
 * spark.addAll(7.81, 3.82, 8.39, 2.06);
 * System.out.println(spark.render());
 * </pre>
 */
public class Sparkline {

    private final int width;
    private final int height;
    private final ChartStyle style;
    private final List<Double> values = new ArrayList<>();
    private String color;

    private Sparkline(Builder builder) {
        this.width = builder.width;
        this.height = builder.height;
        this.style = builder.style;
        this.color = builder.color;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Sparkline add(double value) {
        values.add(value);
        return this;
    }

    public Sparkline addAll(double... vals) {
        for (double v : vals) {
            values.add(v);
        }
        return this;
    }

    public Sparkline addAll(List<Double> vals) {
        values.addAll(vals);
        return this;
    }

    /**
     * Render the sparkline to a string.
     */
    public String render() {
        if (values.isEmpty())
            return "";

        // Determine which values to show (last N if more data than width)
        int dataCount = Math.min(values.size(), width);
        int startIdx = values.size() - dataCount;

        // Find min/max for scaling
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (int i = startIdx; i < values.size(); i++) {
            min = Math.min(min, values.get(i));
            max = Math.max(max, values.get(i));
        }
        double range = max - min;
        if (range == 0)
            range = 1;

        if (height == 1) {
            return renderSingleLine(startIdx, dataCount, min, range);
        } else {
            return renderMultiLine(startIdx, dataCount, min, range);
        }
    }

    private String renderSingleLine(int startIdx, int count, double min, double range) {
        StringBuilder sb = new StringBuilder();
        if (color != null)
            sb.append(color);
        for (int i = 0; i < count; i++) {
            double fraction = (values.get(startIdx + i) - min) / range;
            sb.append(BlockEncoder.forFraction(fraction));
        }
        if (color != null)
            sb.append("\u001B[0m");
        return sb.toString();
    }

    private String renderMultiLine(int startIdx, int count, double min, double range) {
        // Multi-line sparkline: each column has height cells,
        // each cell uses block elements for sub-cell resolution
        int totalLevels = height * 8; // 8 levels per block character
        StringBuilder sb = new StringBuilder();

        for (int row = 0; row < height; row++) {
            if (row > 0)
                sb.append('\n');
            if (color != null)
                sb.append(color);
            for (int col = 0; col < count; col++) {
                double fraction = (values.get(startIdx + col) - min) / range;
                int totalFill = (int) (fraction * totalLevels);
                // This row covers levels: (height - 1 - row) * 8 to (height - row) * 8
                int rowBottom = (height - 1 - row) * 8;
                int fillInRow = totalFill - rowBottom;
                if (fillInRow <= 0) {
                    sb.append(' ');
                } else if (fillInRow >= 8) {
                    sb.append(BlockEncoder.FULL_BLOCK);
                } else {
                    sb.append(BlockEncoder.forLevel(fillInRow));
                }
            }
            if (color != null)
                sb.append("\u001B[0m");
        }
        return sb.toString();
    }

    public static class Builder {
        private int width = 20;
        private int height = 1;
        private ChartStyle style = ChartStyle.UNICODE;
        private String color;

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

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public Sparkline build() {
            return new Sparkline(this);
        }
    }
}

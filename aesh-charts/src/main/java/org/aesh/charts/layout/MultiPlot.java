package org.aesh.charts.layout;

import java.util.ArrayList;
import java.util.List;

import org.aesh.charts.common.ChartStyle;
import org.aesh.charts.linechart.LineChart;

/**
 * Stacks multiple charts vertically with an optional shared X-axis.
 * <p>
 * Each chart renders independently with its own Y-axis, but they share
 * the same width and can share a common time axis at the bottom.
 * <p>
 * Example:
 *
 * <pre>
 * MultiPlot plot = MultiPlot.builder()
 *         .width(80)
 *         .build();
 * plot.addChart(cpuChart);
 * plot.addChart(memChart);
 * plot.addChart(latencyChart);
 * System.out.println(plot.render());
 * </pre>
 */
public class MultiPlot {

    private final int width;
    private final ChartStyle style;
    private final List<LineChart> charts = new ArrayList<>();

    private MultiPlot(Builder builder) {
        this.width = builder.width;
        this.style = builder.style;
    }

    public static Builder builder() {
        return new Builder();
    }

    public MultiPlot addChart(LineChart chart) {
        charts.add(chart);
        return this;
    }

    /**
     * Scroll all charts left by the given amount.
     */
    public void scrollLeft(int amount) {
        for (LineChart chart : charts) {
            chart.scrollLeft(amount);
        }
    }

    /**
     * Scroll all charts right by the given amount.
     */
    public void scrollRight(int amount) {
        for (LineChart chart : charts) {
            chart.scrollRight(amount);
        }
    }

    /**
     * Render all charts stacked vertically with separators.
     * <p>
     * The separator spans the widest rendered child, so charts narrower
     * than the plot width still get a matching divider.
     */
    public String render() {
        if (charts.isEmpty())
            return "";

        List<String> rendered = new ArrayList<>(charts.size());
        int maxWidth = 0;
        for (LineChart chart : charts) {
            String out = chart.render();
            rendered.add(out);
            for (String line : out.split("\n", -1))
                maxWidth = Math.max(maxWidth, visibleLength(line));
        }
        String separator = buildSeparator(Math.max(maxWidth, width));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rendered.size(); i++) {
            if (i > 0) {
                sb.append('\n').append(separator).append('\n');
            }
            sb.append(rendered.get(i));
        }
        return sb.toString();
    }

    private String buildSeparator(int separatorWidth) {
        StringBuilder sb = new StringBuilder();
        String sep = style.horizontalSeparator();
        for (int i = 0; i < separatorWidth; i++) {
            sb.append(sep);
        }
        return sb.toString();
    }

    private static int visibleLength(String line) {
        return line.replaceAll("\u001B\\[[;\\d]*m", "").length();
    }

    public static class Builder {
        private int width = 80;
        private ChartStyle style = ChartStyle.UNICODE;

        public Builder width(int width) {
            if (width <= 0)
                throw new IllegalArgumentException("width must be positive, got: " + width);
            this.width = width;
            return this;
        }

        public Builder style(ChartStyle style) {
            this.style = style;
            return this;
        }

        public MultiPlot build() {
            return new MultiPlot(this);
        }
    }
}

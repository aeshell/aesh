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
     */
    public String render() {
        if (charts.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        String separator = buildSeparator();

        for (int i = 0; i < charts.size(); i++) {
            if (i > 0) {
                sb.append('\n').append(separator).append('\n');
            }
            sb.append(charts.get(i).render());
        }
        return sb.toString();
    }

    private String buildSeparator() {
        StringBuilder sb = new StringBuilder();
        String sep = style.horizontalSeparator();
        for (int i = 0; i < width; i++) {
            sb.append(sep);
        }
        return sb.toString();
    }

    public static class Builder {
        private int width = 80;
        private ChartStyle style = ChartStyle.UNICODE;

        public Builder width(int width) {
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

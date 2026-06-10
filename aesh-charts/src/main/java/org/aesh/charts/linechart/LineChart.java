package org.aesh.charts.linechart;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.aesh.charts.axis.Axis;
import org.aesh.charts.canvas.BrailleEncoder;
import org.aesh.charts.canvas.Canvas;
import org.aesh.charts.common.ChartStyle;
import org.aesh.charts.common.DataSeries;
import org.aesh.charts.common.Legend;
import org.aesh.charts.common.LineStyle;
import org.aesh.charts.common.Scale;

/**
 * A line chart that plots one or more data series on a 2D grid.
 * <p>
 * Supports multiple rendering modes (ASCII, Unicode, Braille),
 * multiple series with colors and line styles, and viewport scrolling
 * for data that exceeds the terminal width.
 * <p>
 * Example:
 *
 * <pre>
 * LineChart chart = LineChart.builder()
 *         .width(60).height(15)
 *         .style(ChartStyle.BRAILLE)
 *         .xLabel("Time").yLabel("CPU %")
 *         .build();
 * chart.addSeries(cpuData);
 * System.out.println(chart.render());
 * </pre>
 */
public class LineChart {

    private final int width;
    private final int height;
    private final ChartStyle style;
    private final String xLabel;
    private final String yLabel;
    private final Scale xScale;
    private final Scale yScale;
    private final List<DataSeries> seriesList = new ArrayList<>();
    private boolean showLegend = true;

    // Viewport for scrolling
    private int viewportStart = -1; // -1 = auto (show latest)
    private int viewportSize = -1; // -1 = fit to width

    protected LineChart(Builder builder) {
        this.width = builder.width;
        this.height = builder.height;
        this.style = builder.style;
        this.xLabel = builder.xLabel;
        this.yLabel = builder.yLabel;
        this.xScale = builder.xScale;
        this.yScale = builder.yScale;
        this.showLegend = builder.showLegend;
    }

    public static Builder builder() {
        return new Builder();
    }

    public LineChart addSeries(DataSeries series) {
        seriesList.add(series);
        return this;
    }

    public List<DataSeries> seriesList() {
        return seriesList;
    }

    // --- Viewport scrolling ---

    public void scrollLeft(int amount) {
        if (viewportStart < 0)
            viewportStart = computeAutoViewportStart();
        viewportStart = Math.max(0, viewportStart - amount);
    }

    public void scrollRight(int amount) {
        if (viewportStart < 0)
            viewportStart = computeAutoViewportStart();
        viewportStart += amount;
    }

    public void scrollToStart() {
        viewportStart = 0;
    }

    public void scrollToEnd() {
        viewportStart = -1;
    }

    public void setViewportSize(int size) {
        this.viewportSize = size;
    }

    /**
     * Render the chart to a multi-line string.
     */
    public String render() {
        if (seriesList.isEmpty())
            return "";

        // Compute Y-axis range across all series
        Axis yAxis = new Axis().scale(yScale);
        if (yLabel != null)
            yAxis.label(yLabel);
        double yMin = Double.MAX_VALUE, yMax = -Double.MAX_VALUE;
        for (DataSeries s : seriesList) {
            yMin = Math.min(yMin, s.yMin());
            yMax = Math.max(yMax, s.yMax());
        }
        yAxis.autoRange(yMin, yMax);

        // Compute X-axis range
        Axis xAxis = new Axis().scale(xScale);
        if (xLabel != null)
            xAxis.label(xLabel);
        double xMin = Double.MAX_VALUE, xMax = -Double.MAX_VALUE;
        for (DataSeries s : seriesList) {
            xMin = Math.min(xMin, s.xMin());
            xMax = Math.max(xMax, s.xMax());
        }
        xAxis.autoRange(xMin, xMax);

        // Layout: legend (1 line) + plot area + x-axis (2 lines: ticks + label)
        int yAxisWidth = yAxis.labelWidth();
        int plotWidth = width - yAxisWidth - 1;
        int legendHeight = (showLegend && seriesList.size() > 1) ? 1 : 0;
        int xAxisHeight = 2 + (xLabel != null ? 1 : 0);
        int plotHeight = height - legendHeight - xAxisHeight;
        if (plotWidth < 5 || plotHeight < 3)
            return "Chart too small";

        int totalHeight = height;
        Canvas canvas = new Canvas(width, totalHeight, style == ChartStyle.BRAILLE);

        int plotLeft = yAxisWidth + 1;
        int plotRight = plotLeft + plotWidth - 1;
        int plotTop = legendHeight;
        int plotBottom = plotTop + plotHeight - 1;

        // Draw axes
        yAxis.drawYAxis(canvas, plotLeft - 1, plotTop, plotBottom, style);
        xAxis.drawXAxis(canvas, plotLeft - 1, plotRight, plotBottom + 1, style);

        // Plot each series
        for (DataSeries series : seriesList) {
            plotSeries(canvas, series, xAxis, yAxis, plotLeft, plotRight, plotTop, plotBottom);
        }

        // Draw legend
        if (showLegend && seriesList.size() > 1) {
            String legend = Legend.render(
                    seriesList.stream().map(DataSeries::name).collect(Collectors.toList()),
                    seriesList.stream().map(DataSeries::color).collect(Collectors.toList()),
                    style);
            // Center the legend
            int legendX = plotLeft + (plotWidth - stripAnsi(legend).length()) / 2;
            if (legendX < 0)
                legendX = 0;
            // Write legend character by character to preserve ANSI codes
            canvas.writeString(legendX, 0, legend);
        }

        return canvas.render();
    }

    /**
     * Plot a single data series onto the canvas.
     */
    protected void plotSeries(Canvas canvas, DataSeries series,
            Axis xAxis, Axis yAxis,
            int plotLeft, int plotRight, int plotTop, int plotBottom) {

        int plotWidth = plotRight - plotLeft + 1;
        int plotHeight = plotBottom - plotTop;

        if (style == ChartStyle.BRAILLE) {
            plotBraille(canvas, series, xAxis, yAxis,
                    plotLeft, plotTop, plotWidth, plotHeight);
        } else {
            plotStandard(canvas, series, xAxis, yAxis,
                    plotLeft, plotTop, plotWidth, plotHeight);
        }
    }

    private void plotBraille(Canvas canvas, DataSeries series,
            Axis xAxis, Axis yAxis,
            int plotLeft, int plotTop, int plotWidth, int plotHeight) {

        int subWidth = plotWidth * BrailleEncoder.CELL_WIDTH;
        int subHeight = plotHeight * BrailleEncoder.CELL_HEIGHT;
        String color = series.color();

        for (int i = 0; i < series.size(); i++) {
            double xNorm = xAxis.normalize(series.xAt(i));
            double yNorm = yAxis.normalize(series.yAt(i));

            int subX = plotLeft * BrailleEncoder.CELL_WIDTH + (int) (xNorm * (subWidth - 1));
            // Y is inverted: top = 0, bottom = max
            int subY = plotTop * BrailleEncoder.CELL_HEIGHT + (int) ((1.0 - yNorm) * (subHeight - 1));

            // Apply line style filtering
            if (shouldDraw(series.lineStyle(), i)) {
                canvas.setBrailleDot(subX, subY, color);
            }

            // Draw line segments between consecutive points
            if (i > 0) {
                double prevXNorm = xAxis.normalize(series.xAt(i - 1));
                double prevYNorm = yAxis.normalize(series.yAt(i - 1));
                int prevSubX = plotLeft * BrailleEncoder.CELL_WIDTH + (int) (prevXNorm * (subWidth - 1));
                int prevSubY = plotTop * BrailleEncoder.CELL_HEIGHT + (int) ((1.0 - prevYNorm) * (subHeight - 1));
                drawBrailleLine(canvas, prevSubX, prevSubY, subX, subY, color, series.lineStyle(), i);
            }
        }
    }

    private void drawBrailleLine(Canvas canvas, int x0, int y0, int x1, int y1,
            String color, LineStyle lineStyle, int pointIndex) {
        // Bresenham's line algorithm for braille sub-cells
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int step = 0;

        while (true) {
            if (shouldDraw(lineStyle, pointIndex + step)) {
                canvas.setBrailleDot(x0, y0, color);
            }
            if (x0 == x1 && y0 == y1)
                break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
            step++;
        }
    }

    private void plotStandard(Canvas canvas, DataSeries series,
            Axis xAxis, Axis yAxis,
            int plotLeft, int plotTop, int plotWidth, int plotHeight) {

        String color = series.color();
        char dot = style.dot();

        for (int i = 0; i < series.size(); i++) {
            if (!shouldDraw(series.lineStyle(), i))
                continue;

            double xNorm = xAxis.normalize(series.xAt(i));
            double yNorm = yAxis.normalize(series.yAt(i));

            int cellX = plotLeft + (int) (xNorm * (plotWidth - 1));
            int cellY = plotTop + (int) ((1.0 - yNorm) * plotHeight);

            canvas.set(cellX, cellY, dot, color);
        }
    }

    private static boolean shouldDraw(LineStyle lineStyle, int index) {
        switch (lineStyle) {
            case DASHED:
                return index % 2 == 0;
            case DOTTED:
                return index % 3 == 0;
            default:
                return true;
        }
    }

    private int computeAutoViewportStart() {
        // Default: show all data
        return 0;
    }

    private static String stripAnsi(String s) {
        return s.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    // --- Builder ---

    public static class Builder {
        private int width = 60;
        private int height = 15;
        private ChartStyle style = ChartStyle.UNICODE;
        private String xLabel;
        private String yLabel;
        private Scale xScale = Scale.LINEAR;
        private Scale yScale = Scale.LINEAR;
        private boolean showLegend = true;

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

        public Builder xLabel(String xLabel) {
            this.xLabel = xLabel;
            return this;
        }

        public Builder yLabel(String yLabel) {
            this.yLabel = yLabel;
            return this;
        }

        public Builder xScale(Scale scale) {
            this.xScale = scale;
            return this;
        }

        public Builder yScale(Scale scale) {
            this.yScale = scale;
            return this;
        }

        public Builder showLegend(boolean show) {
            this.showLegend = show;
            return this;
        }

        public LineChart build() {
            return new LineChart(this);
        }
    }
}

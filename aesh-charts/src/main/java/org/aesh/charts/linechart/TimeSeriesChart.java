package org.aesh.charts.linechart;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import org.aesh.charts.common.ChartStyle;
import org.aesh.charts.common.DataSeries;
import org.aesh.charts.common.Scale;
import org.aesh.charts.common.TimeSeries;

/**
 * A line chart with a time-formatted X-axis.
 * <p>
 * Extends {@link LineChart} with automatic time axis formatting based on
 * the data range (seconds, minutes, hours, days).
 * <p>
 * Example:
 *
 * <pre>
 * TimeSeriesChart chart = TimeSeriesChart.builder()
 *         .width(60).height(15)
 *         .style(ChartStyle.BRAILLE)
 *         .yLabel("CPU %")
 *         .build();
 * chart.addTimeSeries(cpuTimeSeries);
 * System.out.println(chart.render());
 * </pre>
 */
public class TimeSeriesChart extends LineChart {

    private static final DateTimeFormatter FMT_SECONDS = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter FMT_MINUTES = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter FMT_HOURS = DateTimeFormatter.ofPattern("MM/dd HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter FMT_DAYS = DateTimeFormatter.ofPattern("yyyy/MM/dd")
            .withZone(ZoneId.systemDefault());

    private TimeSeriesChart(TimeSeriesBuilder builder) {
        super(builder);
    }

    public static TimeSeriesBuilder tsBuilder() {
        return new TimeSeriesBuilder();
    }

    /**
     * Add a time series, converting it to a DataSeries internally.
     */
    public TimeSeriesChart addTimeSeries(TimeSeries ts) {
        DataSeries ds = ts.toDataSeries();
        addSeries(ds);
        return this;
    }

    /**
     * Choose the appropriate time format based on the data range.
     */
    public static DateTimeFormatter chooseFormat(long startMillis, long endMillis) {
        long spanMillis = endMillis - startMillis;
        if (spanMillis < 3_600_000L) { // < 1 hour
            return FMT_SECONDS;
        } else if (spanMillis < 86_400_000L) { // < 1 day
            return FMT_MINUTES;
        } else if (spanMillis < 30L * 86_400_000L) { // < 30 days
            return FMT_HOURS;
        } else {
            return FMT_DAYS;
        }
    }

    /**
     * Format an epoch millis value using the appropriate format.
     */
    public static String formatTime(long epochMillis, DateTimeFormatter fmt) {
        return fmt.format(Instant.ofEpochMilli(epochMillis));
    }

    // Builder extending LineChart.Builder with covariant overrides so
    // fluent chains preserve the TimeSeriesBuilder type through build().
    public static class TimeSeriesBuilder extends LineChart.Builder {

        @Override
        public TimeSeriesBuilder width(int width) {
            super.width(width);
            return this;
        }

        @Override
        public TimeSeriesBuilder height(int height) {
            super.height(height);
            return this;
        }

        @Override
        public TimeSeriesBuilder style(ChartStyle style) {
            super.style(style);
            return this;
        }

        @Override
        public TimeSeriesBuilder title(String title) {
            super.title(title);
            return this;
        }

        @Override
        public TimeSeriesBuilder xLabel(String xLabel) {
            super.xLabel(xLabel);
            return this;
        }

        @Override
        public TimeSeriesBuilder yLabel(String yLabel) {
            super.yLabel(yLabel);
            return this;
        }

        @Override
        public TimeSeriesBuilder xScale(Scale scale) {
            super.xScale(scale);
            return this;
        }

        @Override
        public TimeSeriesBuilder yScale(Scale scale) {
            super.yScale(scale);
            return this;
        }

        @Override
        public TimeSeriesBuilder xTickFormatter(Function<Double, String> formatter) {
            super.xTickFormatter(formatter);
            return this;
        }

        @Override
        public TimeSeriesBuilder yTickFormatter(Function<Double, String> formatter) {
            super.yTickFormatter(formatter);
            return this;
        }

        @Override
        public TimeSeriesBuilder xRange(double min, double max) {
            super.xRange(min, max);
            return this;
        }

        @Override
        public TimeSeriesBuilder yRange(double min, double max) {
            super.yRange(min, max);
            return this;
        }

        @Override
        public TimeSeriesBuilder xMin(double min) {
            super.xMin(min);
            return this;
        }

        @Override
        public TimeSeriesBuilder xMax(double max) {
            super.xMax(max);
            return this;
        }

        @Override
        public TimeSeriesBuilder yMin(double min) {
            super.yMin(min);
            return this;
        }

        @Override
        public TimeSeriesBuilder yMax(double max) {
            super.yMax(max);
            return this;
        }

        @Override
        public TimeSeriesBuilder viewportSize(int size) {
            super.viewportSize(size);
            return this;
        }

        @Override
        public TimeSeriesBuilder showLegend(boolean show) {
            super.showLegend(show);
            return this;
        }

        @Override
        public TimeSeriesChart build() {
            return new TimeSeriesChart(this);
        }
    }
}

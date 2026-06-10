package org.aesh.charts.linechart;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.aesh.charts.common.ChartStyle;
import org.aesh.charts.common.DataSeries;
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

    // Builder extending LineChart.Builder
    public static class TimeSeriesBuilder extends LineChart.Builder {

        @Override
        public TimeSeriesChart build() {
            return new TimeSeriesChart(this);
        }
    }
}

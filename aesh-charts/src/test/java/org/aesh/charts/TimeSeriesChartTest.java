package org.aesh.charts;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.aesh.charts.common.ChartStyle;
import org.aesh.charts.common.TimeSeries;
import org.aesh.charts.linechart.TimeSeriesChart;
import org.junit.Test;

public class TimeSeriesChartTest {

    @Test
    public void testChooseFormatBoundaries() {
        long hour = 3_600_000L;
        long day = 86_400_000L;
        long t = 1_718_456_730_000L;
        // Zone is baked into formatters at class load, so assert shapes only
        assertTrue("Sub-hour span uses seconds",
                TimeSeriesChart.formatTime(t, TimeSeriesChart.chooseFormat(t, t + 30 * 60_000L))
                        .matches("\\d{2}:\\d{2}:\\d{2}"));
        assertTrue("Sub-day span uses minutes",
                TimeSeriesChart.formatTime(t, TimeSeriesChart.chooseFormat(t, t + 12 * hour))
                        .matches("\\d{2}:\\d{2}"));
        assertTrue("Multi-day span uses month/day",
                TimeSeriesChart.formatTime(t, TimeSeriesChart.chooseFormat(t, t + 10 * day))
                        .matches("\\d{2}/\\d{2} \\d{2}:\\d{2}"));
        assertTrue("Multi-month span uses year",
                TimeSeriesChart.formatTime(t, TimeSeriesChart.chooseFormat(t, t + 60 * day))
                        .matches("\\d{4}/\\d{2}/\\d{2}"));
    }

    @Test
    public void testFormatTimeShape() {
        // Zone is baked into the formatter at class load, so only assert shape
        String formatted = TimeSeriesChart.formatTime(1_000L,
                TimeSeriesChart.chooseFormat(0, 60_000L));
        assertNotNull(formatted);
        assertTrue("HH:mm:ss shape, got: " + formatted,
                formatted.matches("\\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    public void testAddTimeSeriesRenders() {
        // NB: parent Builder methods return the parent type, so keep the
        // TimeSeriesBuilder reference and call its covariant build().
        TimeSeriesChart.TimeSeriesBuilder builder = TimeSeriesChart.tsBuilder();
        builder.width(50);
        builder.height(10);
        builder.style(ChartStyle.UNICODE);
        builder.showLegend(false);
        TimeSeriesChart chart = builder.build();
        TimeSeries ts = TimeSeries.of("events",
                new long[] { 0, 60_000L, 120_000L },
                new double[] { 1.0, 2.0, 3.0 });
        chart.addTimeSeries(ts);

        String output = chart.render();
        assertNotNull(output);
        assertFalse(output.isEmpty());
    }
}

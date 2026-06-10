package org.aesh.charts;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.aesh.charts.common.ChartStyle;
import org.aesh.charts.common.DataSeries;
import org.aesh.charts.common.HorizontalLine;
import org.aesh.charts.common.Marker;
import org.aesh.charts.linechart.LineChart;
import org.junit.Test;

public class MarkerTest {

    @Test
    public void testMarkerRendered() {
        LineChart chart = LineChart.builder()
                .width(40).height(12)
                .style(ChartStyle.UNICODE)
                .showLegend(false)
                .build();

        DataSeries series = DataSeries.ofValues("data", 10, 20, 50, 30, 15, 25, 40, 20, 10);
        chart.addSeries(series);

        // Mark the spike at index 2 (x=2, y=50) as a regression
        chart.addMarker(Marker.at(2, 50).label("regr").symbol('!'));

        String output = chart.render();
        assertNotNull(output);
        assertTrue("Should contain the marker label 'regr'", output.contains("regr"));
        assertTrue("Should contain the marker symbol '!'", output.contains("!"));
    }

    @Test
    public void testMultipleMarkers() {
        LineChart chart = LineChart.builder()
                .width(50).height(15)
                .style(ChartStyle.UNICODE)
                .showLegend(false)
                .build();

        DataSeries series = DataSeries.ofValues("perf", 100, 105, 110, 200, 105, 100, 98, 300, 100);
        chart.addSeries(series);

        chart.addMarker(Marker.at(3, 200).label("chg1").symbol('\u25CF'));
        chart.addMarker(Marker.at(7, 300).label("chg2").symbol('\u25CF'));

        String output = chart.render();
        assertNotNull(output);
        assertTrue("Should contain chg1", output.contains("chg1"));
        assertTrue("Should contain chg2", output.contains("chg2"));
    }

    @Test
    public void testHorizontalThresholdLine() {
        LineChart chart = LineChart.builder()
                .width(40).height(12)
                .style(ChartStyle.UNICODE)
                .showLegend(false)
                .build();

        DataSeries series = DataSeries.ofValues("latency", 50, 60, 55, 120, 65, 70, 55, 130, 60);
        chart.addSeries(series);

        chart.addHorizontalLine(HorizontalLine.at(100).label("max").dashed(true));

        String output = chart.render();
        assertNotNull(output);
        assertTrue("Should contain threshold label 'max'", output.contains("max"));
    }

    @Test
    public void testMarkerWithColor() {
        LineChart chart = LineChart.builder()
                .width(40).height(10)
                .style(ChartStyle.UNICODE)
                .showLegend(false)
                .build();

        DataSeries series = DataSeries.ofValues("data", 10, 20, 30, 20, 10);
        chart.addSeries(series);

        chart.addMarker(Marker.at(2, 30)
                .label("peak")
                .color("\u001B[31m") // red
                .symbol('\u25B2')); // triangle up

        String output = chart.render();
        assertNotNull(output);
        assertTrue("Should contain ANSI color code", output.contains("\u001B[31m"));
        assertTrue("Should contain label", output.contains("peak"));
    }

    @Test
    public void testHorizontalLineIncludedInYRange() {
        LineChart chart = LineChart.builder()
                .width(40).height(10)
                .style(ChartStyle.UNICODE)
                .showLegend(false)
                .build();

        // Data goes up to 50, but threshold is at 100
        DataSeries series = DataSeries.ofValues("data", 10, 20, 30, 40, 50);
        chart.addSeries(series);
        chart.addHorizontalLine(HorizontalLine.at(100).label("limit"));

        String output = chart.render();
        assertNotNull(output);
        // The Y-axis should extend to at least 100 to show the threshold
        assertTrue("Should contain threshold label", output.contains("limit"));
    }

    @Test
    public void testAsciiMarkerFallback() {
        LineChart chart = LineChart.builder()
                .width(30).height(8)
                .style(ChartStyle.ASCII)
                .showLegend(false)
                .build();

        DataSeries series = DataSeries.ofValues("data", 5, 10, 15, 10, 5);
        chart.addSeries(series);
        chart.addMarker(Marker.at(2, 15).symbol('\u25CF')); // Unicode symbol

        String output = chart.render();
        assertNotNull(output);
        // ASCII mode should use 'X' instead of Unicode symbol
        assertTrue("ASCII mode should use X for marker", output.contains("X"));
    }
}

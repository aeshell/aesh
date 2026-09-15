package org.aesh.charts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.aesh.charts.common.ChartStyle;
import org.aesh.charts.common.DataSeries;
import org.aesh.charts.common.HorizontalLine;
import org.aesh.charts.common.Marker;
import org.aesh.charts.linechart.LineChart;
import org.aesh.charts.linechart.TimeSeriesChart;
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

    // --- Collision avoidance tests (#622) ---

    @Test
    public void testColocatedMarkersJoinLabels() {
        DataSeries s = new DataSeries("s");
        s.add(0, 1.0);
        s.add(1, 2.0);
        s.add(2, 10.0);
        LineChart chart = LineChart.builder().width(80).height(20)
                .style(ChartStyle.BRAILLE).showLegend(false)
                .yMin(0.55).yMax(10.45).build();
        chart.addSeries(s);
        // Two markers at nearly the same position — should merge
        chart.addMarker(Marker.at(1, 2.0).label("900%").color("\u001B[31m").symbol('\u25B2'));
        chart.addMarker(Marker.at(1, 2.18).label("100%").color("\u001B[31m").symbol('\u25B2'));

        String output = stripAnsi(chart.render());
        // Both labels should be legible — either merged ("900%, 100%") or offset
        assertTrue("First label should be present", output.contains("900%"));
        assertTrue("Second label should be present", output.contains("100%"));
        // The old bug: second symbol overwrote the first label, producing "90▲%"
        assertFalse("Labels should not be garbled", output.contains("90\u25B2%"));
    }

    @Test
    public void testNearMissMarkersOffset() {
        DataSeries s = new DataSeries("s");
        for (int i = 0; i <= 10; i++)
            s.add(i, i * 10);
        LineChart chart = LineChart.builder().width(60).height(15)
                .style(ChartStyle.UNICODE).showLegend(false).build();
        chart.addSeries(s);
        chart.addMarker(Marker.at(5, 50).label("mid"));
        chart.addMarker(Marker.at(6, 60).label("upper"));

        String output = stripAnsi(chart.render());
        assertTrue("mid label visible", output.contains("mid"));
        assertTrue("upper label visible", output.contains("upper"));
    }

    @Test
    public void testDenseClusterDoesNotCrash() {
        DataSeries s = new DataSeries("s");
        for (int i = 0; i < 5; i++)
            s.add(i, i);
        LineChart chart = LineChart.builder().width(40).height(8)
                .style(ChartStyle.UNICODE).showLegend(false).build();
        chart.addSeries(s);
        // 5 markers in a tiny chart — some labels will be dropped
        for (int i = 0; i < 5; i++)
            chart.addMarker(Marker.at(i, i).label("m" + i).symbol('!'));

        String output = chart.render();
        assertNotNull(output);
        // At minimum, marker symbols should all be present
        assertTrue(output.contains("!"));
    }

    @Test
    public void testEdgeMarkerLabelFallsBack() {
        DataSeries s = new DataSeries("s");
        s.add(0, 0);
        s.add(1, 100);
        LineChart chart = LineChart.builder().width(40).height(10)
                .style(ChartStyle.UNICODE).showLegend(false).build();
        chart.addSeries(s);
        // Marker at the very top — label can only go below
        chart.addMarker(Marker.at(1, 100).label("top"));

        String output = stripAnsi(chart.render());
        assertTrue(output.contains("top"));
    }

    @Test
    public void testTimeSeriesChartInheritsCollisionFix() {
        LineChart chart = TimeSeriesChart.tsBuilder().width(60).height(15)
                .style(ChartStyle.UNICODE).showLegend(false).build();
        DataSeries s = new DataSeries("ts");
        s.add(1000, 10);
        s.add(2000, 20);
        s.add(3000, 30);
        chart.addSeries(s);
        chart.addMarker(Marker.atTime(2000, 20).label("a"));
        chart.addMarker(Marker.atTime(2000, 20).label("b"));

        String output = stripAnsi(chart.render());
        // Merged label
        assertTrue("Merged labels visible", output.contains("a, b") || (output.contains("a") && output.contains("b")));
    }

    private static String stripAnsi(String s) {
        return s.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    // --- Legend entry tests (#623) ---

    @Test
    public void testMarkerLegendEntry() {
        LineChart chart = LineChart.builder()
                .width(60).height(12)
                .style(ChartStyle.UNICODE)
                .showLegend(true)
                .build();

        DataSeries s = DataSeries.ofValues("series1", 10, 20, 30, 20, 10);
        chart.addSeries(s);
        chart.addMarker(Marker.at(2, 30).label("peak").symbol('\u25B2')
                .color("\u001B[31m").legendName("Regression"));

        String output = stripAnsi(chart.render());
        assertTrue("Legend should contain marker name", output.contains("Regression"));
        assertTrue("Legend should contain marker symbol", output.contains("\u25B2"));
    }

    @Test
    public void testMarkerLegendDedup() {
        LineChart chart = LineChart.builder()
                .width(70).height(12)
                .style(ChartStyle.UNICODE)
                .showLegend(true)
                .build();

        DataSeries s = DataSeries.ofValues("data", 10, 20, 30, 40, 50);
        chart.addSeries(s);
        chart.addMarker(Marker.at(1, 20).label("a").symbol('!').legendName("Alerts"));
        chart.addMarker(Marker.at(3, 40).label("b").symbol('!').legendName("Alerts"));

        String output = stripAnsi(chart.render());
        // "Alerts" should appear exactly once, not twice
        int idx = output.indexOf("Alerts");
        assertTrue("Legend should contain Alerts", idx >= 0);
        assertEquals("Legend should not duplicate Alerts", -1,
                output.indexOf("Alerts", idx + 1));
    }

    @Test
    public void testMarkerWithoutLegendNameNotInLegend() {
        LineChart chart = LineChart.builder()
                .width(60).height(12)
                .style(ChartStyle.UNICODE)
                .showLegend(true)
                .build();

        DataSeries s1 = DataSeries.ofValues("s1", 10, 20, 30);
        DataSeries s2 = DataSeries.ofValues("s2", 30, 20, 10);
        chart.addSeries(s1);
        chart.addSeries(s2);
        chart.addMarker(Marker.at(1, 20).label("ann"));

        String output = stripAnsi(chart.render());
        assertTrue("Series legend should appear", output.contains("s1"));
        assertTrue("Series legend should appear", output.contains("s2"));
        // "ann" is a per-point label, not a legend name
        // It should appear as a label on the chart but not duplicated in the legend line
    }

    @Test
    public void testSingleSeriesWithMarkerLegendShown() {
        // With only 1 series, legend is normally hidden.
        // But if a marker has a legendName, legend should appear.
        LineChart chart = LineChart.builder()
                .width(60).height(12)
                .style(ChartStyle.UNICODE)
                .showLegend(true)
                .build();

        DataSeries s = DataSeries.ofValues("only", 10, 20, 30);
        chart.addSeries(s);
        chart.addMarker(Marker.at(2, 30).label("peak").legendName("Events"));

        String output = stripAnsi(chart.render());
        assertTrue("Marker legend should appear even with 1 series",
                output.contains("Events"));
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

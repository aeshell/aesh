package org.aesh.charts;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.aesh.charts.common.ChartStyle;
import org.aesh.charts.common.DataSeries;
import org.aesh.charts.common.LineStyle;
import org.aesh.charts.linechart.LineChart;
import org.junit.Test;

public class LineChartTest {

    @Test
    public void testBasicLineChart() {
        LineChart chart = LineChart.builder()
                .width(40).height(15)
                .style(ChartStyle.UNICODE)
                .yLabel("Value")
                .build();

        DataSeries series = DataSeries.ofValues("test", 1, 3, 5, 7, 5, 3, 1);
        chart.addSeries(series);

        String output = chart.render();
        assertNotNull(output);
        assertFalse(output.isEmpty());
    }

    @Test
    public void testBrailleLineChart() {
        LineChart chart = LineChart.builder()
                .width(40).height(12)
                .style(ChartStyle.BRAILLE)
                .build();

        DataSeries series = DataSeries.ofValues("sine", 0, 3, 5, 7, 5, 3, 0, -3, -5, -7, -5, -3, 0);
        chart.addSeries(series);

        String output = chart.render();
        assertNotNull(output);
        // Braille characters are in U+2800-U+28FF range
        boolean hasBraille = false;
        for (char c : output.toCharArray()) {
            if (c >= 0x2800 && c <= 0x28FF) {
                hasBraille = true;
                break;
            }
        }
        assertTrue("Should contain braille characters", hasBraille);
    }

    @Test
    public void testMultiSeriesChart() {
        LineChart chart = LineChart.builder()
                .width(50).height(15)
                .style(ChartStyle.UNICODE)
                .build();

        DataSeries s1 = DataSeries.ofValues("series1", 1, 2, 3, 4, 5);
        DataSeries s2 = DataSeries.ofValues("series2", 5, 4, 3, 2, 1);
        chart.addSeries(s1);
        chart.addSeries(s2);

        String output = chart.render();
        assertNotNull(output);
        // Legend should show both series names
        assertTrue("Should show series1 in legend", output.contains("series1"));
        assertTrue("Should show series2 in legend", output.contains("series2"));
    }

    @Test
    public void testDashedLine() {
        LineChart chart = LineChart.builder()
                .width(40).height(10)
                .style(ChartStyle.UNICODE)
                .showLegend(false)
                .build();

        DataSeries series = DataSeries.ofValues("dashed", 1, 2, 3, 4, 5);
        series.lineStyle(LineStyle.DASHED);
        chart.addSeries(series);

        String output = chart.render();
        assertNotNull(output);
    }

    @Test
    public void testEmptyChart() {
        LineChart chart = LineChart.builder().build();
        assertEquals("", chart.render());
    }

    @Test
    public void testFromAccessorFunction() {
        double[][] data = { { 0, 10 }, { 1, 20 }, { 2, 15 } };
        java.util.List<double[]> list = java.util.Arrays.asList(data);

        DataSeries series = DataSeries.from("points", list, d -> d[0], d -> d[1]);
        assertEquals(3, series.size());
        assertEquals(10.0, series.yAt(0), 0.001);
    }

    private static void assertEquals(String expected, String actual) {
        org.junit.Assert.assertEquals(expected, actual);
    }

    private static void assertEquals(int expected, int actual) {
        org.junit.Assert.assertEquals(expected, actual);
    }

    private static void assertEquals(double expected, double actual, double delta) {
        org.junit.Assert.assertEquals(expected, actual, delta);
    }
}

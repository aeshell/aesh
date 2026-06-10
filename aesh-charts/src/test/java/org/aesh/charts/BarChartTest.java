package org.aesh.charts;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.aesh.charts.barchart.BarChart;
import org.aesh.charts.barchart.Orientation;
import org.aesh.charts.common.ChartStyle;
import org.junit.Test;

public class BarChartTest {

    @Test
    public void testVerticalBarChart() {
        BarChart chart = BarChart.builder()
                .width(40).height(15)
                .orientation(Orientation.VERTICAL)
                .style(ChartStyle.UNICODE)
                .build();
        chart.addBar("Q1", 42.5);
        chart.addBar("Q2", 38.1);
        chart.addBar("Q3", 55.2);
        String output = chart.render();
        assertNotNull(output);
        assertTrue("Should contain Q1 label", output.contains("Q1"));
        assertTrue("Should contain Q2 label", output.contains("Q2"));
        assertTrue("Should contain Q3 label", output.contains("Q3"));
    }

    @Test
    public void testHorizontalBarChart() {
        BarChart chart = BarChart.builder()
                .width(40).height(5)
                .orientation(Orientation.HORIZONTAL)
                .style(ChartStyle.UNICODE)
                .build();
        chart.addBar("CPU", 78.5);
        chart.addBar("MEM", 45.2);
        chart.addBar("DSK", 92.1);
        String output = chart.render();
        assertNotNull(output);
        assertTrue("Should contain CPU label", output.contains("CPU"));
        assertTrue("Should contain MEM label", output.contains("MEM"));
    }

    @Test
    public void testAsciiStyle() {
        BarChart chart = BarChart.builder()
                .width(30).height(10)
                .style(ChartStyle.ASCII)
                .build();
        chart.addBar("A", 10);
        chart.addBar("B", 20);
        String output = chart.render();
        assertNotNull(output);
        assertTrue("ASCII style should use # for bars", output.contains("#"));
    }

    @Test
    public void testEmptyBarChart() {
        BarChart chart = BarChart.builder().build();
        assertEquals("", chart.render());
    }

    @Test
    public void testSingleBar() {
        BarChart chart = BarChart.builder()
                .width(20).height(10)
                .build();
        chart.addBar("Only", 100);
        String output = chart.render();
        assertNotNull(output);
        assertTrue("Should contain the label", output.contains("Only"));
    }

    private static void assertEquals(String expected, String actual) {
        org.junit.Assert.assertEquals(expected, actual);
    }
}

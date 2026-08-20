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

    @Test
    public void testTitleRendered() {
        DataSeries series = new DataSeries("data");
        for (int i = 0; i <= 10; i++) {
            series.add(i, i * 2);
        }

        LineChart chart = LineChart.builder()
                .title("My Chart Title")
                .width(40)
                .height(12)
                .build();
        chart.addSeries(series);

        String rendered = chart.render();
        assertTrue("Rendered output should contain the title",
                rendered.contains("My Chart Title"));
        // Title should be on the first line
        String firstLine = rendered.split("\n")[0];
        assertTrue("Title should be on the first line",
                firstLine.contains("My Chart Title"));
    }

    @Test
    public void testXTickFormatter() {
        String[] labels = { "Mon", "Tue", "Wed", "Thu", "Fri" };
        DataSeries series = new DataSeries("data");
        for (int i = 0; i < labels.length; i++) {
            series.add(i, (i + 1) * 10);
        }

        LineChart chart = LineChart.builder()
                .width(50)
                .height(10)
                .xTickFormatter(x -> {
                    int idx = (int) Math.round(x);
                    return idx >= 0 && idx < labels.length ? labels[idx] : "";
                })
                .build();
        chart.addSeries(series);

        String rendered = chart.render();
        assertTrue("Should contain custom X label 'Mon'", rendered.contains("Mon"));
        assertTrue("Should contain custom X label 'Fri'", rendered.contains("Fri"));
    }

    @Test
    public void testYTickFormatter() {
        DataSeries series = new DataSeries("data");
        series.add(0, 25);
        series.add(1, 75);

        LineChart chart = LineChart.builder()
                .width(40)
                .height(10)
                .yTickFormatter(y -> String.format("%.0f%%", y))
                .build();
        chart.addSeries(series);

        String rendered = chart.render();
        // Y-axis labels should have % suffix
        assertTrue("Should contain percentage format", rendered.contains("%"));
    }

    @Test
    public void testExplicitXRange() {
        DataSeries series = new DataSeries("data");
        for (int i = 0; i <= 10; i++) {
            series.add(i, i * 3);
        }

        LineChart chart = LineChart.builder()
                .width(40).height(10)
                .xRange(0, 10)
                .build();
        chart.addSeries(series);

        String rendered = chart.render();
        // X-axis should show 0 and 10 as bounds
        assertTrue("Should contain X min tick '0'", rendered.contains("0"));
        assertTrue("Should contain X max tick '10'", rendered.contains("10"));
        // Should NOT contain values beyond the explicit range
        assertFalse("Should not extend beyond 10", rendered.contains("12"));
    }

    @Test
    public void testExplicitYRange() {
        DataSeries series = new DataSeries("data");
        series.add(0, 35);
        series.add(5, 38);
        series.add(10, 32);

        LineChart chart = LineChart.builder()
                .width(40).height(10)
                .yRange(30, 40)
                .build();
        chart.addSeries(series);

        String rendered = chart.render();
        // Y-axis should show 30 and 40 as bounds
        assertTrue("Should contain Y min '30'",
                rendered.contains("30"));
        assertTrue("Should contain Y max '40'",
                rendered.contains("40"));
    }

    @Test
    public void testYMinAnchoredAtZero() {
        DataSeries series = new DataSeries("data");
        series.add(0, 50);
        series.add(1, 80);

        LineChart chart = LineChart.builder()
                .width(40).height(10)
                .yMin(0)
                .build();
        chart.addSeries(series);

        String rendered = chart.render();
        // Y-axis should start at 0
        assertTrue("Y-axis should include 0", rendered.contains("0"));
    }

    // --- Viewport tests (#594) ---

    @Test
    public void testViewportShowsSubsetOfData() {
        DataSeries series = new DataSeries("data");
        for (int i = 0; i < 20; i++) {
            series.add(i, i * 5);
        }

        // Show only 5 data points
        LineChart chart = LineChart.builder()
                .width(40).height(10)
                .viewportSize(5)
                .build();
        chart.addSeries(series);
        chart.scrollToStart();

        String rendered = chart.render();
        // X-axis should show range around 0-4 (first 5 points), not 0-19
        assertTrue("Should contain tick near 0", rendered.contains("0"));
        assertFalse("Should NOT contain tick at 19", rendered.contains("19."));
    }

    @Test
    public void testViewportViaBuilder() {
        DataSeries series = new DataSeries("data");
        for (int i = 0; i < 100; i++) {
            series.add(i, Math.sin(i * 0.1) * 10);
        }

        // Builder-set viewport should work
        LineChart chart = LineChart.builder()
                .width(50).height(10)
                .viewportSize(10)
                .build();
        chart.addSeries(series);

        // Default auto mode shows latest points (90-99)
        String rendered = chart.render();
        assertNotNull(rendered);
        assertFalse("Chart should not be empty", rendered.isEmpty());
    }

    @Test
    public void testScrollLeftShiftsWindow() {
        DataSeries series = new DataSeries("data");
        for (int i = 0; i < 20; i++) {
            series.add(i, i);
        }

        LineChart chart = LineChart.builder()
                .width(40).height(10)
                .viewportSize(5)
                .build();
        chart.addSeries(series);

        // Start at the end (auto mode: points 15-19)
        String renderedEnd = chart.render();

        // Scroll to start
        chart.scrollToStart();
        String renderedStart = chart.render();

        // The two renders should be different (different X range)
        assertFalse("Scrolled chart should differ from end-of-data chart",
                renderedEnd.equals(renderedStart));
    }

    @Test
    public void testScrollToEndShowsLatest() {
        DataSeries series = new DataSeries("data");
        for (int i = 0; i < 50; i++) {
            series.add(i, i * 2);
        }

        LineChart chart = LineChart.builder()
                .width(40).height(10)
                .viewportSize(10)
                .build();
        chart.addSeries(series);

        // scrollToEnd should show latest data
        chart.scrollToEnd();
        String rendered = chart.render();
        assertNotNull(rendered);
        // The last data point X value is 49, should be visible in the X-axis
        assertTrue("Should show data near the end",
                rendered.contains("49") || rendered.contains("48") || rendered.contains("50"));
    }

    @Test
    public void testNoViewportShowsAllData() {
        DataSeries series = new DataSeries("data");
        for (int i = 0; i < 20; i++) {
            series.add(i, i);
        }

        // No viewport set -- default behavior
        LineChart chart = LineChart.builder()
                .width(50).height(10)
                .build();
        chart.addSeries(series);

        String rendered = chart.render();
        // Should show full range including 0 and close to 20
        assertTrue("Should contain tick near 0", rendered.contains("0"));
    }

    @Test
    public void testNoTitleByDefault() {
        DataSeries series = new DataSeries("data");
        series.add(0, 0);
        series.add(1, 1);

        LineChart chart = LineChart.builder()
                .width(40)
                .height(10)
                .build();
        chart.addSeries(series);

        String rendered = chart.render();
        // Without a title, the first line should not be empty padding
        String firstLine = rendered.split("\n")[0];
        assertFalse("First line should not be blank without title",
                firstLine.trim().isEmpty());
    }

    /**
     * Regression test for #595: viewport clipping with braille style caused
     * ArrayIndexOutOfBoundsException in BrailleEncoder.dotBit() when data
     * points fell outside the visible X range.
     */
    @Test
    public void testBrailleViewportDoesNotThrow() {
        LineChart chart = LineChart.builder()
                .width(80).height(20)
                .style(ChartStyle.BRAILLE)
                .viewportSize(10)
                .build();

        DataSeries series = new DataSeries("test");
        for (int i = 0; i < 21; i++) {
            series.add(i, 35.0 + Math.random() * 0.1);
        }
        chart.addSeries(series);

        // Should not throw AIOOBE
        String output = chart.render();
        assertNotNull(output);
        assertFalse(output.isEmpty());

        // Should still contain braille characters in the visible area
        boolean hasBraille = false;
        for (char c : output.toCharArray()) {
            if (c >= 0x2800 && c <= 0x28FF) {
                hasBraille = true;
                break;
            }
        }
        assertTrue("Viewport braille chart should contain braille characters", hasBraille);
    }

    /**
     * Verify viewport clipping works with standard (non-braille) styles too.
     * plotStandard() had the same out-of-range issue but was masked by
     * Canvas.set() bounds checking.
     */
    @Test
    public void testStandardViewportSkipsOutOfRange() {
        LineChart chart = LineChart.builder()
                .width(60).height(15)
                .style(ChartStyle.UNICODE)
                .viewportSize(5)
                .build();

        DataSeries series = new DataSeries("test");
        for (int i = 0; i < 30; i++) {
            series.add(i, i * 2.0);
        }
        chart.addSeries(series);
        chart.scrollToStart();

        String output = chart.render();
        assertNotNull(output);
        assertFalse(output.isEmpty());
    }
}

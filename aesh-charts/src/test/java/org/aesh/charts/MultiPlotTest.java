package org.aesh.charts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.aesh.charts.common.ChartStyle;
import org.aesh.charts.common.DataSeries;
import org.aesh.charts.layout.MultiPlot;
import org.aesh.charts.linechart.LineChart;
import org.junit.Test;

public class MultiPlotTest {

    private static LineChart chart(String name, double... values) {
        LineChart chart = LineChart.builder()
                .width(40).height(8)
                .style(ChartStyle.UNICODE)
                .showLegend(false)
                .build();
        chart.addSeries(DataSeries.ofValues(name, values));
        return chart;
    }

    @Test
    public void testEmptyPlot() {
        MultiPlot plot = MultiPlot.builder().build();
        assertEquals("", plot.render());
    }

    @Test
    public void testStackedCharts() {
        MultiPlot plot = MultiPlot.builder().width(40)
                .style(ChartStyle.UNICODE).build();
        plot.addChart(chart("cpu", 10, 20, 30, 20, 10));
        plot.addChart(chart("mem", 5, 15, 25, 15, 5));

        String output = plot.render();
        assertFalse("Stacked output expected", output.isEmpty());
        // Data range ticks from both charts should appear
        assertTrue(output.contains("30") || output.contains("25"));
        // Separator line of ─ between the two charts
        assertTrue("Separator expected", output.contains("\u2500\u2500\u2500"));
    }

    @Test
    public void testSeparatorMatchesWidth() {
        MultiPlot plot = MultiPlot.builder().width(40)
                .style(ChartStyle.ASCII).build();
        plot.addChart(chart("a", 1, 2, 3));
        plot.addChart(chart("b", 3, 2, 1));

        String output = plot.render();
        boolean found = false;
        for (String line : output.split("\n")) {
            if (line.matches("-+")) {
                assertEquals(40, line.length());
                found = true;
            }
        }
        assertTrue("Width-40 separator expected", found);
    }

    @Test
    public void testScrollFansOut() {
        MultiPlot plot = MultiPlot.builder().width(40)
                .style(ChartStyle.UNICODE).build();
        LineChart c1 = LineChart.builder()
                .width(40).height(8)
                .style(ChartStyle.UNICODE)
                .showLegend(false)
                .viewportSize(3)
                .build();
        DataSeries s = new DataSeries("s");
        for (int i = 0; i < 10; i++)
            s.add(i, i);
        c1.addSeries(s);
        plot.addChart(c1);

        String before = plot.render();
        plot.scrollLeft(100);
        String after = plot.render();
        assertFalse("Scroll should change rendering", before.equals(after));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWidthZeroRejected() {
        MultiPlot.builder().width(0);
    }

    @Test
    public void testSeparatorFollowsPlotWidthForNarrowChildren() {
        MultiPlot plot = MultiPlot.builder().width(80)
                .style(ChartStyle.ASCII).build();
        plot.addChart(chart("a", 1, 2, 3));
        plot.addChart(chart("b", 3, 2, 1));

        String output = plot.render();
        boolean found = false;
        for (String line : output.split("\n")) {
            if (line.matches("-+")) {
                assertEquals(80, line.length());
                found = true;
            }
        }
        assertTrue("Width-80 separator expected", found);
    }
}

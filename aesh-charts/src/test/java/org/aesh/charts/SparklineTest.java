package org.aesh.charts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.aesh.charts.sparkline.Sparkline;
import org.junit.Test;

public class SparklineTest {

    @Test
    public void testSingleLineSparkline() {
        Sparkline spark = Sparkline.builder().width(10).height(1).build();
        spark.addAll(0, 2, 4, 6, 8, 6, 4, 2, 0);
        String output = spark.render();
        assertNotNull(output);
        assertFalse(output.isEmpty());
        // Should be a single line of block characters
        assertFalse(output.contains("\n"));
        // Should have 9 characters (one per data point)
        assertEquals(9, output.length());
    }

    @Test
    public void testMultiLineSparkline() {
        Sparkline spark = Sparkline.builder().width(5).height(3).build();
        spark.addAll(1, 5, 10, 3, 7);
        String output = spark.render();
        assertNotNull(output);
        // Should have 3 lines
        String[] lines = output.split("\n");
        assertEquals(3, lines.length);
    }

    @Test
    public void testEmptySparkline() {
        Sparkline spark = Sparkline.builder().build();
        assertEquals("", spark.render());
    }

    @Test
    public void testConstantValues() {
        Sparkline spark = Sparkline.builder().width(5).height(1).build();
        spark.addAll(5, 5, 5, 5, 5);
        String output = spark.render();
        assertNotNull(output);
        assertEquals(5, output.length());
    }

    @Test
    public void testWidthLimitsData() {
        Sparkline spark = Sparkline.builder().width(3).height(1).build();
        spark.addAll(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        String output = spark.render();
        // Width is 3, so only last 3 values should be shown
        assertEquals(3, output.length());
    }
}

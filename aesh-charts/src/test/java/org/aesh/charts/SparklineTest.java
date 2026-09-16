package org.aesh.charts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.aesh.charts.canvas.BlockEncoder;
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
    public void testAllNegativeValues() {
        Sparkline spark = Sparkline.builder().width(5).height(1).build();
        spark.addAll(-5, -4, -3, -2, -1);
        String output = spark.render();
        assertNotNull(output);
        assertEquals(5, output.length());
        // Range must span min..max: lowest maps to lowest block, highest to full
        assertEquals(BlockEncoder.forFraction(0.0), output.charAt(0));
        assertEquals(BlockEncoder.forFraction(1.0), output.charAt(4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWidthZeroRejected() {
        Sparkline.builder().width(0);
    }

    @Test
    public void testDescendingData() {
        Sparkline spark = Sparkline.builder().width(5).height(1).build();
        spark.addAll(8, 6, 4, 2, 0);
        String output = spark.render();
        assertEquals(5, output.length());
        // Highest first, lowest last
        assertEquals(BlockEncoder.forFraction(1.0), output.charAt(0));
        assertEquals(BlockEncoder.forFraction(0.0), output.charAt(4));
    }

    @Test
    public void testColorWrapping() {
        Sparkline spark = Sparkline.builder().width(3).height(1)
                .color("\u001B[31m").build();
        spark.addAll(1, 2, 3);
        String output = spark.render();
        assertTrue(output.startsWith("\u001B[31m"));
        assertTrue(output.endsWith("\u001B[0m"));
        assertEquals(3 + "\u001B[31m".length() + "\u001B[0m".length(), output.length());
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

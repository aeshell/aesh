package org.aesh.charts.axis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for Axis auto-range and tick generation (#585).
 */
public class AxisTest {

    @Test
    public void testAutoRange_0to20() {
        Axis axis = new Axis().tickCount(5);
        axis.autoRange(0, 20);
        assertEquals("min should be 0", 0.0, axis.min(), 0.001);
        assertEquals("max should be 20", 20.0, axis.max(), 0.001);

        double[] ticks = axis.tickValues();
        assertEquals(5, ticks.length);
        assertEquals(0.0, ticks[0], 0.001);
        assertEquals(5.0, ticks[1], 0.001);
        assertEquals(10.0, ticks[2], 0.001);
        assertEquals(15.0, ticks[3], 0.001);
        assertEquals(20.0, ticks[4], 0.001);
    }

    @Test
    public void testAutoRange_0to100() {
        Axis axis = new Axis().tickCount(5);
        axis.autoRange(0, 100);
        assertEquals(0.0, axis.min(), 0.001);
        assertEquals(100.0, axis.max(), 0.001);
    }

    @Test
    public void testAutoRange_3to97() {
        Axis axis = new Axis().tickCount(5);
        axis.autoRange(3, 97);
        // Should round to nice bounds
        assertTrue("min should be <= 3", axis.min() <= 3);
        assertTrue("max should be >= 97", axis.max() >= 97);
        // Shouldn't have excessive padding
        assertTrue("min should be >= -10", axis.min() >= -10);
        assertTrue("max should be <= 110", axis.max() <= 110);
    }

    @Test
    public void testAutoRange_negativeToPositive() {
        Axis axis = new Axis().tickCount(5);
        axis.autoRange(-10, 10);
        assertEquals(-10.0, axis.min(), 0.001);
        assertEquals(10.0, axis.max(), 0.001);
    }

    @Test
    public void testAutoRange_sameValues() {
        Axis axis = new Axis().tickCount(5);
        axis.autoRange(5, 5);
        assertEquals(4.0, axis.min(), 0.001);
        assertEquals(6.0, axis.max(), 0.001);
    }

    @Test
    public void testAutoRange_smallRange() {
        Axis axis = new Axis().tickCount(5);
        axis.autoRange(0.1, 0.9);
        assertTrue("min should be <= 0.1", axis.min() <= 0.1);
        assertTrue("max should be >= 0.9", axis.max() >= 0.9);
    }

    @Test
    public void testAutoRange_largeRange() {
        Axis axis = new Axis().tickCount(5);
        axis.autoRange(0, 1000);
        assertEquals(0.0, axis.min(), 0.001);
        assertEquals(1000.0, axis.max(), 0.001);
    }

    @Test
    public void testNiceNum() {
        // Ceiling mode (round=false)
        assertEquals(1.0, Axis.niceNum(0.7, false), 0.001);
        assertEquals(1.0, Axis.niceNum(1.0, false), 0.001);
        assertEquals(2.0, Axis.niceNum(1.5, false), 0.001);
        assertEquals(5.0, Axis.niceNum(3.0, false), 0.001);
        assertEquals(5.0, Axis.niceNum(5.0, false), 0.001);
        assertEquals(10.0, Axis.niceNum(7.0, false), 0.001);
        assertEquals(50.0, Axis.niceNum(30.0, false), 0.001);
        assertEquals(200.0, Axis.niceNum(150.0, false), 0.001);

        // Round mode (round=true)
        assertEquals(1.0, Axis.niceNum(1.2, true), 0.001);
        assertEquals(2.0, Axis.niceNum(2.0, true), 0.001);
        assertEquals(5.0, Axis.niceNum(4.0, true), 0.001);
        assertEquals(10.0, Axis.niceNum(8.0, true), 0.001);
    }
}

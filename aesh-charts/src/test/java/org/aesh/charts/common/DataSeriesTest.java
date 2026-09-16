package org.aesh.charts.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DataSeriesTest {

    @Test
    public void testOfValuesIndexing() {
        DataSeries s = DataSeries.ofValues("d", 10.0, 20.0, 30.0);
        assertEquals(3, s.size());
        assertEquals(10.0, s.yAt(0), 0.0);
        assertEquals(30.0, s.yAt(2), 0.0);
    }

    @Test
    public void testMinMax() {
        DataSeries s = DataSeries.ofValues("d", 5.0, -2.0, 8.0);
        assertEquals(-2.0, s.yMin(), 0.0);
        assertEquals(8.0, s.yMax(), 0.0);
    }

    @Test
    public void testAddXY() {
        DataSeries s = new DataSeries("d");
        s.add(100.0, 1.5);
        s.add(200.0, 2.5);
        assertEquals(2, s.size());
        assertEquals(100.0, s.xAt(0), 0.0);
        assertEquals(200.0, s.xAt(1), 0.0);
        assertEquals(100.0, s.xMin(), 0.0);
        assertEquals(200.0, s.xMax(), 0.0);
    }
}

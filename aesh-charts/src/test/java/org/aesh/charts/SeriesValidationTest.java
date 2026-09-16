package org.aesh.charts;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.aesh.charts.common.DataSeries;
import org.aesh.charts.common.TimeSeries;
import org.junit.Test;

public class SeriesValidationTest {

    @Test(expected = IllegalArgumentException.class)
    public void testDataSeriesArrayMismatchRejected() {
        new DataSeries("bad", new double[] { 1, 2, 3 }, new double[] { 1, 2 });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDataSeriesListMismatchRejected() {
        List<Double> x = Arrays.asList(1.0, 2.0);
        List<Double> y = Arrays.asList(1.0);
        new DataSeries("bad", x, y);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTimeSeriesArrayMismatchRejected() {
        TimeSeries.of("bad", new long[] { 1, 2 }, new double[] { 1.0 });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTimeSeriesListMismatchRejected() {
        List<Instant> ts = Arrays.asList(Instant.ofEpochMilli(1));
        List<Double> vs = Arrays.asList(1.0, 2.0);
        TimeSeries.of("bad", ts, vs);
    }
}

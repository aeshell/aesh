package org.aesh.charts.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class LegendTest {

    @Test
    public void testAsciiDash() {
        String out = Legend.render(Arrays.asList("a", "b"),
                Arrays.asList(null, null), ChartStyle.ASCII);
        assertEquals("-- a  -- b", out);
    }

    @Test
    public void testUnicodeDash() {
        String out = Legend.render(Collections.singletonList("cpu"),
                Collections.singletonList("\u001B[31m"), ChartStyle.UNICODE);
        assertEquals("\u001B[31m\u2500\u2500\u001B[0m cpu", out);
    }

    @Test
    public void testEmpty() {
        assertEquals("", Legend.render(Collections.emptyList(),
                Collections.emptyList(), ChartStyle.UNICODE));
    }

    @Test
    public void testMarkerEntriesUseSymbols() {
        Marker m = Marker.at(1, 2).label("peak").symbol('!').legendName("Alerts");
        String out = Legend.render(Collections.singletonList("s"),
                Collections.singletonList(null), ChartStyle.UNICODE,
                Collections.singletonList(m));
        assertTrue(out.contains("── s"));
        assertTrue(out.contains("! Alerts"));
    }

    @Test
    public void testMarkerEntriesDeduped() {
        Marker a = Marker.at(1, 1).legendName("X");
        Marker b = Marker.at(2, 2).legendName("X");
        String out = Legend.render(Collections.emptyList(), Collections.emptyList(),
                ChartStyle.UNICODE, Arrays.asList(a, b));
        assertEquals(1, out.split("X", -1).length - 1);
    }

    @Test
    public void testMarkersWithoutLegendNameSkipped() {
        Marker m = Marker.at(1, 1).label("ann");
        String out = Legend.render(Collections.emptyList(), Collections.emptyList(),
                ChartStyle.UNICODE, Collections.singletonList(m));
        assertEquals("", out);
    }

    @Test
    public void testNullMarkers() {
        String out = Legend.render(Collections.singletonList("s"),
                Collections.singletonList(null), ChartStyle.UNICODE, null);
        assertEquals("\u2500\u2500 s", out);
    }
}

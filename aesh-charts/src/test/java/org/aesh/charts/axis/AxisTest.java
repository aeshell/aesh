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
    public void testLabelWidthWithoutLabel() {
        Axis axis = new Axis().tickCount(5);
        axis.autoRange(0, 100);
        int widthNoLabel = axis.labelWidth();

        axis.label("CPU %");
        int widthWithLabel = axis.labelWidth();

        assertTrue("labelWidth with label should be larger",
                widthWithLabel > widthNoLabel);
        assertEquals("label should add 2 chars (column + gap)",
                widthNoLabel + 2, widthWithLabel);
    }

    @Test
    public void testYAxisLabelRendered() {
        Axis axis = new Axis().tickCount(3);
        axis.autoRange(0, 100);
        axis.label("Val");

        // Create a canvas large enough to hold the axis
        org.aesh.charts.canvas.Canvas canvas = new org.aesh.charts.canvas.Canvas(20, 10, false);
        org.aesh.charts.common.ChartStyle style = org.aesh.charts.common.ChartStyle.UNICODE;

        int x = axis.labelWidth();
        axis.drawYAxis(canvas, x, 0, 9, style);

        // The label "Val" should appear vertically in the rendered output
        String rendered = canvas.render();
        assertTrue("Rendered output should contain 'V'", rendered.contains("V"));
        assertTrue("Rendered output should contain 'a'", rendered.contains("a"));
        assertTrue("Rendered output should contain 'l'", rendered.contains("l"));
    }

    @Test
    public void testXAxisLastTickLabelNotClipped() {
        Axis axis = new Axis().tickCount(5);
        axis.autoRange(0, 20);

        // Create a canvas where the right edge is tight
        int canvasWidth = 30;
        org.aesh.charts.canvas.Canvas canvas = new org.aesh.charts.canvas.Canvas(canvasWidth, 4, false);
        org.aesh.charts.common.ChartStyle style = org.aesh.charts.common.ChartStyle.UNICODE;

        axis.drawXAxis(canvas, 0, canvasWidth - 1, 0, style);

        String rendered = canvas.render();
        // After autoRange, interval-derived precision: range 0-20, interval=5 → 0 decimals (#593)
        // Last tick label "20" should be fully visible, not clipped
        assertTrue("Last tick label '20' should be fully rendered",
                rendered.contains("20"));
        // First tick label should also be present
        assertTrue("First tick label '0' should be rendered",
                rendered.contains("0"));
    }

    @Test
    public void testTickFormatterOverridesDefault() {
        Axis axis = new Axis().tickCount(3);
        axis.autoRange(0, 100);

        // After autoRange, interval-derived precision applies (#593):
        // range 0-100, tickCount 3: interval=50, autoDecimalPlaces=0
        assertEquals("0", axis.formatTick(0));
        assertEquals("50", axis.formatTick(50));

        // Custom formatter takes priority over autoDecimalPlaces
        axis.tickFormatter(v -> "item-" + v.intValue());
        assertEquals("item-0", axis.formatTick(0));
        assertEquals("item-50", axis.formatTick(50));
        assertEquals("item-100", axis.formatTick(100));
    }

    @Test
    public void testTickFormatterAffectsLabelWidth() {
        Axis axis = new Axis().tickCount(3);
        axis.autoRange(0, 10);

        int defaultWidth = axis.labelWidth();

        // Set a formatter that produces wider labels
        axis.tickFormatter(v -> "category-" + v.intValue());
        int customWidth = axis.labelWidth();

        assertTrue("Custom formatter with wider labels should increase labelWidth",
                customWidth > defaultWidth);
    }

    // --- #593: Auto-precision tick formatting for narrow data ranges ---

    @Test
    public void testNarrowRangeTickPrecision() {
        // Data range 35.00-35.12: adjacent ticks must not produce duplicate labels
        Axis axis = new Axis().tickCount(5);
        axis.autoRange(35.00, 35.12);

        double[] ticks = axis.tickValues();
        String[] labels = new String[ticks.length];
        for (int i = 0; i < ticks.length; i++) {
            labels[i] = axis.formatTick(ticks[i]);
        }

        // All adjacent labels must be distinct
        for (int i = 1; i < labels.length; i++) {
            assertTrue("Adjacent tick labels must differ: labels[" + (i - 1) + "]=\""
                    + labels[i - 1] + "\" vs labels[" + i + "]=\"" + labels[i] + "\"",
                    !labels[i - 1].equals(labels[i]));
        }

        // Labels should show at least 2 decimal places for this interval
        for (String lbl : labels) {
            int dot = lbl.indexOf('.');
            assertTrue("Label should have a decimal point: " + lbl, dot >= 0);
            int decimals = lbl.length() - dot - 1;
            assertTrue("Label should have >= 2 decimals for narrow range: " + lbl,
                    decimals >= 2);
        }
    }

    @Test
    public void testWideRangeTickPrecision() {
        // Data range 0-1000: tick interval is large, should use 0 decimals
        Axis axis = new Axis().tickCount(5);
        axis.autoRange(0, 1000);

        assertEquals("0", axis.formatTick(0));
        assertEquals("250", axis.formatTick(250));
        assertEquals("500", axis.formatTick(500));
        assertEquals("1000", axis.formatTick(1000));
    }

    @Test
    public void testMediumRangeTickPrecision() {
        // Data range 0-2: tick interval ~0.5, should use 1 decimal
        Axis axis = new Axis().tickCount(5);
        axis.autoRange(0, 2);

        double[] ticks = axis.tickValues();
        for (double tick : ticks) {
            String lbl = axis.formatTick(tick);
            int dot = lbl.indexOf('.');
            assertTrue("Label should have a decimal: " + lbl, dot >= 0);
            int decimals = lbl.length() - dot - 1;
            assertEquals("Should have exactly 1 decimal for interval ~0.5: " + lbl,
                    1, decimals);
        }
    }

    @Test
    public void testAutoDecimalDoesNotOverrideCustomFormatter() {
        Axis axis = new Axis().tickCount(5);
        axis.autoRange(35.00, 35.12);

        // Custom formatter should take priority over autoDecimalPlaces
        axis.tickFormatter(v -> String.valueOf(v.intValue()));
        assertEquals("35", axis.formatTick(35.00));
        assertEquals("35", axis.formatTick(35.05));
    }

    @Test
    public void testAutoDecimalDoesNotOverrideFormatPattern() {
        Axis axis = new Axis().tickCount(5);
        axis.autoRange(35.00, 35.12);

        // Explicit format pattern should take priority over autoDecimalPlaces
        axis.format("%.4f");
        assertEquals("35.0000", axis.formatTick(35.0));
        assertEquals("35.0500", axis.formatTick(35.05));
    }

    @Test
    public void testManualRangeUsesOldRules() {
        // When autoRange() is NOT called, old magnitude-based rules apply
        Axis axis = new Axis().tickCount(5);
        axis.min(35.00).max(35.15);

        // Value >= 1 should use %.1f (old rule)
        assertEquals("35.0", axis.formatTick(35.0));
        assertEquals("35.1", axis.formatTick(35.05));
    }

    @Test
    public void testVeryNarrowRangeTickPrecision() {
        // Data range 100.000-100.003: extremely narrow, needs many decimals
        Axis axis = new Axis().tickCount(5);
        axis.autoRange(100.000, 100.003);

        double[] ticks = axis.tickValues();
        String[] labels = new String[ticks.length];
        for (int i = 0; i < ticks.length; i++) {
            labels[i] = axis.formatTick(ticks[i]);
        }

        // All adjacent labels must be distinct
        for (int i = 1; i < labels.length; i++) {
            assertTrue("Adjacent tick labels must differ: labels[" + (i - 1) + "]=\""
                    + labels[i - 1] + "\" vs labels[" + i + "]=\"" + labels[i] + "\"",
                    !labels[i - 1].equals(labels[i]));
        }
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

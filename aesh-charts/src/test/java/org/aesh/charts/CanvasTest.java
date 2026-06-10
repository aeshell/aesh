package org.aesh.charts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.aesh.charts.canvas.BlockEncoder;
import org.aesh.charts.canvas.BrailleEncoder;
import org.aesh.charts.canvas.Canvas;
import org.junit.Test;

public class CanvasTest {

    @Test
    public void testBasicCanvas() {
        Canvas c = new Canvas(5, 3);
        c.set(0, 0, 'A');
        c.set(4, 2, 'Z');
        assertEquals('A', c.get(0, 0));
        assertEquals('Z', c.get(4, 2));
        assertEquals(' ', c.get(2, 1));
    }

    @Test
    public void testWriteString() {
        Canvas c = new Canvas(10, 1);
        c.writeString(2, 0, "hello");
        assertEquals('h', c.get(2, 0));
        assertEquals('o', c.get(6, 0));
    }

    @Test
    public void testRender() {
        Canvas c = new Canvas(3, 2);
        c.set(0, 0, 'A');
        c.set(1, 0, 'B');
        c.set(2, 0, 'C');
        c.set(0, 1, 'D');
        c.set(1, 1, 'E');
        c.set(2, 1, 'F');
        assertEquals("ABC\nDEF", c.render());
    }

    @Test
    public void testBrailleEncoder() {
        // Empty braille
        assertEquals('\u2800', BrailleEncoder.empty());
        // Single dot (top-left)
        assertEquals('\u2801', BrailleEncoder.toChar(0x01));
        // All dots
        assertEquals('\u28FF', BrailleEncoder.toChar(0xFF));
    }

    @Test
    public void testBrailleCanvas() {
        Canvas c = new Canvas(2, 2, true);
        // Set a dot in the top-left sub-cell of cell (0,0)
        c.setBrailleDot(0, 0, null);
        char ch = c.get(0, 0);
        assertTrue("Should be a braille char", ch >= 0x2800 && ch <= 0x28FF);
        assertEquals('\u2801', ch); // top-left dot only
    }

    @Test
    public void testBlockEncoder() {
        assertEquals(' ', BlockEncoder.forFraction(0.0));
        assertEquals('\u2588', BlockEncoder.forFraction(1.0)); // full block
        assertEquals('\u2584', BlockEncoder.forFraction(0.5)); // half block
        // Level-based
        assertEquals(' ', BlockEncoder.forLevel(0));
        assertEquals('\u2581', BlockEncoder.forLevel(1));
        assertEquals('\u2588', BlockEncoder.forLevel(8));
    }

    @Test
    public void testHorizontalLine() {
        Canvas c = new Canvas(5, 1);
        c.horizontalLine(0, 0, 5, '-');
        assertEquals("-----", c.render());
    }

    @Test
    public void testVerticalLine() {
        Canvas c = new Canvas(1, 3);
        c.verticalLine(0, 0, 3, '|');
        assertEquals("|\n|\n|", c.render());
    }

    @Test
    public void testOutOfBoundsIgnored() {
        Canvas c = new Canvas(3, 3);
        // These should not throw
        c.set(-1, 0, 'X');
        c.set(0, -1, 'X');
        c.set(10, 0, 'X');
        c.set(0, 10, 'X');
        assertEquals(' ', c.get(-1, 0));
        assertEquals(' ', c.get(10, 10));
    }
}

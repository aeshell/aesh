package org.aesh.charts.canvas;

/**
 * Encodes sub-cell coordinates into Unicode Braille characters (U+2800-U+28FF).
 * <p>
 * Each braille character represents a 2-wide x 4-tall dot matrix, giving
 * 2x horizontal and 4x vertical resolution per terminal character cell.
 * <p>
 * Dot positions and bit mapping:
 *
 * <pre>
 *   Col 0  Col 1       Bit 0  Bit 3
 *   Row 0  Row 0       Bit 1  Bit 4
 *   Row 1  Row 1       Bit 2  Bit 5
 *   Row 2  Row 2       Bit 6  Bit 7
 * </pre>
 *
 * Character = U+2800 + (bit0 | bit1 | ... | bit7)
 */
public final class BrailleEncoder {

    /** Width of a braille cell in sub-pixels. */
    public static final int CELL_WIDTH = 2;

    /** Height of a braille cell in sub-pixels. */
    public static final int CELL_HEIGHT = 4;

    /** Base Unicode code point for braille patterns. */
    private static final int BRAILLE_BASE = 0x2800;

    /**
     * Bit positions for each dot in the braille cell.
     * Index: [row][col], where row 0 is top, col 0 is left.
     */
    private static final int[][] DOT_BITS = {
            { 0x01, 0x08 }, // row 0
            { 0x02, 0x10 }, // row 1
            { 0x04, 0x20 }, // row 2
            { 0x40, 0x80 }, // row 3
    };

    private BrailleEncoder() {
    }

    /**
     * Get the bit mask for a specific dot position.
     *
     * @param row row within the cell (0-3, top to bottom)
     * @param col column within the cell (0-1, left to right)
     * @return the bit mask
     */
    public static int dotBit(int row, int col) {
        return DOT_BITS[row][col];
    }

    /**
     * Convert a bit pattern to the corresponding braille character.
     *
     * @param bits the combined bit pattern (0x00-0xFF)
     * @return the braille character
     */
    public static char toChar(int bits) {
        return (char) (BRAILLE_BASE + (bits & 0xFF));
    }

    /**
     * Return the empty braille character (no dots set).
     */
    public static char empty() {
        return (char) BRAILLE_BASE;
    }
}

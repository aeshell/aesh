package org.aesh.charts.canvas;

/**
 * A 2D character grid for rendering chart content.
 * <p>
 * The canvas uses a coordinate system where (0,0) is the top-left corner.
 * Each cell holds a character and an optional ANSI color/style string.
 * <p>
 * For braille mode, the canvas operates at sub-cell resolution: each
 * character cell represents a 2x4 dot matrix. Use {@link #setBrailleDot}
 * to set individual dots within cells.
 */
public class Canvas {

    private final int width;
    private final int height;
    private final char[][] chars;
    private final String[][] styles;
    private final int[][] brailleBits; // only allocated for braille mode

    /**
     * Create a canvas with the given dimensions in character cells.
     *
     * @param width width in character cells
     * @param height height in character cells
     * @param braille whether to enable braille sub-cell rendering
     */
    public Canvas(int width, int height, boolean braille) {
        this.width = width;
        this.height = height;
        this.chars = new char[height][width];
        this.styles = new String[height][width];
        this.brailleBits = braille ? new int[height][width] : null;
        clear();
    }

    public Canvas(int width, int height) {
        this(width, height, false);
    }

    /** Clear the canvas to spaces. */
    public void clear() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                chars[y][x] = ' ';
                styles[y][x] = null;
            }
        }
        if (brailleBits != null) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    brailleBits[y][x] = 0;
                }
            }
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /**
     * Set a character at the given position.
     *
     * @param x column (0 = left)
     * @param y row (0 = top)
     * @param ch the character
     */
    public void set(int x, int y, char ch) {
        set(x, y, ch, null);
    }

    /**
     * Set a character with an ANSI style at the given position.
     *
     * @param x column (0 = left)
     * @param y row (0 = top)
     * @param ch the character
     * @param style ANSI escape sequence (e.g., ANSI.GREEN_TEXT), or null
     */
    public void set(int x, int y, char ch, String style) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            chars[y][x] = ch;
            styles[y][x] = style;
        }
    }

    /**
     * Get the character at the given position.
     */
    public char get(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return chars[y][x];
        }
        return ' ';
    }

    /**
     * Write a string starting at the given position.
     */
    public void writeString(int x, int y, String text) {
        writeString(x, y, text, null);
    }

    /**
     * Write a string with style starting at the given position.
     */
    public void writeString(int x, int y, String text, String style) {
        for (int i = 0; i < text.length(); i++) {
            set(x + i, y, text.charAt(i), style);
        }
    }

    /**
     * Set a braille dot at sub-cell coordinates.
     * <p>
     * The sub-cell coordinates are mapped to the containing character cell
     * and the appropriate dot bit is set.
     *
     * @param subX sub-cell X coordinate (each cell is 2 sub-pixels wide)
     * @param subY sub-cell Y coordinate (each cell is 4 sub-pixels tall)
     * @param style ANSI style for the cell
     */
    public void setBrailleDot(int subX, int subY, String style) {
        if (brailleBits == null)
            return;
        int cellX = subX / BrailleEncoder.CELL_WIDTH;
        int cellY = subY / BrailleEncoder.CELL_HEIGHT;
        if (cellX < 0 || cellX >= width || cellY < 0 || cellY >= height)
            return;
        int dotCol = subX % BrailleEncoder.CELL_WIDTH;
        int dotRow = subY % BrailleEncoder.CELL_HEIGHT;
        brailleBits[cellY][cellX] |= BrailleEncoder.dotBit(dotRow, dotCol);
        chars[cellY][cellX] = BrailleEncoder.toChar(brailleBits[cellY][cellX]);
        if (style != null) {
            styles[cellY][cellX] = style;
        }
    }

    /**
     * Draw a horizontal line.
     */
    public void horizontalLine(int x, int y, int length, char ch) {
        horizontalLine(x, y, length, ch, null);
    }

    public void horizontalLine(int x, int y, int length, char ch, String style) {
        for (int i = 0; i < length; i++) {
            set(x + i, y, ch, style);
        }
    }

    /**
     * Draw a vertical line.
     */
    public void verticalLine(int x, int y, int length, char ch) {
        verticalLine(x, y, length, ch, null);
    }

    public void verticalLine(int x, int y, int length, char ch, String style) {
        for (int i = 0; i < length; i++) {
            set(x, y + i, ch, style);
        }
    }

    /**
     * Render the canvas to a multi-line string.
     * <p>
     * Each cell is rendered with its ANSI style (if any), and styles are
     * reset after each styled run to prevent color bleeding.
     *
     * @return the rendered string
     */
    public String render() {
        StringBuilder sb = new StringBuilder(width * height * 2);
        for (int y = 0; y < height; y++) {
            if (y > 0)
                sb.append('\n');
            String currentStyle = null;
            for (int x = 0; x < width; x++) {
                String cellStyle = styles[y][x];
                if (cellStyle != null && !cellStyle.equals(currentStyle)) {
                    sb.append(cellStyle);
                    currentStyle = cellStyle;
                } else if (cellStyle == null && currentStyle != null) {
                    sb.append("\u001B[0m"); // ANSI reset
                    currentStyle = null;
                }
                sb.append(chars[y][x]);
            }
            if (currentStyle != null) {
                sb.append("\u001B[0m"); // reset at end of line
            }
        }
        return sb.toString();
    }
}

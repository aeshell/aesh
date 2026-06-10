package org.aesh.charts.common;

/**
 * Rendering style for terminal charts.
 * <p>
 * Controls which character sets are used for drawing:
 * <ul>
 * <li>{@link #ASCII} — basic ASCII characters, works on all terminals</li>
 * <li>{@link #UNICODE} — box drawing and block elements for cleaner appearance</li>
 * <li>{@link #BRAILLE} — braille dot patterns for high-resolution line charts (2x4 sub-cell resolution)</li>
 * </ul>
 */
public enum ChartStyle {
    /** ASCII characters only: |, -, +, #, * */
    ASCII,

    /** Unicode box drawing (│─┌┐└┘) and block elements (▁▂▃▄▅▆▇█) */
    UNICODE,

    /** Unicode braille patterns (U+2800-U+28FF) for 2x4 sub-cell resolution */
    BRAILLE;

    /** Vertical axis character. */
    public char verticalLine() {
        return this == ASCII ? '|' : '\u2502'; // │
    }

    /** Horizontal axis character. */
    public char horizontalLine() {
        return this == ASCII ? '-' : '\u2500'; // ─
    }

    /** Bottom-left corner. */
    public char cornerBottomLeft() {
        return this == ASCII ? '+' : '\u2514'; // └
    }

    /** Bottom-right corner. */
    public char cornerBottomRight() {
        return this == ASCII ? '+' : '\u2518'; // ┘
    }

    /** T-junction pointing left (right axis). */
    public char teeLeft() {
        return this == ASCII ? '+' : '\u2524'; // ┤
    }

    /** T-junction pointing right (left axis tick). */
    public char teeRight() {
        return this == ASCII ? '+' : '\u251C'; // ├
    }

    /** T-junction pointing up (bottom axis tick). */
    public char teeUp() {
        return this == ASCII ? '+' : '\u2534'; // ┴
    }

    /** Cross junction. */
    public char cross() {
        return this == ASCII ? '+' : '\u253C'; // ┼
    }

    /** Dot character for plotting points. */
    public char dot() {
        return this == ASCII ? '*' : '\u2022'; // •
    }

    /** Horizontal separator for between stacked charts. */
    public String horizontalSeparator() {
        return this == ASCII ? "-" : "\u2500"; // ─
    }
}

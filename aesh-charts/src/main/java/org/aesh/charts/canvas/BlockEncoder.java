package org.aesh.charts.canvas;

/**
 * Maps fractional values (0.0-1.0) to Unicode block element characters.
 * <p>
 * Block elements provide 8 levels of vertical fill per character cell,
 * useful for bar charts and sparklines.
 * <p>
 * Characters used (bottom-to-top fill):
 *
 * <pre>
 *   ▁ (1/8)  ▂ (2/8)  ▃ (3/8)  ▄ (4/8)
 *   ▅ (5/8)  ▆ (6/8)  ▇ (7/8)  █ (8/8)
 * </pre>
 */
public final class BlockEncoder {

    /** Block elements from 1/8 to 8/8 fill. */
    private static final char[] BLOCKS = {
            ' ', // 0/8
            '\u2581', // ▁ 1/8
            '\u2582', // ▂ 2/8
            '\u2583', // ▃ 3/8
            '\u2584', // ▄ 4/8
            '\u2585', // ▅ 5/8
            '\u2586', // ▆ 6/8
            '\u2587', // ▇ 7/8
            '\u2588', // █ 8/8
    };

    /** Full block character. */
    public static final char FULL_BLOCK = '\u2588';

    /** Number of vertical levels per cell (including empty). */
    public static final int LEVELS = BLOCKS.length;

    private BlockEncoder() {
    }

    /**
     * Get the block character for a fractional value.
     *
     * @param fraction value between 0.0 and 1.0
     * @return the corresponding block character
     */
    public static char forFraction(double fraction) {
        if (fraction <= 0.0)
            return ' ';
        if (fraction >= 1.0)
            return FULL_BLOCK;
        int level = (int) (fraction * 8);
        if (level >= BLOCKS.length)
            level = BLOCKS.length - 1;
        return BLOCKS[level];
    }

    /**
     * Get the block character for a specific level (0-8).
     *
     * @param level the fill level (0 = empty, 8 = full)
     * @return the corresponding block character
     */
    public static char forLevel(int level) {
        if (level < 0)
            return ' ';
        if (level >= BLOCKS.length)
            return FULL_BLOCK;
        return BLOCKS[level];
    }
}

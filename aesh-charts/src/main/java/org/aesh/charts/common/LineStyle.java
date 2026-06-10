package org.aesh.charts.common;

/**
 * Line rendering style for differentiating series on monochrome terminals.
 */
public enum LineStyle {
    /** Continuous line (default). */
    SOLID,

    /** Dashed line (every other point). */
    DASHED,

    /** Dotted line (every third point). */
    DOTTED
}

package org.aesh.charts.common;

/**
 * A marker on a chart — an annotated point that highlights a specific
 * data value (e.g., a change detection, regression, anomaly).
 * <p>
 * Markers are rendered as a highlighted character at the data point
 * position, optionally with a label. They can be used to indicate
 * change detections from systems like Horreum/h5m.
 * <p>
 * Example:
 *
 * <pre>
 * chart.addMarker(Marker.at(timestamp, value)
 *         .label("regression")
 *         .color(ANSI.RED_TEXT)
 *         .symbol('!'));
 * </pre>
 */
public class Marker {

    private final double x;
    private final double y;
    private String label;
    private String color;
    private char symbol = '\u25CF'; // ● filled circle

    private Marker(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Create a marker at the given data coordinates.
     */
    public static Marker at(double x, double y) {
        return new Marker(x, y);
    }

    /**
     * Create a marker at a timestamp and value (for time series charts).
     */
    public static Marker atTime(long epochMillis, double value) {
        return new Marker(epochMillis, value);
    }

    /**
     * Set the label shown near the marker point.
     */
    public Marker label(String label) {
        this.label = label;
        return this;
    }

    /**
     * Set the ANSI color for the marker symbol and label.
     */
    public Marker color(String color) {
        this.color = color;
        return this;
    }

    /**
     * Set the symbol character rendered at the marker position.
     * <p>
     * Common choices:
     * <ul>
     * <li>{@code '!'} — alert/regression</li>
     * <li>{@code '\u25CF'} — filled circle (default)</li>
     * <li>{@code '\u25B2'} — triangle up</li>
     * <li>{@code '\u25BC'} — triangle down</li>
     * <li>{@code '\u2605'} — star</li>
     * <li>{@code '\u26A0'} — warning sign</li>
     * <li>{@code 'X'} — ASCII fallback</li>
     * </ul>
     */
    public Marker symbol(char symbol) {
        this.symbol = symbol;
        return this;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public String label() {
        return label;
    }

    public String color() {
        return color;
    }

    public char symbol() {
        return symbol;
    }
}

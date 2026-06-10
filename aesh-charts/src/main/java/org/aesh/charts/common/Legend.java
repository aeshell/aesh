package org.aesh.charts.common;

import java.util.List;

/**
 * Renders a compact legend for multi-series charts.
 * <p>
 * Example output:
 *
 * <pre>
 * ── cpu0  ── cpu1  ── mem
 * </pre>
 */
public class Legend {

    private Legend() {
    }

    /**
     * Render a single-line legend from series names and colors.
     *
     * @param names series names
     * @param colors ANSI color strings (parallel to names), null entries use default
     * @param style chart style
     * @return the rendered legend string
     */
    public static String render(List<String> names, List<String> colors, ChartStyle style) {
        StringBuilder sb = new StringBuilder();
        String dash = style == ChartStyle.ASCII ? "--" : "\u2500\u2500";
        for (int i = 0; i < names.size(); i++) {
            if (i > 0)
                sb.append("  ");
            String color = (colors != null && i < colors.size()) ? colors.get(i) : null;
            if (color != null)
                sb.append(color);
            sb.append(dash);
            if (color != null)
                sb.append("\u001B[0m");
            sb.append(' ').append(names.get(i));
        }
        return sb.toString();
    }
}

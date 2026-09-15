package org.aesh.charts.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders a compact legend for multi-series charts.
 * <p>
 * Example output (series + markers):
 *
 * <pre>
 * ── cpu0  ── cpu1  ▲ RD %  ● SD
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
        return render(names, colors, style, null);
    }

    /**
     * Render a single-line legend from series names/colors plus marker entries.
     * Marker entries use their symbol instead of a dash. Markers with the same
     * {@link Marker#legendName()} are deduplicated into a single entry.
     *
     * @param names series names
     * @param colors ANSI color strings (parallel to names), null entries use default
     * @param style chart style
     * @param markers marker list (may be null or empty)
     * @return the rendered legend string
     */
    public static String render(List<String> names, List<String> colors,
            ChartStyle style, List<Marker> markers) {
        StringBuilder sb = new StringBuilder();
        String dash = style == ChartStyle.ASCII ? "--" : "\u2500\u2500";
        boolean first = true;
        for (int i = 0; i < names.size(); i++) {
            if (!first)
                sb.append("  ");
            first = false;
            String color = (colors != null && i < colors.size()) ? colors.get(i) : null;
            if (color != null)
                sb.append(color);
            sb.append(dash);
            if (color != null)
                sb.append("\u001B[0m");
            sb.append(' ').append(names.get(i));
        }
        if (markers != null) {
            for (MarkerLegendEntry entry : deduplicateMarkers(markers, style)) {
                if (!first)
                    sb.append("  ");
                first = false;
                if (entry.color != null)
                    sb.append(entry.color);
                sb.append(entry.symbol);
                if (entry.color != null)
                    sb.append("\u001B[0m");
                sb.append(' ').append(entry.name);
            }
        }
        return sb.toString();
    }

    /**
     * Deduplicate markers by legendName, keeping the first symbol and color
     * encountered for each name. Markers without a legendName are skipped.
     */
    private static List<MarkerLegendEntry> deduplicateMarkers(List<Marker> markers,
            ChartStyle style) {
        Map<String, MarkerLegendEntry> seen = new LinkedHashMap<>();
        for (Marker m : markers) {
            String name = m.legendName();
            if (name == null || name.isEmpty())
                continue;
            if (!seen.containsKey(name)) {
                char symbol = style == ChartStyle.ASCII ? 'X' : m.symbol();
                seen.put(name, new MarkerLegendEntry(name, m.color(), symbol));
            }
        }
        return new ArrayList<>(seen.values());
    }

    private static class MarkerLegendEntry {
        final String name;
        final String color;
        final char symbol;

        MarkerLegendEntry(String name, String color, char symbol) {
            this.name = name;
            this.color = color;
            this.symbol = symbol;
        }
    }
}

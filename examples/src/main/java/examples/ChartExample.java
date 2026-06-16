package examples;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.charts.barchart.BarChart;
import org.aesh.charts.barchart.Orientation;
import org.aesh.charts.common.ChartStyle;
import org.aesh.charts.common.DataSeries;
import org.aesh.charts.common.HorizontalLine;
import org.aesh.charts.common.Marker;
import org.aesh.charts.common.TimeSeries;
import org.aesh.charts.layout.MultiPlot;
import org.aesh.charts.linechart.LineChart;
import org.aesh.charts.linechart.TimeSeriesChart;
import org.aesh.charts.sparkline.Sparkline;
import org.aesh.terminal.utils.ANSI;

/**
 * Interactive demo of aesh-charts.
 * <p>
 * Run: {@code mvn -Pexamples exec:java -pl examples -Dexec.mainClass=examples.ChartExample}
 * <p>
 * Commands:
 * <ul>
 * <li>{@code chart spark} — sparkline demo</li>
 * <li>{@code chart bar} — bar chart demo</li>
 * <li>{@code chart line} — line chart with braille rendering</li>
 * <li>{@code chart change} — change detection markers demo</li>
 * <li>{@code chart compare} — multi-series comparison (2/4/8 cores)</li>
 * <li>{@code chart dashboard} — stacked multi-plot dashboard</li>
 * <li>{@code exit}</li>
 * </ul>
 */
public class ChartExample {

    public static void main(String[] args) {
        AeshConsoleRunner.builder()
                .command(ChartGroup.class)
                .prompt("[charts]$ ")
                .addExitCommand()
                .start();
    }

    @CommandDefinition(name = "chart", description = "Terminal chart demos",
            groupCommands = {
                    SparkCmd.class, BarCmd.class, LineCmd.class,
                    ChangeCmd.class, CompareCmd.class, DashboardCmd.class
            })
    public static class ChartGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            ci.println("Use: chart spark | bar | line | change | compare | dashboard");
            return CommandResult.SUCCESS;
        }
    }

    // ---- Sparkline ----

    @CommandDefinition(name = "spark", description = "Sparkline demo")
    public static class SparkCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            ci.println("Single-line sparkline:");
            Sparkline spark = Sparkline.builder().width(40).height(1).build();
            spark.addAll(4, 6, 8, 10, 8, 6, 4, 2, 0, 2, 4, 6, 8, 10, 12, 10, 8, 6, 4, 2,
                    0, 3, 6, 9, 12, 9, 6, 3, 0, 1, 4, 7, 10, 13, 10, 7, 4, 1, 0, 2);
            ci.println("  " + spark.render());

            ci.println("");
            ci.println("Multi-line sparkline (3 rows):");
            Sparkline tall = Sparkline.builder().width(30).height(3).build();
            tall.addAll(1, 5, 10, 3, 7, 2, 9, 4, 8, 1, 6, 10, 2, 8, 3, 7, 1, 9, 5, 10,
                    4, 8, 2, 6, 10, 3, 7, 1, 5, 9);
            ci.println(tall.render());
            return CommandResult.SUCCESS;
        }
    }

    // ---- Bar chart ----

    @CommandDefinition(name = "bar", description = "Bar chart demo")
    public static class BarCmd implements Command<CommandInvocation> {
        @Option(name = "horizontal", shortName = 'h', hasValue = false,
                description = "Use horizontal orientation")
        boolean horizontal;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            int w = ci.getShell().size().getWidth() - 5;
            if (w < 30) w = 60;

            BarChart chart = BarChart.builder()
                    .width(w)
                    .height(horizontal ? 6 : 15)
                    .orientation(horizontal ? Orientation.HORIZONTAL : Orientation.VERTICAL)
                    .style(ChartStyle.UNICODE)
                    .build();

            chart.addBar("GET /api", 1250, ANSI.GREEN_TEXT);
            chart.addBar("POST /api", 890, ANSI.BLUE_TEXT);
            chart.addBar("GET /health", 2100, ANSI.CYAN_TEXT);
            chart.addBar("PUT /data", 430, ANSI.YELLOW_TEXT);
            chart.addBar("DELETE", 180, ANSI.RED_TEXT);

            ci.println("Requests per second by endpoint:");
            ci.println(chart.render());
            return CommandResult.SUCCESS;
        }
    }

    // ---- Line chart ----

    @CommandDefinition(name = "line", description = "Line chart with braille rendering")
    public static class LineCmd implements Command<CommandInvocation> {
        @Option(name = "style", shortName = 's', defaultValue = "BRAILLE",
                description = "Rendering style: ASCII, UNICODE, BRAILLE")
        ChartStyle chartStyle;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            int w = ci.getShell().size().getWidth() - 5;
            if (w < 40) w = 60;

            LineChart chart = LineChart.builder()
                    .width(w).height(18)
                    .style(chartStyle)
                    .xLabel("Sample").yLabel("Value")
                    .build();

            // Generate sine wave data
            DataSeries sine = new DataSeries("sin(x)");
            DataSeries cosine = new DataSeries("cos(x)");
            for (int i = 0; i < 50; i++) {
                double x = i * 0.3;
                sine.add(x, Math.sin(x) * 10);
                cosine.add(x, Math.cos(x) * 8);
            }
            sine.color(ANSI.GREEN_TEXT);
            cosine.color(ANSI.BLUE_TEXT);

            chart.addSeries(sine);
            chart.addSeries(cosine);

            ci.println(chart.render());
            return CommandResult.SUCCESS;
        }
    }

    // ---- Change detection markers ----

    @CommandDefinition(name = "change", description = "Change detection markers demo")
    public static class ChangeCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            int w = ci.getShell().size().getWidth() - 5;
            if (w < 40) w = 60;

            LineChart chart = LineChart.builder()
                    .width(w).height(18)
                    .style(ChartStyle.BRAILLE)
                    .xLabel("Test run").yLabel("Response time (ms)")
                    .showLegend(false)
                    .build();

            // Simulated response time data with two regressions
            double[] data = {
                    120, 118, 122, 119, 121, 117, 123, 120, 119, 121,  // stable ~120ms
                    118, 122, 120, 119, 121,
                    185, 190, 188, 192, 187, 191, 189, 193, 188, 190,  // regression to ~190ms
                    192, 188, 191, 189, 190,
                    250, 255, 248, 252, 260, 253, 257, 249, 255, 251   // second regression to ~250ms
            };

            DataSeries series = DataSeries.ofValues("p99 latency", data);
            series.color(ANSI.GREEN_TEXT);
            chart.addSeries(series);

            // Threshold line
            chart.addHorizontalLine(HorizontalLine.at(200)
                    .label("SLA")
                    .color(ANSI.YELLOW_TEXT)
                    .dashed(true));

            // Mark the regressions
            chart.addMarker(Marker.at(15, 185)
                    .label("deploy v2.1")
                    .color(ANSI.RED_TEXT)
                    .symbol('!'));

            chart.addMarker(Marker.at(30, 250)
                    .label("deploy v2.3")
                    .color(ANSI.RED_TEXT)
                    .symbol('!'));

            ci.println("Response time with change detection markers:");
            ci.println(chart.render());
            ci.println("");
            ci.println("  " + ANSI.RED_TEXT + "!" + ANSI.RESET + " = change detected");
            ci.println("  " + ANSI.YELLOW_TEXT + "---" + ANSI.RESET + " = SLA threshold (200ms)");
            return CommandResult.SUCCESS;
        }
    }

    // ---- Multi-series comparison ----

    @CommandDefinition(name = "compare", description = "Multi-series comparison (2/4/8 cores)")
    public static class CompareCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            int w = ci.getShell().size().getWidth() - 5;
            if (w < 40) w = 60;

            LineChart chart = LineChart.builder()
                    .width(w).height(18)
                    .style(ChartStyle.BRAILLE)
                    .xLabel("Concurrency").yLabel("p99 latency (ms)")
                    .build();

            // Simulated latency vs concurrency for different core counts
            DataSeries cores2 = new DataSeries("2 cores");
            DataSeries cores4 = new DataSeries("4 cores");
            DataSeries cores8 = new DataSeries("8 cores");

            for (int concurrency = 10; concurrency <= 200; concurrency += 10) {
                // Higher cores = lower latency, until saturation
                cores2.add(concurrency, 50 + concurrency * 2.5 + Math.random() * 20);
                cores4.add(concurrency, 30 + concurrency * 1.2 + Math.random() * 15);
                cores8.add(concurrency, 20 + concurrency * 0.6 + Math.random() * 10);
            }

            cores2.color(ANSI.RED_TEXT);
            cores4.color(ANSI.YELLOW_TEXT);
            cores8.color(ANSI.GREEN_TEXT);

            chart.addSeries(cores2);
            chart.addSeries(cores4);
            chart.addSeries(cores8);

            // SLA line
            chart.addHorizontalLine(HorizontalLine.at(200)
                    .label("SLA")
                    .color(ANSI.CYAN_TEXT)
                    .dashed(true));

            ci.println("Latency comparison across server configurations:");
            ci.println(chart.render());
            return CommandResult.SUCCESS;
        }
    }

    // ---- Dashboard (stacked multi-plot) ----

    @CommandDefinition(name = "dashboard", description = "Stacked multi-plot dashboard")
    public static class DashboardCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            int w = ci.getShell().size().getWidth() - 5;
            if (w < 40) w = 60;

            MultiPlot plot = MultiPlot.builder()
                    .width(w)
                    .style(ChartStyle.BRAILLE)
                    .build();

            // CPU chart
            LineChart cpuChart = LineChart.builder()
                    .width(w).height(10)
                    .style(ChartStyle.BRAILLE)
                    .yLabel("CPU %")
                    .showLegend(false)
                    .build();
            DataSeries cpu = new DataSeries("CPU");
            for (int i = 0; i < 60; i++) {
                cpu.add(i, 30 + Math.sin(i * 0.2) * 20 + Math.random() * 10);
            }
            cpu.color(ANSI.GREEN_TEXT);
            cpuChart.addSeries(cpu);
            cpuChart.addHorizontalLine(HorizontalLine.at(80)
                    .label("alert")
                    .color(ANSI.RED_TEXT)
                    .dashed(true));

            // Memory chart
            LineChart memChart = LineChart.builder()
                    .width(w).height(8)
                    .style(ChartStyle.BRAILLE)
                    .yLabel("Mem MB")
                    .showLegend(false)
                    .build();
            DataSeries mem = new DataSeries("Memory");
            double memVal = 256;
            for (int i = 0; i < 60; i++) {
                memVal += Math.random() * 10 - 4; // slowly growing with noise
                if (memVal < 200) memVal = 200;
                if (memVal > 500) memVal = 500;
                mem.add(i, memVal);
            }
            mem.color(ANSI.BLUE_TEXT);
            memChart.addSeries(mem);

            // Response time chart
            LineChart rtChart = LineChart.builder()
                    .width(w).height(8)
                    .style(ChartStyle.BRAILLE)
                    .yLabel("Resp ms")
                    .build();
            DataSeries p50 = new DataSeries("p50");
            DataSeries p99 = new DataSeries("p99");
            for (int i = 0; i < 60; i++) {
                double base = 50 + Math.sin(i * 0.15) * 15;
                p50.add(i, base + Math.random() * 5);
                p99.add(i, base * 2.5 + Math.random() * 30);
            }
            p50.color(ANSI.GREEN_TEXT);
            p99.color(ANSI.RED_TEXT);
            rtChart.addSeries(p50);
            rtChart.addSeries(p99);

            plot.addChart(cpuChart);
            plot.addChart(memChart);
            plot.addChart(rtChart);

            ci.println("System Dashboard:");
            ci.println(plot.render());
            return CommandResult.SUCCESS;
        }
    }
}

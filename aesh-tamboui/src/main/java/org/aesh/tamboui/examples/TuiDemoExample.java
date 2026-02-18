/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.tamboui.examples;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.CommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.tamboui.TuiAppCommand;
import org.aesh.tamboui.TuiCommand;

import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.TickEvent;
import dev.tamboui.widgets.barchart.Bar;
import dev.tamboui.widgets.barchart.BarGroup;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.gauge.Gauge;
import dev.tamboui.widgets.sparkline.Sparkline;
import dev.tamboui.widgets.table.TableState;
import dev.tamboui.widgets.tabs.TabsState;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demo example showing TUI commands integrated with aesh.
 * Run this class and type the command names at the aesh prompt.
 *
 * @author Aesh team
 */
public class TuiDemoExample {

    /**
     * Minimal TuiAppCommand that shows a styled panel with a welcome message.
     * Press 'q' to quit (handled by TuiAppCommand default key handling).
     */
    @CommandDefinition(name = "tui-hello", description = "Show a hello panel")
    public static class HelloCommand extends TuiAppCommand {

        @Override
        protected Element render() {
            return panel("Hello TamboUI!",
                    text("Welcome to aesh + TamboUI integration.\n\nPress 'q' to quit.")
            ).rounded().borderColor(Color.CYAN).fill();
        }
    }

    /**
     * TuiCommand with an animated progress bar that advances on each tick event.
     * Uses TuiRunner's event loop directly for fine-grained control.
     */
    @CommandDefinition(name = "tui-gauge", description = "Animated progress bar")
    public static class GaugeCommand extends TuiCommand {

        @Option(name = "speed", shortName = 's', defaultValue = {"100"},
                description = "Tick rate in milliseconds")
        private int speedMs;

        @Override
        protected TuiConfig.Builder configure(TuiConfig.Builder builder) {
            return builder.tickRate(Duration.ofMillis(speedMs));
        }

        @Override
        protected void runTui(TuiRunner runner, CommandInvocation invocation) throws Exception {
            AtomicInteger progress = new AtomicInteger(0);

            runner.run(
                    (event, r) -> {
                        if (event instanceof KeyEvent) {
                            KeyEvent key = (KeyEvent) event;
                            if (key.isQuit()) {
                                r.quit();
                                return false;
                            }
                        }
                        if (event instanceof TickEvent) {
                            progress.getAndUpdate(v -> (v + 1) % 101);
                            return true;
                        }
                        return false;
                    },
                    frame -> {
                        Gauge gauge = Gauge.builder()
                                .percent(progress.get())
                                .label("Loading... " + progress.get() + "%")
                                .gaugeColor(Color.GREEN)
                                .block(Block.bordered())
                                .build();

                        frame.renderWidget(gauge, frame.area());
                    }
            );
        }
    }

    /**
     * TuiAppCommand showing sample data in a table with keyboard navigation.
     * Uses the declarative Element DSL via ToolkitRunner.
     */
    @CommandDefinition(name = "tui-table", description = "Show a data table")
    public static class TableCommand extends TuiAppCommand {

        private final TableState tableState = new TableState();

        @Override
        protected Element render() {
            return column(
                    panel("Employee Directory",
                            table()
                                    .header("ID", "Name", "Role", "City")
                                    .widths(
                                            Constraint.length(4),
                                            Constraint.percentage(25),
                                            Constraint.percentage(25),
                                            Constraint.fill(1)
                                    )
                                    .row("1", "Alice", "Engineer", "San Francisco")
                                    .row("2", "Bob", "Designer", "New York")
                                    .row("3", "Carol", "Manager", "London")
                                    .row("4", "Dave", "Analyst", "Berlin")
                                    .row("5", "Eve", "Developer", "Tokyo")
                                    .highlightStyle(Style.EMPTY.bg(Color.DARK_GRAY))
                                    .highlightSymbol(">> ")
                                    .state(tableState)
                                    .fill()
                    ).rounded().borderColor(Color.BLUE).fill(),
                    text("Navigate: Up/Down | Quit: q").dim()
            ).fill();
        }

        @Override
        protected boolean onKeyEvent(KeyEvent event, ToolkitRunner runner) {
            if (event.isQuit()) {
                runner.quit();
                return true;
            }
            if (event.isUp()) {
                tableState.selectPrevious();
                return true;
            }
            if (event.isDown()) {
                tableState.selectNext(5);
                return true;
            }
            return false;
        }
    }

    /**
     * Live sparkline chart with randomly generated data points.
     * New data points are added on each tick, simulating a live feed.
     */
    @CommandDefinition(name = "tui-sparkline", description = "Live sparkline chart")
    public static class SparklineCommand extends TuiCommand {

        @Override
        protected TuiConfig.Builder configure(TuiConfig.Builder builder) {
            return builder.tickRate(Duration.ofMillis(200));
        }

        @Override
        protected void runTui(TuiRunner runner, CommandInvocation invocation) throws Exception {
            Random rng = new Random();
            List<Long> data = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                data.add((long) rng.nextInt(100));
            }

            runner.run(
                    (event, r) -> {
                        if (event instanceof KeyEvent && ((KeyEvent) event).isQuit()) {
                            r.quit();
                            return false;
                        }
                        if (event instanceof TickEvent) {
                            data.add((long) rng.nextInt(100));
                            if (data.size() > 200) {
                                data.remove(0);
                            }
                            return true;
                        }
                        return false;
                    },
                    frame -> {
                        long[] arr = new long[data.size()];
                        for (int i = 0; i < data.size(); i++) {
                            arr[i] = data.get(i);
                        }
                        Sparkline sparkline = Sparkline.builder()
                                .data(arr)
                                .block(Block.builder().title("Live Data Feed").build())
                                .style(Style.EMPTY.fg(Color.YELLOW))
                                .build();

                        frame.renderWidget(sparkline, frame.area());
                    }
            );
        }
    }

    /**
     * Bar chart showing simulated server metrics.
     */
    @CommandDefinition(name = "tui-barchart", description = "Server metrics bar chart")
    public static class BarChartCommand extends TuiAppCommand {

        @Override
        protected Element render() {
            return column(
                    barChart()
                            .groups(
                                    BarGroup.of("web-1",
                                            bar(72, "CPU", Color.RED),
                                            bar(45, "Mem", Color.GREEN),
                                            bar(28, "IO", Color.BLUE)
                                    ),
                                    BarGroup.of("web-2",
                                            bar(55, "CPU", Color.RED),
                                            bar(68, "Mem", Color.GREEN),
                                            bar(15, "IO", Color.BLUE)
                                    ),
                                    BarGroup.of("db-1",
                                            bar(90, "CPU", Color.RED),
                                            bar(82, "Mem", Color.GREEN),
                                            bar(63, "IO", Color.BLUE)
                                    ),
                                    BarGroup.of("cache",
                                            bar(20, "CPU", Color.RED),
                                            bar(95, "Mem", Color.GREEN),
                                            bar(5, "IO", Color.BLUE)
                                    )
                            )
                            .barWidth(5)
                            .barGap(1)
                            .groupGap(3)
                            .max(100)
                            .title("Server Metrics (%)")
                            .rounded()
                            .borderColor(Color.MAGENTA)
                            .fill(),
                    text("Press 'q' to quit").dim()
            ).fill();
        }

        private static Bar bar(long value, String label, Color color) {
            return Bar.builder().value(value).label(label).style(Style.EMPTY.fg(color)).build();
        }
    }

    /**
     * Tabbed interface with different content per tab.
     * Use left/right arrow keys to switch tabs.
     */
    @CommandDefinition(name = "tui-tabs", description = "Tabbed interface")
    public static class TabsCommand extends TuiAppCommand {

        private final TabsState tabsState = new TabsState(0);

        @Override
        protected Element render() {
            Element content;
            int selected = tabsState.selected() != null ? tabsState.selected() : 0;
            switch (selected) {
                case 0:
                    content = column(
                            text("System Overview").bold(),
                            spacer(1),
                            row(
                                    gauge(72).label("CPU: 72%").gaugeColor(Color.RED).title("CPU").rounded().fill(),
                                    gauge(45).label("Mem: 45%").gaugeColor(Color.GREEN).title("Memory").rounded().fill()
                            ).fill(),
                            row(
                                    gauge(28).label("Disk: 28%").gaugeColor(Color.BLUE).title("Disk").rounded().fill(),
                                    gauge(12).label("Net: 12%").gaugeColor(Color.YELLOW).title("Network").rounded().fill()
                            ).fill()
                    ).fill();
                    break;
                case 1:
                    content = column(
                            text("Process List").bold(),
                            spacer(1),
                            list("  java        - 12.3% CPU", "  postgres     -  8.1% CPU",
                                    "  nginx        -  2.4% CPU", "  redis        -  1.8% CPU",
                                    "  node         -  1.2% CPU", "  cron         -  0.1% CPU")
                                    .highlightColor(Color.CYAN)
                                    .title("Top Processes")
                                    .rounded()
                                    .fill()
                    ).fill();
                    break;
                case 2:
                    content = column(
                            text("Event Log").bold(),
                            spacer(1),
                            list("[INFO]  Service started on port 8080",
                                    "[INFO]  Connected to database",
                                    "[WARN]  High memory usage detected",
                                    "[INFO]  Cache warmed up (1234 entries)",
                                    "[ERROR] Connection timeout to upstream",
                                    "[INFO]  Retry succeeded",
                                    "[INFO]  Health check passed")
                                    .title("Recent Events")
                                    .rounded()
                                    .borderColor(Color.YELLOW)
                                    .displayOnly()
                                    .fill()
                    ).fill();
                    break;
                default:
                    content = text("Unknown tab");
                    break;
            }

            return column(
                    tabs("Overview", "Processes", "Logs")
                            .state(tabsState)
                            .highlightColor(Color.CYAN)
                            .divider(" | ")
                            .rounded()
                            .borderColor(Color.WHITE),
                    panel(content).fill()
            ).fill();
        }

        @Override
        protected boolean onKeyEvent(KeyEvent event, ToolkitRunner runner) {
            if (event.isQuit()) {
                runner.quit();
                return true;
            }
            if (event.isLeft()) {
                int current = tabsState.selected() != null ? tabsState.selected() : 0;
                tabsState.select(Math.max(0, current - 1));
                return true;
            }
            if (event.isRight()) {
                int current = tabsState.selected() != null ? tabsState.selected() : 0;
                tabsState.select(Math.min(2, current + 1));
                return true;
            }
            return false;
        }
    }

    /**
     * Calendar widget showing the current month with today highlighted.
     * Use left/right arrows to navigate months.
     */
    @CommandDefinition(name = "tui-calendar", description = "Calendar view")
    public static class CalendarCommand extends TuiAppCommand {

        private final AtomicReference<LocalDate> currentDate =
                new AtomicReference<>(LocalDate.now());

        @Override
        protected Element render() {
            LocalDate date = currentDate.get();
            return column(
                    calendar(date)
                            .showMonthHeader(Style.EMPTY.bold().fg(Color.CYAN))
                            .showWeekdaysHeader(Style.EMPTY.fg(Color.YELLOW))
                            .highlightToday(Color.GREEN)
                            .showSurrounding(Style.EMPTY.dim())
                            .rounded()
                            .borderColor(Color.WHITE)
                            .fill(),
                    text("<< Left/Right to change month | 'q' to quit >>").dim()
            ).fill();
        }

        @Override
        protected boolean onKeyEvent(KeyEvent event, ToolkitRunner runner) {
            if (event.isQuit()) {
                runner.quit();
                return true;
            }
            if (event.isLeft()) {
                currentDate.updateAndGet(d -> d.minusMonths(1));
                return true;
            }
            if (event.isRight()) {
                currentDate.updateAndGet(d -> d.plusMonths(1));
                return true;
            }
            return false;
        }
    }

    /**
     * Multi-panel dashboard combining gauges, sparkline, and bar chart.
     * All data updates live on each tick.
     */
    @CommandDefinition(name = "tui-dashboard", description = "Live system dashboard")
    public static class DashboardCommand extends TuiCommand {

        @Override
        protected TuiConfig.Builder configure(TuiConfig.Builder builder) {
            return builder.tickRate(Duration.ofMillis(500));
        }

        @Override
        protected void runTui(TuiRunner runner, CommandInvocation invocation) throws Exception {
            Random rng = new Random();
            AtomicInteger cpu = new AtomicInteger(50);
            AtomicInteger mem = new AtomicInteger(60);
            AtomicInteger disk = new AtomicInteger(35);
            List<Long> cpuHistory = new ArrayList<>();
            for (int i = 0; i < 60; i++) {
                cpuHistory.add(50L);
            }

            runner.run(
                    (event, r) -> {
                        if (event instanceof KeyEvent && ((KeyEvent) event).isQuit()) {
                            r.quit();
                            return false;
                        }
                        if (event instanceof TickEvent) {
                            cpu.set(clamp(cpu.get() + rng.nextInt(11) - 5, 0, 100));
                            mem.set(clamp(mem.get() + rng.nextInt(7) - 3, 0, 100));
                            disk.set(clamp(disk.get() + rng.nextInt(3) - 1, 0, 100));
                            cpuHistory.add((long) cpu.get());
                            if (cpuHistory.size() > 120) {
                                cpuHistory.remove(0);
                            }
                            return true;
                        }
                        return false;
                    },
                    frame -> {
                        dev.tamboui.layout.Rect area = frame.area();
                        List<dev.tamboui.layout.Rect> rows = dev.tamboui.layout.Layout.vertical()
                                .constraints(
                                        Constraint.length(3),
                                        Constraint.fill(1),
                                        Constraint.length(1)
                                )
                                .split(area);

                        // Top row: three gauges side by side
                        List<dev.tamboui.layout.Rect> gaugeCols = dev.tamboui.layout.Layout.horizontal()
                                .constraints(
                                        Constraint.percentage(33),
                                        Constraint.percentage(34),
                                        Constraint.percentage(33)
                                )
                                .split(rows.get(0));

                        Gauge cpuGauge = Gauge.builder()
                                .percent(cpu.get())
                                .label("CPU " + cpu.get() + "%")
                                .gaugeColor(cpu.get() > 80 ? Color.RED : cpu.get() > 50 ? Color.YELLOW : Color.GREEN)
                                .build();
                        Gauge memGauge = Gauge.builder()
                                .percent(mem.get())
                                .label("MEM " + mem.get() + "%")
                                .gaugeColor(mem.get() > 80 ? Color.RED : mem.get() > 50 ? Color.YELLOW : Color.GREEN)
                                .build();
                        Gauge diskGauge = Gauge.builder()
                                .percent(disk.get())
                                .label("DISK " + disk.get() + "%")
                                .gaugeColor(disk.get() > 80 ? Color.RED : disk.get() > 50 ? Color.YELLOW : Color.GREEN)
                                .build();

                        frame.renderWidget(cpuGauge, gaugeCols.get(0));
                        frame.renderWidget(memGauge, gaugeCols.get(1));
                        frame.renderWidget(diskGauge, gaugeCols.get(2));

                        // Middle: CPU history sparkline
                        long[] histArr = new long[cpuHistory.size()];
                        for (int i = 0; i < cpuHistory.size(); i++) {
                            histArr[i] = cpuHistory.get(i);
                        }
                        Sparkline spark = Sparkline.builder()
                                .data(histArr)
                                .max(100)
                                .block(Block.builder().title("CPU History").build())
                                .style(Style.EMPTY.fg(Color.CYAN))
                                .build();
                        frame.renderWidget(spark, rows.get(1));

                        // Bottom: status line
                        dev.tamboui.widgets.paragraph.Paragraph status =
                                dev.tamboui.widgets.paragraph.Paragraph.builder()
                                        .text(dev.tamboui.text.Text.from("Press 'q' to quit"))
                                        .style(Style.EMPTY.dim())
                                        .build();
                        frame.renderWidget(status, rows.get(2));
                    }
            );
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        AeshConsoleRunner.builder()
                .commands(
                        HelloCommand.class,
                        GaugeCommand.class,
                        TableCommand.class,
                        SparklineCommand.class,
                        BarChartCommand.class,
                        TabsCommand.class,
                        CalendarCommand.class,
                        DashboardCommand.class
                )
                .addExitCommand()
                .prompt("[tui-demo]$ ")
                .start();
    }
}

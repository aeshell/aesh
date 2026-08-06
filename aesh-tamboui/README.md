# Aesh TamboUI Integration

TUI (Text User Interface) framework integration for aesh commands, powered by [TamboUI](https://github.com/tamboui/tamboui).

Build rich terminal UIs — dashboards, tables, gauges, charts — as regular aesh commands. When the user runs the command, the terminal switches to full-screen TUI mode. When they quit, control returns to the aesh prompt.

## Dependency

```xml
<dependency>
    <groupId>org.aesh</groupId>
    <artifactId>aesh-tamboui</artifactId>
    <version>3.17-dev</version>
</dependency>
```

## Two Approaches

| Base Class | Style | Best For |
|---|---|---|
| `TuiAppCommand` | Declarative Element tree (like React) | Static layouts, forms, tables |
| `TuiCommand` | Event-loop with direct rendering | Animations, live data, custom layouts |

Both automatically wire the aesh terminal connection to TamboUI's rendering engine.

## Quick Start: TuiAppCommand (Declarative)

Override `render()` to return an Element tree. TamboUI calls it each frame.

```java
import static dev.tamboui.toolkit.Toolkit.*;

@CommandDefinition(name = "hello", description = "Hello TUI")
public class HelloCommand extends TuiAppCommand {

    @Override
    protected Element render() {
        return panel("Hello",
                text("Welcome to aesh + TamboUI!\n\nPress 'q' to quit."))
            .rounded()
            .borderColor(Color.CYAN)
            .fill();
    }
}
```

Press `q` or Ctrl+C to exit (default key handling). Override `onKeyEvent()` to customize:

```java
@Override
protected boolean onKeyEvent(KeyEvent event, ToolkitRunner runner) {
    if (event.isChar('r')) {
        // handle 'r' key
        return true; // consumed
    }
    // default: quit on 'q' or Ctrl+C
    return super.onKeyEvent(event, runner);
}
```

## Quick Start: TuiCommand (Event Loop)

Override `runTui()` for full control over the event loop and rendering.

```java
@CommandDefinition(name = "gauge", description = "Animated gauge")
public class GaugeCommand extends TuiCommand {

    @Option(name = "speed", defaultValue = "100",
            description = "Tick rate in ms")
    int speedMs;

    @Override
    protected TuiConfig.Builder configure(TuiConfig.Builder builder) {
        return builder.tickRate(Duration.ofMillis(speedMs));
    }

    @Override
    protected void runTui(TuiRunner runner, CommandInvocation invocation)
            throws Exception {
        AtomicInteger progress = new AtomicInteger(0);

        runner.run(
            (event, r) -> {
                if (event instanceof KeyEvent key && key.isQuit()) {
                    r.quit();
                    return false;
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
            });
    }
}
```

## TuiMixin: Standard TUI Options

Add `@Mixin TuiMixin` to your command for standard TUI CLI options. The base classes auto-detect the mixin and apply the options before your `configure()` override.

```java
@CommandDefinition(name = "dashboard", description = "Dashboard")
public class DashboardCommand extends TuiAppCommand {

    @Mixin
    TuiMixin tuiOptions;

    @Override
    protected Element render() {
        return text("Dashboard content");
    }
}
```

The mixin adds these options:

| Option | Default | Description |
|---|---|---|
| `--no-alt-screen` | false | Disable alternate screen buffer |
| `--show-cursor` | false | Show cursor in TUI mode |
| `--mouse` | false | Enable mouse capture |
| `--tick-rate` | 0 | Animation refresh rate in ms (0 = disabled) |
| `--poll-timeout` | 100 | Event poll timeout in ms |

Usage: `dashboard --tick-rate 200 --mouse`

## TuiSupport: Manual Integration

For commands that don't extend the base classes, use `TuiSupport` to create backends and runners from the Shell:

```java
@CommandDefinition(name = "custom", description = "Custom TUI")
public class CustomCommand implements Command<CommandInvocation> {

    @Override
    public CommandResult execute(CommandInvocation invocation)
            throws CommandException {
        try (TuiRunner runner = TuiSupport.createRunner(
                invocation.getShell())) {
            runner.run(
                (event, r) -> { /* handle events */ return false; },
                frame -> { /* render */ });
            return CommandResult.SUCCESS;
        } catch (Exception e) {
            throw new CommandException("TUI error", e);
        }
    }
}
```

Available factory methods:

| Method | Returns | Description |
|---|---|---|
| `createBackend(Shell)` | `AeshBackend` | Raw backend for custom configuration |
| `configBuilder(Shell)` | `TuiConfig.Builder` | Pre-configured builder (backend + no shutdown hook) |
| `createRunner(Shell)` | `TuiRunner` | Ready-to-use event-loop runner |
| `createRunner(Shell, TuiConfig.Builder)` | `TuiRunner` | Runner with custom config |
| `createToolkitRunner(Shell)` | `ToolkitRunner` | Ready-to-use declarative runner |

## Registering TUI Commands

Register TUI commands like any aesh command:

```java
AeshConsoleRunner.builder()
        .command(HelloCommand.class)
        .command(GaugeCommand.class)
        .command(DashboardCommand.class)
        .addExitCommand()
        .start();
```

## Running the Demos

The module includes 8 demo commands in `TuiDemoExample`:

| Command | Type | Description |
|---|---|---|
| `tui-hello` | TuiAppCommand | Styled panel with welcome message |
| `tui-gauge` | TuiCommand | Animated progress bar (`--speed` option) |
| `tui-table` | TuiAppCommand | Keyboard-navigable data table |
| `tui-sparkline` | TuiCommand | Live random-data sparkline |
| `tui-barchart` | TuiAppCommand | Static bar chart |
| `tui-tabs` | TuiAppCommand | Tabbed UI with gauges and event log |
| `tui-calendar` | TuiAppCommand | Month calendar with navigation |
| `tui-dashboard` | TuiCommand | Combined gauges, sparkline, and text |

Run with:

```bash
mvn exec:java -pl aesh-tamboui \
    -Dexec.mainClass=org.aesh.tamboui.examples.TuiDemoExample
```

Then type a command name (e.g., `tui-gauge --speed 50`) at the prompt.

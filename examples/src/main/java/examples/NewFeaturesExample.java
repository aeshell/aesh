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
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package examples;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.shell.Shell;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.tty.StatusLine;

/**
 * Demonstrates new features from aesh 3.15:
 *
 * <ul>
 * <li><b>Dynamic prompt supplier</b> — starship-style prompt showing working
 *     directory and last command duration, rebuilt before each readline cycle.</li>
 * <li><b>Command execution listener</b> — captures command duration and feeds
 *     it into the dynamic prompt.</li>
 * <li><b>Multi-line prompts</b> — info line above the input cursor.</li>
 * <li><b>Right prompt</b> — shows the current time, right-aligned.</li>
 * <li><b>printAbove</b> — async notifications printed above the prompt without
 *     disrupting input (see the {@code notify} command).</li>
 * <li><b>Status lines</b> — persistent status bar between output and prompt
 *     (see the {@code status} command).</li>
 * <li><b>--help=skill/markdown/asciidoc</b> — try {@code build --help=skill} or
 *     {@code deploy --help=markdown} to get formatted documentation.</li>
 * <li><b>Group commands</b> — {@code app build} and {@code app deploy} as
 *     subcommands.</li>
 * </ul>
 *
 * Run:
 * <pre>
 * mvn -pl examples exec:java -Dexec.mainClass=examples.NewFeaturesExample
 * </pre>
 *
 * Try these commands:
 * <pre>
 * help                        # list all commands
 * build --target release      # builds something, updates prompt with duration
 * deploy --environment prod   # deploys, shows duration in prompt
 * build --help=skill          # skill-format documentation
 * build --help=markdown       # markdown documentation
 * deploy --help=asciidoc      # asciidoc documentation
 * notify --message "hello"    # prints above the prompt asynchronously
 * notify --repeat 5           # sends 5 notifications, one per second
 * status --message "building" # sets a persistent status line
 * status --clear              # removes the status line
 * sleep --seconds 3           # sleeps to show duration in prompt
 * app build --target lib      # group command subcommand
 * app deploy --env staging    # group command subcommand
 * app build --help=skill      # formatted help for subcommand
 * exit                        # quit
 * </pre>
 */
public class NewFeaturesExample {

    // Shared state for the dynamic prompt
    private static final AtomicLong lastDurationMs = new AtomicLong(0);
    private static final AtomicReference<String> lastCommand = new AtomicReference<>("");

    public static void main(String[] args) {
        AeshConsoleRunner.builder()
                .command(BuildCommand.class)
                .command(DeployCommand.class)
                .command(NotifyCommand.class)
                .command(StatusCommand.class)
                .command(SleepCommand.class)
                .command(AppGroupCommand.class)
                // Dynamic prompt supplier — called before each readline cycle
                .promptSupplier(NewFeaturesExample::buildPrompt)
                // Command execution listener — captures duration for the prompt
                .commandExecutionListener((commandLine, result, durationMs) -> {
                    lastDurationMs.set(durationMs);
                    // Extract just the command name from the command line
                    String cmd = commandLine.trim();
                    int space = cmd.indexOf(' ');
                    if (space > 0) cmd = cmd.substring(0, space);
                    lastCommand.set(cmd);
                })
                .addExitCommand()
                .start();
    }

    /**
     * Build a starship-style multi-line prompt with right prompt.
     * <p>
     * Line 1: working directory + last command duration (if > 0)
     * Line 2: input prompt
     * Right prompt: current time
     */
    private static Prompt buildPrompt() {
        StringBuilder info = new StringBuilder();
        info.append(System.getProperty("user.dir", "~"));

        long ms = lastDurationMs.getAndSet(0);
        if (ms > 0) {
            String cmd = lastCommand.getAndSet("");
            info.append(" ");
            if (!cmd.isEmpty()) {
                info.append(cmd).append(" ");
            }
            info.append("took ");
            if (ms >= 60_000) {
                info.append(ms / 60_000).append("m ").append((ms % 60_000) / 1000).append("s");
            } else if (ms >= 1000) {
                info.append(String.format("%.1fs", ms / 1000.0));
            } else {
                info.append(ms).append("ms");
            }
        }

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        return Prompt.builder()
                .line(info.toString())
                .line("> ")
                .rightPrompt(time)
                .build();
    }

    // ========== Commands ==========

    @CommandDefinition(name = "build", generateHelp = true,
            description = "Build the project with the specified target")
    public static class BuildCommand implements Command<CommandInvocation> {

        @Option(shortName = 't', description = "Build target (e.g., debug, release, lib)",
                defaultValue = "debug")
        String target;

        @Option(description = "Clean before building")
        boolean clean;

        @Option(shortName = 'j', description = "Number of parallel jobs",
                defaultValue = "4")
        int jobs;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException, InterruptedException {
            if (clean) {
                ci.println("Cleaning build directory...");
                Thread.sleep(200);
            }
            ci.println("Building target '" + target + "' with " + jobs + " jobs...");
            // Simulate build work
            Thread.sleep(500 + (long) (Math.random() * 500));
            ci.println("Build complete.");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "deploy", generateHelp = true,
            description = "Deploy the application to an environment")
    public static class DeployCommand implements Command<CommandInvocation> {

        @Option(shortName = 'e', description = "Target environment",
                defaultValue = "dev",
                allowedValues = {"dev", "staging", "prod"})
        String environment;

        @Option(description = "Skip confirmation prompt")
        boolean force;

        @Argument(description = "Artifact to deploy (default: current build)")
        String artifact;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException, InterruptedException {
            String target = artifact != null ? artifact : "current build";
            if ("prod".equals(environment) && !force) {
                ci.println("Deploying to prod requires --force. Use: deploy -e prod --force");
                return CommandResult.FAILURE;
            }
            ci.println("Deploying " + target + " to " + environment + "...");
            Thread.sleep(300 + (long) (Math.random() * 700));
            ci.println("Deployed successfully to " + environment + ".");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "notify", generateHelp = true,
            description = "Print a notification above the prompt (demonstrates printAbove)")
    public static class NotifyCommand implements Command<CommandInvocation> {

        @Option(shortName = 'm', description = "Notification message",
                defaultValue = "This is a notification!")
        String message;

        @Option(shortName = 'r', description = "Repeat N times (one per second)",
                defaultValue = "1")
        int repeat;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException, InterruptedException {
            Shell shell = ci.getShell();
            if (repeat <= 1) {
                shell.printAbove("[notification] " + message);
            } else {
                // Schedule repeated notifications using printAbove from a background thread
                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "notify-scheduler");
                    t.setDaemon(true);
                    return t;
                });
                ci.println("Sending " + repeat + " notifications (one per second)...");
                for (int i = 1; i <= repeat; i++) {
                    final int n = i;
                    executor.schedule(
                            () -> shell.printAbove("[" + n + "/" + repeat + "] " + message),
                            i, TimeUnit.SECONDS);
                }
                // Wait for all notifications to be sent
                Thread.sleep((repeat + 1) * 1000L);
                executor.shutdown();
                ci.println("All notifications sent.");
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "status", generateHelp = true,
            description = "Set or clear a persistent status line (demonstrates registerStatusLine)")
    public static class StatusCommand implements Command<CommandInvocation> {

        // Shared status line instance across invocations
        private static volatile StatusLine currentStatus;

        @Option(shortName = 'm', description = "Status message to display")
        String message;

        @Option(description = "Clear the status line")
        boolean clear;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            Shell shell = ci.getShell();

            if (clear) {
                if (currentStatus != null) {
                    currentStatus.close();
                    currentStatus = null;
                    ci.println("Status line cleared.");
                } else {
                    ci.println("No status line is active.");
                }
                return CommandResult.SUCCESS;
            }

            if (message == null || message.isEmpty()) {
                ci.println("Usage: status --message \"your message\" or status --clear");
                return CommandResult.FAILURE;
            }

            // Create or update the status line
            if (currentStatus == null) {
                currentStatus = shell.registerStatusLine(100);
                if (currentStatus == null) {
                    ci.println("Status lines are not supported in this terminal.");
                    ci.println("(They require an interactive terminal with readline support.)");
                    return CommandResult.FAILURE;
                }
            }
            currentStatus.setMessage(message);
            ci.println("Status line set to: " + message);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "sleep", generateHelp = true,
            description = "Sleep for a number of seconds (useful for testing duration display)")
    public static class SleepCommand implements Command<CommandInvocation> {

        @Argument(description = "Seconds to sleep", defaultValue = "2")
        int seconds;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException, InterruptedException {
            ci.println("Sleeping for " + seconds + " seconds...");
            Thread.sleep(seconds * 1000L);
            ci.println("Done.");
            return CommandResult.SUCCESS;
        }
    }

    // ========== Group command ==========

    @CommandDefinition(name = "app", generateHelp = true,
            description = "Application management commands",
            groupCommands = {AppBuildCommand.class, AppDeployCommand.class})
    public static class AppGroupCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            ci.println("Use 'app build' or 'app deploy'. Try 'app --help' for details.");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "build", generateHelp = true,
            description = "Build the application")
    public static class AppBuildCommand implements Command<CommandInvocation> {

        @Option(shortName = 't', description = "Build target", defaultValue = "debug")
        String target;

        @Option(description = "Enable optimizations")
        boolean optimize;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException, InterruptedException {
            ci.println("[app] Building target '" + target + "'"
                    + (optimize ? " with optimizations" : "") + "...");
            Thread.sleep(400);
            ci.println("[app] Build complete.");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "deploy", generateHelp = true,
            description = "Deploy the application")
    public static class AppDeployCommand implements Command<CommandInvocation> {

        @Option(shortName = 'e', description = "Target environment",
                defaultValue = "dev",
                allowedValues = {"dev", "staging", "prod"})
        String env;

        @Option(description = "Deploy version tag")
        String version;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException, InterruptedException {
            String ver = version != null ? " v" + version : "";
            ci.println("[app] Deploying" + ver + " to " + env + "...");
            Thread.sleep(300);
            ci.println("[app] Deployed successfully.");
            return CommandResult.SUCCESS;
        }
    }
}

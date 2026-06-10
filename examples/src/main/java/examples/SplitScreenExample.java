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

import org.aesh.AeshConsoleRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.shell.Shell;
import org.aesh.terminal.tty.ScreenRegion;
import org.aesh.terminal.tty.SplitScreen;

/**
 * Demonstrates the split-screen API.
 * <p>
 * The terminal is divided into a scrolling log area (top) and a readline
 * prompt area (bottom). Commands can route output to either region.
 * <p>
 * Available commands:
 * <ul>
 * <li>{@code split [--ratio 0.7]} — enable split-screen</li>
 * <li>{@code log --message "text"} — write to the top (log) region</li>
 * <li>{@code say --message "text"} — write to the bottom (prompt) region</li>
 * <li>{@code unsplit} — close split-screen, restore full terminal</li>
 * <li>{@code exit} — quit</li>
 * </ul>
 * <p>
 * Run: {@code mvn -pl examples exec:java -Dexec.mainClass=examples.SplitScreenExample}
 */
public class SplitScreenExample {

    public static void main(String[] args) {
        AeshConsoleRunner.builder()
                .command(SplitCommand.class)
                .command(LogCommand.class)
                .command(SayCommand.class)
                .command(UnsplitCommand.class)
                .prompt("[split-demo]$ ")
                .addExitCommand()
                .start();
    }

    @CommandDefinition(name = "split", description = "Enable split-screen mode")
    public static class SplitCommand implements Command<CommandInvocation> {

        @Option(shortName = 'r', description = "Ratio for top region (0.0-1.0)",
                defaultValue = "0.7")
        private double ratio;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            Shell shell = ci.getShell();
            SplitScreen split = shell.enableSplitScreen(ratio);
            if (split == null) {
                ci.println("Split-screen is not supported in this terminal.");
                return CommandResult.FAILURE;
            }
            split.setSeparator("\u2500"); // horizontal line
            split.topRegion().writeln("=== Log output area ===");
            split.topRegion().writeln("Use 'log --message ...' to write here.");
            ci.println("Split-screen enabled (" + (int) (ratio * 100) + "% top).");
            ci.println("Type 'unsplit' to restore full screen.");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "log", description = "Write a message to the top (log) region")
    public static class LogCommand implements Command<CommandInvocation> {

        @Option(shortName = 'm', required = true, description = "Message to log")
        private String message;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            Shell shell = ci.getShell();
            SplitScreen split = shell.splitScreen();
            if (split == null) {
                ci.println("Split-screen is not active. Run 'split' first.");
                return CommandResult.FAILURE;
            }
            // Write directly to the top region
            ScreenRegion top = split.topRegion();
            top.writeln(message);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "say", description = "Write a message to the bottom (prompt) region")
    public static class SayCommand implements Command<CommandInvocation> {

        @Option(shortName = 'm', required = true, description = "Message to print")
        private String message;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            // ci.println() writes to the current region (bottom by default)
            ci.println(message);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "unsplit", description = "Close split-screen and restore full terminal")
    public static class UnsplitCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation ci) {
            Shell shell = ci.getShell();
            SplitScreen split = shell.splitScreen();
            if (split == null) {
                ci.println("Split-screen is not active.");
                return CommandResult.FAILURE;
            }
            split.close();
            ci.println("Split-screen closed.");
            return CommandResult.SUCCESS;
        }
    }
}

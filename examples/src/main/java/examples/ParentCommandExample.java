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

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.option.ParentCommand;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.settings.SubCommandModeSettings;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.console.ReadlineConsole;
import org.aesh.readline.Prompt;
import org.aesh.readline.terminal.formatting.Color;
import org.aesh.readline.terminal.formatting.TerminalColor;
import org.aesh.readline.terminal.formatting.TerminalString;

import java.io.IOException;

/**
 * Example demonstrating parent context access in subcommands.
 *
 * This example shows how subcommands can access values from their parent group command
 * using three different approaches:
 * 1. Using @ParentCommand annotation for direct field injection
 * 2. Using CommandInvocation.getParentValue() for programmatic access
 * 3. Using inherited=true on parent options for automatic field population
 *
 * === SUB-COMMAND MODE (recommended) ===
 * Enter sub-command mode first, then run subcommands:
 *   project --name=myapp --verbose    (enters sub-command mode, prompt changes to "project[myapp]> ")
 *   build                              (inherits --verbose via inherited=true)
 *   test --coverage                    (inherits parent options)
 *   status                             (uses inherited --verbose option)
 *   context                            (displays current context values)
 *   exit                               (returns to main prompt, or use "..")
 *
 * === DIRECT INVOCATION (alternative) ===
 * When calling subcommands directly, use the --name option on the subcommand:
 *   project build --name=myapp
 *   project test --name=myapp --coverage
 *   project deploy --name=myapp --env=production
 *
 * @author Ståle W. Pedersen
 */
public class ParentCommandExample {

    public static void main(String[] args) throws CommandLineParserException, IOException, CommandRegistryException {

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(ExitCommand.class)
                .command(ProjectCommand.class)
                .create();

        // Configure sub-command mode settings (optional - defaults work well for most cases)
        // You can customize exit commands, prompts, messages, etc.
        SubCommandModeSettings subCommandSettings = SubCommandModeSettings.builder()
                .exitCommand("exit")           // Primary exit command (default: "exit")
                .alternativeExitCommand("..")   // Alternative exit command (default: "..")
                .contextSeparator(":")          // Separator for nested contexts (default: ":")
                .showArgumentInPrompt(true)     // Show option value in prompt (default: true)
                .enterMessage("Entering {name} mode.")  // Message when entering (default)
                .exitHint("Type '{exit}' or '{alt}' to return.")  // Exit hint
                .exitOnCtrlC(true)              // Ctrl+C exits sub-command mode (default: true)
                .build();

        SettingsBuilder<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                OptionActivator, CommandActivator> builder =
                SettingsBuilder.builder()
                        .logging(true)
                        .commandRegistry(registry)
                        .subCommandModeSettings(subCommandSettings);

        ReadlineConsole console = new ReadlineConsole(builder.build());
        console.setPrompt(new Prompt(new TerminalString("[parent-example]$ ",
                new TerminalColor(Color.CYAN, Color.DEFAULT, Color.Intensity.BRIGHT))));

        console.start();
    }

    @CommandDefinition(name = "exit", description = "Exit the application", aliases = {"quit"})
    public static class ExitCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.stop();
            return CommandResult.SUCCESS;
        }
    }

    /**
     * Parent group command that defines project-level options.
     * Subcommands can access these options via @ParentCommand or CommandInvocation.
     *
     * When executed without a subcommand, enters sub-command mode where
     * subsequent commands have access to the project's options.
     */
    @GroupCommandDefinition(name = "project", description = "Project management commands",
            groupCommands = {BuildCommand.class, TestCommand.class, DeployCommand.class, StatusCommand.class})
    public static class ProjectCommand implements Command<CommandInvocation> {

        @Option(name = "name", shortName = 'n', required = true, description = "Project name")
        private String projectName;

        // inherited=true makes this option automatically available to subcommands
        // Subcommands with a "verbose" field will get the value auto-populated
        @Option(name = "verbose", shortName = 'v', hasValue = false, description = "Enable verbose output", inherited = true)
        private boolean verbose;

        @Option(name = "config", shortName = 'c', description = "Configuration file path")
        private String configFile;

        // Getters for subcommands to access
        public String getProjectName() {
            return projectName;
        }

        public boolean isVerbose() {
            return verbose;
        }

        public String getConfigFile() {
            return configFile;
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            // Display current settings
            invocation.println("Project: " + projectName);
            invocation.println("Verbose: " + verbose);
            if (configFile != null) {
                invocation.println("Config: " + configFile);
            }
            invocation.println("");

            // Enter sub-command mode - this pushes the current command onto the context
            // Subsequent commands (build, test, deploy) will have access to projectName, verbose, etc.
            if (invocation.enterSubCommandMode(this)) {
                invocation.println("Available subcommands: build, test, deploy");
            } else {
                invocation.println("Sub-command mode not available.");
                invocation.println("Use: project build --name=<project-name>");
            }

            return CommandResult.SUCCESS;
        }
    }

    /**
     * Build subcommand - demonstrates @ParentCommand annotation.
     * The parent ProjectCommand is automatically injected when in sub-command mode.
     *
     * Usage:
     *   1. Enter sub-command mode: project --name=myapp --verbose
     *   2. Then run: build --target=jar
     *
     * Or use direct invocation with local options:
     *   project build --name=myapp --target=jar
     */
    @CommandDefinition(name = "build", description = "Build the project")
    public static class BuildCommand implements Command<CommandInvocation> {

        // Parent command is injected when in sub-command mode
        @ParentCommand
        private ProjectCommand parent;

        // Local option that can also be used for direct invocation
        @Option(name = "name", shortName = 'n', description = "Project name (use when not in sub-command mode)")
        private String name;

        @Option(name = "target", shortName = 't', defaultValue = {"jar"}, description = "Build target (jar, war, native)")
        private String target;

        @Option(name = "skip-tests", hasValue = false, description = "Skip running tests during build")
        private boolean skipTests;

        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            // Get project name from parent (if in sub-command mode) or from local option
            String projectName = (parent != null) ? parent.getProjectName() : name;
            boolean verbose = (parent != null) && parent.isVerbose();

            if (projectName == null) {
                invocation.println("Error: Project name is required.");
                invocation.println("Usage: project build --name=<project-name>");
                invocation.println("   Or: Enter sub-command mode first with: project --name=<project-name>");
                return CommandResult.FAILURE;
            }

            invocation.println("=== Building Project ===");
            invocation.println("Project: " + projectName);
            invocation.println("Target: " + target);
            invocation.println("Skip tests: " + skipTests);

            if (verbose) {
                String configFile = parent.getConfigFile();
                invocation.println("\n[VERBOSE] Build configuration:");
                invocation.println("[VERBOSE]   Config file: " + configFile);
                invocation.println("[VERBOSE]   Starting build process...");
                invocation.println("[VERBOSE]   Compiling sources...");
                invocation.println("[VERBOSE]   Packaging " + target + "...");
            }

            invocation.println("\nBuild completed successfully!");
            return CommandResult.SUCCESS;
        }
    }

    /**
     * Test subcommand - demonstrates CommandInvocation.getParentValue() approach.
     * This is useful when you don't want a direct dependency on the parent class.
     *
     * Usage:
     *   1. Enter sub-command mode: project --name=myapp --verbose
     *   2. Then run: test --coverage
     *
     * Or use direct invocation with local options:
     *   project test --name=myapp --coverage
     */
    @CommandDefinition(name = "test", description = "Run project tests")
    public static class TestCommand implements Command<CommandInvocation> {

        // Local option for direct invocation
        @Option(name = "name", shortName = 'n', description = "Project name (use when not in sub-command mode)")
        private String name;

        @Option(name = "coverage", hasValue = false, description = "Generate coverage report")
        private boolean coverage;

        @Option(name = "filter", description = "Filter tests by pattern")
        private String filter;

        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            // Try to get parent values via CommandInvocation (works in sub-command mode)
            String projectName = invocation.getParentValue("projectName", String.class);
            Boolean verbose = invocation.getParentValue("verbose", Boolean.class, false);

            // Fall back to local option if not in sub-command mode
            if (projectName == null) {
                projectName = name;
            }

            if (projectName == null) {
                invocation.println("Error: Project name is required.");
                invocation.println("Usage: project test --name=<project-name>");
                invocation.println("   Or: Enter sub-command mode first with: project --name=<project-name>");
                return CommandResult.FAILURE;
            }

            invocation.println("=== Running Tests ===");
            invocation.println("Project: " + projectName);
            invocation.println("Coverage: " + coverage);
            if (filter != null) {
                invocation.println("Filter: " + filter);
            }

            if (verbose) {
                invocation.println("\n[VERBOSE] Test configuration:");
                invocation.println("[VERBOSE]   Running all test suites...");
                if (filter != null) {
                    invocation.println("[VERBOSE]   Applying filter: " + filter);
                }
                if (coverage) {
                    invocation.println("[VERBOSE]   Generating coverage report...");
                }
            }

            invocation.println("\nAll tests passed!");
            return CommandResult.SUCCESS;
        }
    }

    /**
     * Deploy subcommand - demonstrates both approaches combined.
     *
     * Usage:
     *   1. Enter sub-command mode: project --name=myapp --verbose
     *   2. Then run: deploy --env=production
     *
     * Or use direct invocation with local options:
     *   project deploy --name=myapp --env=production
     */
    @CommandDefinition(name = "deploy", description = "Deploy the project")
    public static class DeployCommand implements Command<CommandInvocation> {

        // Direct injection for type-safe access (works in sub-command mode)
        @ParentCommand
        private ProjectCommand parent;

        // Local option for direct invocation
        @Option(name = "name", shortName = 'n', description = "Project name (use when not in sub-command mode)")
        private String name;

        @Option(name = "env", shortName = 'e', required = true, description = "Target environment (dev, staging, production)")
        private String environment;

        @Option(name = "dry-run", hasValue = false, description = "Simulate deployment without making changes")
        private boolean dryRun;

        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            // Get project name from parent (if in sub-command mode) or from local option
            String projectName = (parent != null) ? parent.getProjectName() : name;
            boolean verbose = (parent != null) && parent.isVerbose();

            if (projectName == null) {
                invocation.println("Error: Project name is required.");
                invocation.println("Usage: project deploy --name=<project-name> --env=<environment>");
                invocation.println("   Or: Enter sub-command mode first with: project --name=<project-name>");
                return CommandResult.FAILURE;
            }

            invocation.println("=== Deploying Project ===");
            invocation.println("Project: " + projectName);
            invocation.println("Environment: " + environment);
            invocation.println("Dry run: " + dryRun);

            // Can also use CommandInvocation for specific values (works in sub-command mode)
            String configFile = invocation.getParentValue("configFile", String.class);
            if (configFile != null) {
                invocation.println("Using config: " + configFile);
            }

            if (verbose) {
                invocation.println("\n[VERBOSE] Deployment steps:");
                invocation.println("[VERBOSE]   1. Building artifact...");
                invocation.println("[VERBOSE]   2. Uploading to " + environment + " server...");
                invocation.println("[VERBOSE]   3. Restarting services...");
                if (dryRun) {
                    invocation.println("[VERBOSE]   (Dry run - no actual changes made)");
                }
            }

            if (dryRun) {
                invocation.println("\nDry run completed - no changes made.");
            } else {
                invocation.println("\nDeployment to " + environment + " completed successfully!");
            }

            return CommandResult.SUCCESS;
        }
    }

    /**
     * Status subcommand - demonstrates inherited option auto-population.
     *
     * The "verbose" field will be automatically populated from the parent's
     * inherited verbose option when in sub-command mode.
     *
     * This approach is cleaner than @ParentCommand or getParentValue() when you
     * just need specific option values and want them auto-populated.
     *
     * Usage:
     *   1. Enter sub-command mode: project --name=myapp --verbose
     *   2. Then run: status
     *      (verbose field is auto-populated from parent)
     */
    @CommandDefinition(name = "status", description = "Show project status")
    public static class StatusCommand implements Command<CommandInvocation> {

        // This field has the same name as the parent's inherited option.
        // It will be automatically populated when in sub-command mode.
        @Option(name = "verbose", shortName = 'v', hasValue = false, description = "Show detailed status")
        private boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            invocation.println("=== Project Status ===");

            // Get the project name from parent context
            String projectName = invocation.getParentValue("projectName", String.class);
            if (projectName != null) {
                invocation.println("Project: " + projectName);
            }

            invocation.println("Status: Active");
            invocation.println("Health: Good");

            // The verbose field was auto-populated from parent's inherited option
            if (verbose) {
                invocation.println("\n[VERBOSE] Detailed status:");
                invocation.println("[VERBOSE]   Last build: 2 hours ago");
                invocation.println("[VERBOSE]   Test coverage: 87%");
                invocation.println("[VERBOSE]   Dependencies: 42 (3 outdated)");
                invocation.println("[VERBOSE]   Code quality: A");

                // Also demonstrate getInheritedValue for explicit inherited value access
                Boolean inheritedVerbose = invocation.getInheritedValue("verbose", Boolean.class);
                invocation.println("[VERBOSE]   (verbose inherited from parent: " + inheritedVerbose + ")");
            }

            return CommandResult.SUCCESS;
        }
    }
}

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

import java.util.List;
import java.util.Map;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;

/**
 * Interactive sub-command mode example for testing completions, help, and options.
 *
 * Try these scenarios:
 *
 * 1. Enter sub-command mode:
 *    app<enter>
 *
 * 2. Tab completion of child commands:
 *    <tab>          (lists: build, deploy, config, status)
 *    bu<tab>        (completes to: build)
 *
 * 3. Tab completion of child options:
 *    build --<tab>  (lists: --target, --clean, --help)
 *    build --ta<tab> (completes to: --target=)
 *
 * 4. Help on child commands:
 *    build --help
 *    deploy --help
 *
 * 5. allowedValues completion:
 *    deploy --environment <tab>  (lists: dev, staging, prod)
 *    deploy --environment p<tab> (completes to: prod)
 *
 * 6. OptionList with allowedValues:
 *    deploy --tags <tab>  (lists: v1, v2, latest)
 *
 * 7. OptionGroup (key=value properties):
 *    config -Dkey=value -Dflag
 *
 * 8. Inherited options:
 *    build --verbose       (inherited from parent)
 *
 * 9. Exit sub-command mode:
 *    exit    (or "..")
 *
 * 10. Direct invocation (without sub-command mode):
 *     app build --target jar --clean
 *     app deploy --environment prod
 */
public class SubCommandModeExample {

    public static void main(String[] args) {
        AeshConsoleRunner.builder()
                .command(AppCommand.class)
                .prompt("[demo]$ ")
                .addExitCommand()
                .start();
    }

    @CommandDefinition(name = "app", description = "Application manager",
            generateHelp = true,
            groupCommands = {
                    BuildCommand.class,
                    DeployCommand.class,
                    ConfigCommand.class,
                    StatusCommand.class
            })
    public static class AppCommand implements Command<CommandInvocation> {

        @Option(name = "verbose", shortName = 'v', hasValue = false,
                inherited = true, description = "Enable verbose output")
        private boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {
            invocation.enterSubCommandMode(this);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "build", description = "Build the project", generateHelp = true)
    public static class BuildCommand implements Command<CommandInvocation> {

        @Option(name = "target", shortName = 't',
                allowedValues = { "jar", "war", "native" },
                defaultValue = "jar",
                description = "Build target type")
        private String target;

        @Option(name = "clean", shortName = 'c', hasValue = false,
                description = "Clean before building")
        private boolean clean;

        @Option(name = "parallel", shortName = 'p',
                description = "Number of parallel threads")
        private int parallel;

        boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            if (verbose) {
                invocation.println("[VERBOSE] Starting build...");
            }
            invocation.println("Building " + target + (clean ? " (clean)" : "")
                    + (parallel > 0 ? " with " + parallel + " threads" : ""));
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "deploy", description = "Deploy the application", generateHelp = true)
    public static class DeployCommand implements Command<CommandInvocation> {

        @Option(name = "environment", shortName = 'e',
                allowedValues = { "dev", "staging", "prod" },
                description = "Target environment")
        private String environment;

        @Option(name = "force", shortName = 'f', hasValue = false,
                description = "Force deployment without confirmation")
        private boolean force;

        @OptionList(name = "tags",
                allowedValues = { "v1", "v2", "latest" },
                description = "Deployment tags")
        private List<String> tags;

        @Argument(description = "Application version to deploy")
        private String version;

        boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            if (environment == null) {
                invocation.println("Error: --environment is required");
                return CommandResult.FAILURE;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Deploying");
            if (version != null)
                sb.append(" v").append(version);
            sb.append(" to ").append(environment);
            if (force)
                sb.append(" (forced)");
            if (tags != null && !tags.isEmpty())
                sb.append(" tags=").append(tags);
            invocation.println(sb.toString());
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "config", description = "Manage configuration", generateHelp = true)
    public static class ConfigCommand implements Command<CommandInvocation> {

        @OptionGroup(shortName = 'D', description = "Set a config property",
                defaultValue = "true")
        private Map<String, String> properties;

        @Option(name = "list", shortName = 'l', hasValue = false,
                description = "List all config properties")
        private boolean list;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            if (list) {
                invocation.println("Current configuration:");
                invocation.println("  (no properties stored in this demo)");
            }
            if (properties != null && !properties.isEmpty()) {
                invocation.println("Setting properties:");
                properties.forEach((k, v) ->
                        invocation.println("  " + k + " = " + v));
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "status", description = "Show application status", generateHelp = true)
    public static class StatusCommand implements Command<CommandInvocation> {

        @Option(name = "format", shortName = 'f',
                allowedValues = { "text", "json", "yaml" },
                defaultValue = "text",
                description = "Output format")
        private String format;

        boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            if ("json".equals(format)) {
                invocation.println("{\"status\": \"running\", \"uptime\": \"2h 15m\"}");
            } else if ("yaml".equals(format)) {
                invocation.println("status: running");
                invocation.println("uptime: 2h 15m");
            } else {
                invocation.println("Status: running");
                invocation.println("Uptime: 2h 15m");
            }
            return CommandResult.SUCCESS;
        }
    }
}

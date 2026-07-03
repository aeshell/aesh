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
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;

/**
 * Demonstrates options with {@code acceptNameWithoutDashes = true}.
 * <p>
 * Options with this flag can be typed without the {@code --} prefix,
 * like bare words. This is useful for tools that want a more natural
 * command syntax — similar to {@code curl}, {@code chmod}, or
 * domain-specific CLIs where dashes feel verbose.
 * <p>
 * Available commands:
 * <ul>
 * <li>{@code deploy} — deploy to an environment using bare option names</li>
 * <li>{@code query} — search with bare options for natural language feel</li>
 * <li>{@code exit} — quit</li>
 * </ul>
 * <p>
 * Try these:
 * <pre>
 * deploy production                  # bare option "production" sets env
 * deploy staging force               # multiple bare options
 * deploy --env production --force    # traditional dashes also work
 *
 * query verbose limit=10 users       # bare options with values
 * query --verbose --limit 10 users   # same with dashes
 * </pre>
 * <p>
 * Run: {@code mvn -Pexamples exec:java -pl examples -Dexec.mainClass=examples.BareOptionExample}
 */
public class BareOptionExample {

    public static void main(String[] args) {
        AeshConsoleRunner.builder()
                .command(DeployCommand.class)
                .command(QueryCommand.class)
                .prompt("[bare-opts]$ ")
                .addExitCommand()
                .start();
    }

    @CommandDefinition(name = "deploy", generateHelp = true,
            description = "Deploy application to an environment")
    public static class DeployCommand implements Command<CommandInvocation> {

        @Option(name = "production", acceptNameWithoutDashes = true,
                hasValue = false, description = "Deploy to production")
        boolean production;

        @Option(name = "staging", acceptNameWithoutDashes = true,
                hasValue = false, description = "Deploy to staging")
        boolean staging;

        @Option(name = "force", acceptNameWithoutDashes = true,
                hasValue = false, description = "Force deploy without confirmation")
        boolean force;

        @Option(name = "replicas", acceptNameWithoutDashes = true,
                description = "Number of replicas", defaultValue = "1")
        int replicas;

        @Option(name = "env", description = "Target environment (traditional option)")
        String env;

        @Override
        public CommandResult execute(CommandInvocation ci)
                throws CommandException, InterruptedException {

            String target;
            if (production) {
                target = "production";
            } else if (staging) {
                target = "staging";
            } else if (env != null) {
                target = env;
            } else {
                target = "development";
            }

            ci.println("Deploying to " + target
                    + " with " + replicas + " replicas"
                    + (force ? " (forced)" : ""));

            if ("production".equals(target) && !force) {
                ci.println("Warning: use 'force' for production deploys");
            }

            Thread.sleep(500);
            ci.println("Deploy complete.");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "query", generateHelp = true,
            description = "Search and query data")
    public static class QueryCommand implements Command<CommandInvocation> {

        @Option(name = "verbose", acceptNameWithoutDashes = true,
                hasValue = false, description = "Show detailed output")
        boolean verbose;

        @Option(name = "count", acceptNameWithoutDashes = true,
                hasValue = false, description = "Show only the count")
        boolean count;

        @Option(name = "limit", acceptNameWithoutDashes = true,
                description = "Maximum results", defaultValue = "50")
        int limit;

        @Option(name = "format", acceptNameWithoutDashes = true,
                description = "Output format",
                allowedValues = { "table", "json", "csv" },
                defaultValue = "table")
        String format;

        @Argument(description = "Search term")
        String searchTerm;

        @Override
        public CommandResult execute(CommandInvocation ci)
                throws CommandException, InterruptedException {

            String term = searchTerm != null ? searchTerm : "*";

            if (verbose) {
                ci.println("Query: term=" + term
                        + " limit=" + limit
                        + " format=" + format
                        + " count=" + count);
            }

            ci.println("Searching for '" + term + "' (limit=" + limit
                    + ", format=" + format + ")...");

            Thread.sleep(300);

            if (count) {
                ci.println("Found: 42 results");
            } else {
                ci.println("Results:");
                int show = Math.min(limit, 5);
                for (int i = 1; i <= show; i++) {
                    ci.println("  " + i + ". " + term + "-result-" + i);
                }
                if (limit > 5) {
                    ci.println("  ... (" + (limit - 5) + " more)");
                }
            }

            return CommandResult.SUCCESS;
        }
    }
}

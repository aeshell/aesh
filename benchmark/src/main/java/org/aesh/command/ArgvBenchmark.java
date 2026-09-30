/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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
package org.aesh.command;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.operator.OperatorType;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Pre-tokenized execution cost: {@code executeCommand(commandName, args)}
 * rebuilds display strings and token wrappers per call (#662).
 * <p>
 * Workload mirrors the issue: an already-registered command executed
 * repeatedly with alternating options and one or two positionals, on both
 * the generated-metadata and the reflection paths. The command records an
 * observed checksum so invocations are never dead-code eliminated.
 * <p>
 * Run with {@code -prof gc} for allocation rates.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class ArgvBenchmark {

    @CommandDefinition(name = "net", description = "probe")
    public static class NetGenCommand implements Command<CommandInvocation> {
        @Option(name = "host", description = "host")
        String host;
        @Option(name = "count", description = "count")
        int count;
        @Option(name = "verbose", description = "verbose", hasValue = false)
        boolean verbose;
        @Arguments(description = "targets")
        java.util.List<String> targets;

        static volatile long checksum;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            long hash = 7;
            if (host != null)
                hash = hash * 31 + host.hashCode();
            hash = hash * 31 + count;
            if (targets != null) {
                for (String target : targets)
                    hash = hash * 31 + target.hashCode();
            }
            checksum += hash + (verbose ? 1 : 0);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "net", description = "probe")
    public static class NetReflectCommand implements Command<CommandInvocation> {
        @Option(name = "host", description = "host")
        String host;
        @Option(name = "count", description = "count")
        int count;
        @Option(name = "verbose", description = "verbose", hasValue = false)
        boolean verbose;
        @Arguments(description = "targets")
        java.util.List<String> targets;

        static volatile long checksum;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            long hash = 7;
            if (host != null)
                hash = hash * 31 + host.hashCode();
            hash = hash * 31 + count;
            if (targets != null) {
                for (String target : targets)
                    hash = hash * 31 + target.hashCode();
            }
            checksum += hash + (verbose ? 1 : 0);
            return CommandResult.SUCCESS;
        }
    }

    private static final String[][] ARG_SETS = {
            { "--host", "example.com", "--count", "3", "host1" },
            { "--host", "example.org", "--verbose", "host1", "host2" },
            { "--count", "7", "host2" },
            { "--host", "example.net", "--count", "1", "--verbose", "host3" },
    };

    private CommandRuntime<CommandInvocation> generated;
    private CommandRuntime<CommandInvocation> reflection;
    private int next;

    private static CommandRuntime<CommandInvocation> runtimeFor(CommandRegistry<CommandInvocation> registry)
            throws Exception {
        return AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registry)
                .operators(EnumSet.allOf(OperatorType.class))
                .build();
    }

    @Setup
    public void setup() throws Exception {
        CommandRegistry<CommandInvocation> generatedRegistry = AeshCommandRegistryBuilder
                .<CommandInvocation> builder()
                .command(NetGenCommand.class)
                .create();
        generated = runtimeFor(generatedRegistry);

        // Reflection path: build the container without consulting the
        // generated metadata registry (same trick as StartupBenchmark).
        Method reflectiveCreate = AeshCommandContainerBuilder.class
                .getDeclaredMethod("doGenerateCommandLineParser", Command.class);
        reflectiveCreate.setAccessible(true);
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        @SuppressWarnings("unchecked")
        CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) reflectiveCreate
                .invoke(builder, new NetReflectCommand());
        CommandRegistry<CommandInvocation> reflectionRegistry = AeshCommandRegistryBuilder
                .<CommandInvocation> builder()
                .create();
        ((org.aesh.command.registry.MutableCommandRegistry<CommandInvocation>) reflectionRegistry)
                .addCommand(container);
        reflection = runtimeFor(reflectionRegistry);
        next = 0;

        // Correctness gate: every arg set must succeed on both runtimes
        // before measuring (a silently skipped populate would benchmark
        // nothing at full speed), and both paths must agree exactly.
        for (String[] args : ARG_SETS) {
            NetGenCommand.checksum = 0;
            NetReflectCommand.checksum = 0;
            if (generated.executeCommand("net", args) != CommandResult.SUCCESS)
                throw new IllegalStateException("generated fixture failed");
            if (reflection.executeCommand("net", args) != CommandResult.SUCCESS)
                throw new IllegalStateException("reflection fixture failed");
            if (NetGenCommand.checksum == 0 || NetReflectCommand.checksum == 0)
                throw new IllegalStateException("fixtures produced no checksum");
            if (NetGenCommand.checksum != NetReflectCommand.checksum)
                throw new IllegalStateException("path divergence: generated="
                        + NetGenCommand.checksum + " reflection=" + NetReflectCommand.checksum);
        }
    }

    private String[] nextArgs() {
        String[] args = ARG_SETS[next & (ARG_SETS.length - 1)];
        next++;
        return args;
    }

    @Benchmark
    public void generated(Blackhole blackhole) throws Exception {
        blackhole.consume(generated.executeCommand("net", nextArgs()));
    }

    @Benchmark
    public void reflection(Blackhole blackhole) throws Exception {
        blackhole.consume(reflection.executeCommand("net", nextArgs()));
    }

    public static void main(String[] args) throws Exception {
        new Runner(new OptionsBuilder()
                .include(ArgvBenchmark.class.getSimpleName())
                .build()).run();
    }
}

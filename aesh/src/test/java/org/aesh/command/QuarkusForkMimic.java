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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.metadata.MetadataProviderRegistry;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.converter.CLConverterManager;

/**
 * Headless replay of the Quarkus augmentation-thread surface that stalls
 * forked {@code quarkus:dev} JVMs on Windows (quarkusio/quarkus#57716,
 * aeshell/aesh#686): load plus instantiate annotation-driven command
 * classes, look up converters and metadata providers, and build command
 * containers and registries — sequentially and from several threads racing
 * first-touch class initialization, the way parallel build-step threads do.
 * <p>
 * This is a standalone {@code main()}, not a surefire test, so each CI
 * attempt runs in a fresh JVM with faithful stdin/environment control and
 * class initialization replays every run. A watchdog dumps every stack and
 * exits nonzero on timeout, so a hanging run names the culprit in the log.
 * <p>
 * Fidelity ceiling, read before over-interpreting green: Quarkus's own
 * command classes ({@code QuitCommand}, {@code HelpCommand},
 * {@code ConsoleCommandBuildItem}s) live in Quarkus, so this replays with
 * stand-in commands. Green exonerates aesh-side loading, instantiation,
 * container building and registry/provider/converter paths — it cannot
 * exonerate Quarkus-side class initialization. The converter cache and the
 * metadata-provider lookup are already lock-free by inspection
 * ({@code ConcurrentHashMap} + {@code putIfAbsent}; {@code ClassValue} +
 * generation counter), and this harness proves it under load.
 */
public class QuarkusForkMimic {

    private static final int THREADS = 8;
    private static final int PARALLEL_ITERATIONS = 200;
    private static final int SEQUENTIAL_ITERATIONS = 500;
    private static final long WATCHDOG_MILLIS = 60000;

    public enum Mode {
        FAST,
        SAFE
    }

    @CommandDefinition(name = "deploy", description = "deploy it")
    public static class DeployCmd implements Command<CommandInvocation> {
        @Option(name = "target", description = "target")
        String target;
        @Option(name = "count", description = "count")
        int count;
        @Option(name = "verbose", description = "chatty", hasValue = false)
        boolean verbose;
        @Option(name = "mode", description = "mode")
        Mode mode;
        @OptionList(name = "include", description = "includes")
        List<String> includes;
        @Argument(description = "file", paramLabel = "file")
        String file;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "a", description = "child a")
    public static class ChildA implements Command<CommandInvocation> {
        @Option(name = "flag", description = "flag", hasValue = false)
        boolean flag;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "b", description = "child b")
    public static class ChildB implements Command<CommandInvocation> {
        @Option(name = "count", description = "count")
        int count;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "root", description = "group", groupCommands = { ChildA.class, ChildB.class })
    public static class GroupCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final List<Class<? extends Command>> COMMANDS = Arrays.<Class<? extends Command>> asList(
            DeployCmd.class, ChildA.class, ChildB.class, GroupCmd.class);

    public static void main(String[] args) throws Exception {
        Thread watchdog = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(WATCHDOG_MILLIS);
                } catch (InterruptedException e) {
                    return;
                }
                System.err.println("WATCHDOG: no completion within " + WATCHDOG_MILLIS
                        + "ms — dumping all stacks:");
                for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                    System.err.println("\"" + entry.getKey().getName() + "\" state=" + entry.getKey().getState());
                    for (StackTraceElement element : entry.getValue())
                        System.err.println("    at " + element);
                }
                System.err.flush();
                System.exit(2);
            }
        }, "mimic-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();

        System.out.println("QuarkusForkMimic: java=" + System.getProperty("java.version")
                + " os=" + System.getProperty("os.name")
                + " cpus=" + Runtime.getRuntime().availableProcessors()
                + " threads=" + THREADS
                + " parallelIterations=" + PARALLEL_ITERATIONS
                + " sequentialIterations=" + SEQUENTIAL_ITERATIONS);

        // Parallel first: first-touch class initialization happens under
        // racing threads here, mirroring parallel augmentation steps.
        runParallelPhase();
        runSequentialPhase();

        // Beyond the failing path (Quarkus never builds registries headless),
        // but cheap: one registry build exercises the same container paths.
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder
                .<CommandInvocation> builder()
                .commands(COMMANDS)
                .create();
        System.out.println("registry built: " + registry);

        watchdog.interrupt();
        System.out.println("MIMIC PASS");
    }

    private static void runParallelPhase() throws Exception {
        long start = System.nanoTime();
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        final CountDownLatch ready = new CountDownLatch(THREADS);
        final CountDownLatch done = new CountDownLatch(THREADS);
        for (int t = 0; t < THREADS; t++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ready.countDown();
                    try {
                        ready.await();
                        for (int i = 0; i < PARALLEL_ITERATIONS; i++)
                            replayOnce();
                    } catch (Throwable e) {
                        failure.compareAndSet(null, e);
                    } finally {
                        done.countDown();
                    }
                }
            }, "mimic-worker").start();
        }
        done.await();
        if (failure.get() != null)
            throw new AssertionError("parallel phase failed", failure.get());
        System.out.println("parallel phase done in " + (System.nanoTime() - start) / 1000000 + "ms");
    }

    private static void runSequentialPhase() throws Exception {
        long start = System.nanoTime();
        for (int i = 0; i < SEQUENTIAL_ITERATIONS; i++)
            replayOnce();
        System.out.println("sequential phase done in " + (System.nanoTime() - start) / 1000000 + "ms");
    }

    /**
     * One replay of the augmentation-thread surface for every stand-in
     * command: load plus instantiate (what Quarkus's ConsoleProcessor does),
     * converter and provider lookups, and a full container build.
     */
    private static void replayOnce() throws Exception {
        ClassLoader loader = QuarkusForkMimic.class.getClassLoader();
        for (Class<? extends Command> commandClass : COMMANDS) {
            Class<?> loaded = Class.forName(commandClass.getName(), true, loader);
            loaded.getDeclaredConstructor().newInstance();
        }
        CLConverterManager converters = CLConverterManager.getInstance();
        converters.getConverter(String.class);
        converters.getConverter(int.class);
        converters.getConverter(Mode.class);
        converters.getConverter(Object.class);
        for (Class<? extends Command> commandClass : COMMANDS) {
            MetadataProviderRegistry.getProvider(commandClass);
            new AeshCommandContainerBuilder<CommandInvocation>().create(commandClass);
        }
    }
}

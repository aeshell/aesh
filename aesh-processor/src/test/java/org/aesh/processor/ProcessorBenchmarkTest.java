/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
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
package org.aesh.processor;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.aesh.command.Command;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.CommandLineParserBuilder;
import org.aesh.command.metadata.CommandMetadataProvider;
import org.junit.Test;

/**
 * Benchmark comparing reflection-based vs generated metadata command creation.
 * <p>
 * Not a JMH benchmark (no forking, no blackhole sinks), but provides a
 * consistent directional comparison. Results are written to stdout and
 * captured in surefire reports.
 *
 * @author Aesh team
 */
public class ProcessorBenchmarkTest {

    private static final int WARMUP_ITERATIONS = 2000;
    private static final int MEASURED_ITERATIONS = 5000;

    // A command with 14 annotated fields: Options, OptionList, OptionGroup, Argument
    private static final String RICH_COMMAND_SOURCE =
            "package bench;\n" +
            "\n" +
            "import java.util.List;\n" +
            "import java.util.Map;\n" +
            "import org.aesh.command.Command;\n" +
            "import org.aesh.command.CommandDefinition;\n" +
            "import org.aesh.command.CommandResult;\n" +
            "import org.aesh.command.invocation.CommandInvocation;\n" +
            "import org.aesh.command.option.Argument;\n" +
            "import org.aesh.command.option.Option;\n" +
            "import org.aesh.command.option.OptionList;\n" +
            "import org.aesh.command.option.OptionGroup;\n" +
            "\n" +
            "@CommandDefinition(name = \"rich\", description = \"A command with many options\",\n" +
            "        aliases = {\"r\", \"rc\"})\n" +
            "public class RichCommand implements Command<CommandInvocation> {\n" +
            "    @Option(shortName = 'V', hasValue = false, description = \"Verbose\")\n" +
            "    private boolean verbose;\n" +
            "\n" +
            "    @Option(shortName = 'q', hasValue = false, description = \"Quiet\")\n" +
            "    private boolean quiet;\n" +
            "\n" +
            "    @Option(shortName = 'o', description = \"Output file\", required = true)\n" +
            "    private String output;\n" +
            "\n" +
            "    @Option(shortName = 'f', description = \"Format\")\n" +
            "    private String format;\n" +
            "\n" +
            "    @Option(description = \"Timeout in seconds\", defaultValue = \"30\")\n" +
            "    private int timeout;\n" +
            "\n" +
            "    @Option(description = \"Max retries\", defaultValue = \"3\")\n" +
            "    private int maxRetries;\n" +
            "\n" +
            "    @Option(description = \"Enable compression\", hasValue = false)\n" +
            "    private boolean compress;\n" +
            "\n" +
            "    @Option(description = \"Encoding\")\n" +
            "    private String encoding;\n" +
            "\n" +
            "    @Option(description = \"Log level\", defaultValue = \"INFO\")\n" +
            "    private String logLevel;\n" +
            "\n" +
            "    @Option(description = \"Config file\")\n" +
            "    private String config;\n" +
            "\n" +
            "    @OptionList(shortName = 'i', description = \"Include patterns\")\n" +
            "    private List<String> includes;\n" +
            "\n" +
            "    @OptionList(shortName = 'e', description = \"Exclude patterns\")\n" +
            "    private List<String> excludes;\n" +
            "\n" +
            "    @OptionGroup(shortName = 'D', description = \"System properties\")\n" +
            "    private Map<String, String> properties;\n" +
            "\n" +
            "    @Argument(description = \"Source directory\")\n" +
            "    private String source;\n" +
            "\n" +
            "    @Override\n" +
            "    public CommandResult execute(CommandInvocation ci) {\n" +
            "        return CommandResult.SUCCESS;\n" +
            "    }\n" +
            "}\n";

    // A minimal command with a single option
    private static final String SIMPLE_COMMAND_SOURCE =
            "package bench;\n" +
            "\n" +
            "import org.aesh.command.Command;\n" +
            "import org.aesh.command.CommandDefinition;\n" +
            "import org.aesh.command.CommandResult;\n" +
            "import org.aesh.command.invocation.CommandInvocation;\n" +
            "import org.aesh.command.option.Option;\n" +
            "\n" +
            "@CommandDefinition(name = \"simple\", description = \"Simple command\")\n" +
            "public class SimpleCommand implements Command<CommandInvocation> {\n" +
            "    @Option(shortName = 'n', description = \"Name\")\n" +
            "    private String name;\n" +
            "\n" +
            "    @Override\n" +
            "    public CommandResult execute(CommandInvocation ci) {\n" +
            "        return CommandResult.SUCCESS;\n" +
            "    }\n" +
            "}\n";

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void benchmarkReflectionVsGenerated() throws Exception {
        CompilationResult result = compileWithProcessor(
                new InMemorySource("bench.RichCommand", RICH_COMMAND_SOURCE),
                new InMemorySource("bench.SimpleCommand", SIMPLE_COMMAND_SOURCE));

        if (!result.success) {
            System.out.println("Compilation failed: " + result.diagnostics);
            return;
        }

        Class<?> richClass = result.classLoader.loadClass("bench.RichCommand");
        Class<?> simpleClass = result.classLoader.loadClass("bench.SimpleCommand");
        CommandMetadataProvider richProvider =
                (CommandMetadataProvider) result.classLoader.loadClass("bench.RichCommand_AeshMetadata").newInstance();
        CommandMetadataProvider simpleProvider =
                (CommandMetadataProvider) result.classLoader.loadClass("bench.SimpleCommand_AeshMetadata").newInstance();

        System.out.println("=== Benchmark: Rich command (14 annotated fields) ===");
        System.out.println("Warmup: " + WARMUP_ITERATIONS + ", Measured: " + MEASURED_ITERATIONS + " iterations");
        System.out.println();
        benchmarkPair("Rich command", richClass, richProvider);

        System.out.println();
        System.out.println("=== Benchmark: Simple command (1 annotated field) ===");
        benchmarkPair("Simple command", simpleClass, simpleProvider);

        System.out.println();
        System.out.println("=== First-call latency (single invocation, JVM already warm) ===");
        long singleReflNs = timeOnce(() -> createViaReflection(richClass));
        long singleGenNs = timeOnce(() -> createViaProvider(richProvider, richClass));
        System.out.printf("  Reflection:  %,d ns%n", singleReflNs);
        System.out.printf("  Generated:   %,d ns%n", singleGenNs);
    }

    @SuppressWarnings("rawtypes")
    private void benchmarkPair(String label, Class<?> commandClass, CommandMetadataProvider provider) throws Exception {
        // Warmup + measure reflection
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            createViaReflection(commandClass);
        }
        long reflectionNs = timeIterations(() -> createViaReflection(commandClass));

        // Warmup + measure generated
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            createViaProvider(provider, commandClass);
        }
        long generatedNs = timeIterations(() -> createViaProvider(provider, commandClass));

        double reflAvgUs = (reflectionNs / (double) MEASURED_ITERATIONS) / 1000.0;
        double genAvgUs = (generatedNs / (double) MEASURED_ITERATIONS) / 1000.0;
        double speedup = reflAvgUs / genAvgUs;

        System.out.printf("  Reflection:  %,.1f us/op  (total: %,d ms)%n", reflAvgUs, reflectionNs / 1_000_000);
        System.out.printf("  Generated:   %,.1f us/op  (total: %,d ms)%n", genAvgUs, generatedNs / 1_000_000);
        System.out.printf("  Speedup:     %.2fx%n", speedup);
    }

    private long timeIterations(ThrowingRunnable runnable) throws Exception {
        long start = System.nanoTime();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            runnable.run();
        }
        return System.nanoTime() - start;
    }

    private long timeOnce(ThrowingRunnable runnable) throws Exception {
        long start = System.nanoTime();
        runnable.run();
        return System.nanoTime() - start;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CommandContainer createViaReflection(Class<?> commandClass) throws Exception {
        AeshCommandContainerBuilder builder = new AeshCommandContainerBuilder();
        Command instance = (Command) commandClass.newInstance();
        return builder.create(instance);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CommandContainer createViaProvider(CommandMetadataProvider provider, Class<?> commandClass) throws Exception {
        Command instance = (Command) commandClass.newInstance();
        ProcessedCommand pc = provider.buildProcessedCommand(instance);
        return new AeshCommandContainer(
                CommandLineParserBuilder.builder()
                        .processedCommand(pc)
                        .create());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    // --- In-memory compilation infrastructure ---

    private CompilationResult compileWithProcessor(InMemorySource... sources) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Path outputDir = Files.createTempDirectory("aesh-benchmark");

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(outputDir.toFile()));

            List<File> cpFiles = new ArrayList<>();
            for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
                cpFiles.add(new File(entry));
            }
            fileManager.setLocation(StandardLocation.CLASS_PATH, cpFiles);

            // Run annotation processor
            JavaCompiler.CompilationTask procTask = compiler.getTask(
                    null, fileManager, diagnostics,
                    Arrays.asList("-proc:only", "-processor", AeshAnnotationProcessor.class.getName()),
                    null, Arrays.asList(sources));
            boolean procSuccess = procTask.call();

            // Collect generated sources
            List<JavaFileObject> allSources = new ArrayList<>(Arrays.asList(sources));
            Files.walk(outputDir)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        try {
                            String content = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                            String className = outputDir.relativize(p).toString()
                                    .replace(File.separatorChar, '.').replace(".java", "");
                            allSources.add(new InMemorySource(className, content));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

            // Compile everything
            DiagnosticCollector<JavaFileObject> compileDiagnostics = new DiagnosticCollector<>();
            JavaCompiler.CompilationTask compileTask = compiler.getTask(
                    null, fileManager, compileDiagnostics,
                    Arrays.asList("-proc:none"), null, allSources);
            boolean compileSuccess = compileTask.call();

            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{outputDir.toUri().toURL()}, getClass().getClassLoader());

            return new CompilationResult(procSuccess && compileSuccess,
                    diagnostics.getDiagnostics().toString() + compileDiagnostics.getDiagnostics().toString(),
                    classLoader);
        }
    }

    private static class CompilationResult {
        final boolean success;
        final String diagnostics;
        final URLClassLoader classLoader;

        CompilationResult(boolean success, String diagnostics, URLClassLoader classLoader) {
            this.success = success;
            this.diagnostics = diagnostics;
            this.classLoader = classLoader;
        }
    }

    private static class InMemorySource extends SimpleJavaFileObject {
        private final String code;

        InMemorySource(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}

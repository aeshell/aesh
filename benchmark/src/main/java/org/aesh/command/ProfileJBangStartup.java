package org.aesh.command;

import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.registry.MutableCommandRegistryImpl;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.metadata.MetadataProviderRegistry;

/**
 * Profiling harness for the JBang-like CLI application using the generated
 * (annotation processor) path only. Designed for use with async-profiler
 * via {@code -agentpath}.
 * <p>
 * Two modes:
 * <ul>
 * <li>{@code container} — loops container creation only
 * ({@code AeshCommandContainerBuilder.create(JBangCommand.class)}).
 * Isolates generated metadata resolution + ProcessedCommand construction.</li>
 * <li>{@code full} — loops the full end-to-end startup:
 * container creation + registry + runtime build + parse + execute.
 * Shows the complete cost a user experiences.</li>
 * </ul>
 * <p>
 * Usage:
 *
 * <pre>
 * # CPU profiling (container creation, 50k iterations):
 * java -agentpath:/path/to/libasyncProfiler.so=start,event=cpu,file=out.jfr,interval=1ms \
 *      -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints \
 *      -cp ... org.aesh.command.ProfileJBangStartup container 50000
 *
 * # CPU profiling (full startup, 10k iterations):
 * java -agentpath:/path/to/libasyncProfiler.so=start,event=cpu,file=out.jfr,interval=1ms \
 *      -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints \
 *      -cp ... org.aesh.command.ProfileJBangStartup full 10000
 * </pre>
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ProfileJBangStartup {

    private static final int WARMUP_ITERATIONS = 1000;

    /** Representative command lines covering different subcommands and option patterns. */
    private static final String[] COMMANDS = {
            "jbang run test.java",
            "jbang run --debug test.java -- --arg1 --arg2",
            "jbang build --java 21 test.java",
            "jbang init --template cli hello.java",
            "jbang edit --open idea test.java",
            "jbang alias --list",
            "jbang config --set key",
            "jbang export --format native test.java",
            "jbang info --tools test.java",
            "jbang --verbose run test.java",
    };

    public static void main(String[] args) throws Exception {
        // Explicitly register the generated metadata registry so the generated
        // path is used for all commands (the benchmark module lacks META-INF
        // service files that would enable automatic ServiceLoader discovery)
        MetadataProviderRegistry.register(new org.aesh.command._AeshMetadataRegistry());

        String mode = args.length > 0 ? args[0] : "container";
        int iterations = args.length > 1 ? Integer.parseInt(args[1]) : 50_000;

        switch (mode) {
            case "container":
                profileContainerCreation(iterations);
                break;
            case "full":
                profileFullStartup(iterations);
                break;
            default:
                System.err.println("Unknown mode: " + mode);
                System.err.println("Usage: ProfileJBangStartup <container|full> [iterations]");
                System.exit(1);
        }
    }

    /**
     * Profile container creation only (generated path).
     * Each iteration creates a fresh CommandContainer for the entire JBang
     * command hierarchy (11 subcommands, 5 mixins, ~35 options on run path).
     */
    private static void profileContainerCreation(int iterations) throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();

        System.out.println("Mode: container (generated path)");
        System.out.println("Warming up (" + WARMUP_ITERATIONS + " iterations)...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            builder.create(org.aesh.command.jbang.JBangCommand.class);
        }

        System.out.println("=== PROFILING START === (" + iterations + " iterations)");
        for (int i = 0; i < iterations; i++) {
            builder.create(org.aesh.command.jbang.JBangCommand.class);
        }
        System.out.println("=== PROFILING STOP ===");
    }

    /**
     * Profile full end-to-end startup (generated path).
     * Each iteration creates container + registry + runtime, then parses and
     * executes all representative command lines.
     */
    private static void profileFullStartup(int iterations) throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();

        System.out.println("Mode: full (generated path)");
        System.out.println("Warming up (" + WARMUP_ITERATIONS + " iterations)...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            runFullIteration(builder);
        }

        System.out.println("=== PROFILING START === (" + iterations + " iterations)");
        for (int i = 0; i < iterations; i++) {
            runFullIteration(builder);
        }
        System.out.println("=== PROFILING STOP ===");
    }

    private static void runFullIteration(AeshCommandContainerBuilder<CommandInvocation> builder) throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        registry.addCommand(builder.create(org.aesh.command.jbang.JBangCommand.class));
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(registry).build();
        for (String cmd : COMMANDS) {
            runtime.executeCommand(cmd);
        }
    }
}

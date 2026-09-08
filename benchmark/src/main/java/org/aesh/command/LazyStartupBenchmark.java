package org.aesh.command;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.aesh.AeshRuntimeRunner;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

public class LazyStartupBenchmark {

    private static final int CHILD_COUNT = 20;
    private static final int WARMUP_ITERATIONS = 50;
    private static final int MEASURED_ITERATIONS = 200;

    public abstract static class BaseChild implements Command<CommandInvocation> {
        static final AtomicInteger constructions = new AtomicInteger();

        @Option(description = "Child option")
        private String opt;

        protected BaseChild() {
            constructions.incrementAndGet();
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "c00", description = "child 0")
    public static class C00 extends BaseChild {
    }

    @CommandDefinition(name = "c01", description = "child 1")
    public static class C01 extends BaseChild {
    }

    @CommandDefinition(name = "c02", description = "child 2")
    public static class C02 extends BaseChild {
    }

    @CommandDefinition(name = "c03", description = "child 3")
    public static class C03 extends BaseChild {
    }

    @CommandDefinition(name = "c04", description = "child 4")
    public static class C04 extends BaseChild {
    }

    @CommandDefinition(name = "c05", description = "child 5")
    public static class C05 extends BaseChild {
    }

    @CommandDefinition(name = "c06", description = "child 6")
    public static class C06 extends BaseChild {
    }

    @CommandDefinition(name = "c07", description = "child 7")
    public static class C07 extends BaseChild {
    }

    @CommandDefinition(name = "c08", description = "child 8")
    public static class C08 extends BaseChild {
    }

    @CommandDefinition(name = "c09", description = "child 9")
    public static class C09 extends BaseChild {
    }

    @CommandDefinition(name = "c10", description = "child 10")
    public static class C10 extends BaseChild {
    }

    @CommandDefinition(name = "c11", description = "child 11")
    public static class C11 extends BaseChild {
    }

    @CommandDefinition(name = "c12", description = "child 12")
    public static class C12 extends BaseChild {
    }

    @CommandDefinition(name = "c13", description = "child 13")
    public static class C13 extends BaseChild {
    }

    @CommandDefinition(name = "c14", description = "child 14")
    public static class C14 extends BaseChild {
    }

    @CommandDefinition(name = "c15", description = "child 15")
    public static class C15 extends BaseChild {
    }

    @CommandDefinition(name = "c16", description = "child 16")
    public static class C16 extends BaseChild {
    }

    @CommandDefinition(name = "c17", description = "child 17")
    public static class C17 extends BaseChild {
    }

    @CommandDefinition(name = "c18", description = "child 18")
    public static class C18 extends BaseChild {
    }

    @CommandDefinition(name = "c19", description = "child 19")
    public static class C19 extends BaseChild {
    }

    @CommandDefinition(name = "bench", description = "bench root", generateHelp = true, groupCommands = {
            C00.class, C01.class, C02.class, C03.class, C04.class,
            C05.class, C06.class, C07.class, C08.class, C09.class,
            C10.class, C11.class, C12.class, C13.class, C14.class,
            C15.class, C16.class, C17.class, C18.class, C19.class })
    public static class BenchRoot implements Command<CommandInvocation> {
        static final AtomicInteger constructions = new AtomicInteger();

        public BenchRoot() {
            constructions.incrementAndGet();
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    private static long runOnce(boolean lazy) {
        return runScenario(lazy, new String[] { "c00" }, lazy ? 2 : CHILD_COUNT + 1);
    }

    private static long runHelpOnce(boolean lazy) {
        return runScenario(lazy, new String[] { "--help" }, lazy ? 0 : CHILD_COUNT + 1);
    }

    private static long runScenario(boolean lazy, String[] cmdArgs, int expectedConstructions) {
        BaseChild.constructions.set(0);
        BenchRoot.constructions.set(0);
        PrintStream original = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        CommandResult result;
        long elapsed;
        try {
            long start = System.nanoTime();
            AeshRuntimeRunner runner = AeshRuntimeRunner.builder().lazyStartup(lazy);
            result = runner.command(BenchRoot.class).args(cmdArgs).execute();
            elapsed = System.nanoTime() - start;
        } finally {
            System.setOut(original);
        }
        if (result != CommandResult.SUCCESS)
            throw new IllegalStateException("Unexpected result: " + result);
        int constructed = BaseChild.constructions.get() + BenchRoot.constructions.get();
        if (constructed != expectedConstructions)
            throw new IllegalStateException(
                    "Expected " + expectedConstructions + " constructions, got " + constructed + " (lazy=" + lazy
                            + ", args=" + String.join(" ", cmdArgs) + ")");
        return elapsed;
    }

    public static void main(String[] args) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            runOnce(false);
            runOnce(true);
            runHelpOnce(false);
            runHelpOnce(true);
        }
        long eagerTotal = 0;
        long lazyTotal = 0;
        long eagerHelpTotal = 0;
        long lazyHelpTotal = 0;
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            eagerTotal += runOnce(false);
            lazyTotal += runOnce(true);
            eagerHelpTotal += runHelpOnce(false);
            lazyHelpTotal += runHelpOnce(true);
        }
        System.out.println("=== Lazy Startup Benchmark (1 root group + " + CHILD_COUNT
                + " children, executing one child) ===");
        System.out.println("Mode   | Constructions | Mean time");
        System.out.println("Eager  | " + String.format("%13d", CHILD_COUNT + 1) + " | "
                + TimeUnit.NANOSECONDS.toMicros(eagerTotal / MEASURED_ITERATIONS) + " us");
        System.out.println("Lazy   | " + String.format("%13d", 2) + " | "
                + TimeUnit.NANOSECONDS.toMicros(lazyTotal / MEASURED_ITERATIONS) + " us");
        System.out.println("=== Lazy Startup Benchmark (--help) ===");
        System.out.println("Mode   | Constructions | Mean time");
        System.out.println("Eager  | " + String.format("%13d", CHILD_COUNT + 1) + " | "
                + TimeUnit.NANOSECONDS.toMicros(eagerHelpTotal / MEASURED_ITERATIONS) + " us");
        System.out.println("Lazy   | " + String.format("%13d", 0) + " | "
                + TimeUnit.NANOSECONDS.toMicros(lazyHelpTotal / MEASURED_ITERATIONS) + " us");
    }
}

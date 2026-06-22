package org.aesh.command.operator;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.ReadlineConsole;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Tests for the streaming pipe implementation (#532).
 * Verifies concurrent pipeline execution, back-pressure, early termination,
 * and large data handling.
 */
public class StreamingPipeTest {

    /**
     * Test that a large amount of data can flow through a pipe without OOM.
     * With the old ByteArrayOutputStream model, this would buffer everything.
     * With streaming pipes, data flows through the 8KB pipe buffer.
     */
    @Test
    public void testLargeDataPipe() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(LargeProducerCommand.class)
                .command(LineCounterCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .enableOperatorParser(true)
                .commandRegistry(registry)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // Produce 10000 lines, count them through pipe
        connection.read("large-producer | line-counter" + Config.getLineSeparator());
        Thread.sleep(2000);
        String output = connection.getOutputBuffer();
        assertTrue("Should count 10000 lines, got: " + output,
                output.contains("lines=10000"));

        console.stop();
    }

    /**
     * Test that pipe stages run concurrently — the producer and consumer
     * overlap in time rather than running sequentially.
     */
    @Test
    public void testConcurrentExecution() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();

        // Reset shared state
        ConcurrencyProducerCommand.producerStarted.set(false);
        ConcurrencyConsumerCommand.consumerStartedBeforeProducerFinished.set(false);
        ConcurrencyProducerCommand.producerFinished.set(false);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(ConcurrencyProducerCommand.class)
                .command(ConcurrencyConsumerCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .enableOperatorParser(true)
                .commandRegistry(registry)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("conc-producer | conc-consumer" + Config.getLineSeparator());
        Thread.sleep(2000);

        assertTrue("Consumer should have started before producer finished (concurrent execution)",
                ConcurrencyConsumerCommand.consumerStartedBeforeProducerFinished.get());

        console.stop();
    }

    /**
     * Test early termination — downstream command reads only the first line
     * and exits. The upstream command should not hang (SIGPIPE-like behavior).
     */
    @Test
    public void testEarlyTermination() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(SlowProducerCommand.class)
                .command(HeadCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .enableOperatorParser(true)
                .commandRegistry(registry)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // Head reads only 1 line; slow-producer should not hang
        connection.read("slow-producer | head-cmd" + Config.getLineSeparator());

        // Should complete within 3 seconds (slow-producer would take 10s+ if not terminated)
        String output = connection.waitForOutputContaining("first-line", 3000);
        assertTrue("Head should have read the first line",
                output.contains("first-line"));

        console.stop();
    }

    /**
     * Test pipe combined with output redirection: A | B > file
     */
    @Test
    public void testPipeWithRedirection() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();
        java.io.File tmpFile = java.io.File.createTempFile("aesh-pipe-test", ".txt");
        tmpFile.deleteOnExit();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(AeshCommandPipelineTest.PipeCommand.class)
                .command(AeshCommandPipelineTest.UpperCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .enableOperatorParser(true)
                .commandRegistry(registry)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("pipe | upper > " + tmpFile.getAbsolutePath() + Config.getLineSeparator());
        Thread.sleep(500);

        String content = new String(java.nio.file.Files.readAllBytes(tmpFile.toPath())).trim();
        assertTrue("File should contain uppercased output, got: " + content,
                content.contains("HELLO") && content.contains("AESH"));

        console.stop();
    }

    /**
     * Test that && after a pipe chain works correctly.
     */
    @Test
    public void testPipeFollowedByAnd() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(AeshCommandPipelineTest.PipeCommand.class)
                .command(AeshCommandPipelineTest.UpperCommand.class)
                .command(AeshCommandPipelineTest.BarCommand.class)
                .command(EchoCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .enableOperatorParser(true)
                .commandRegistry(registry)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        // pipe | upper should succeed, then echo should run
        connection.read("pipe | upper && echo" + Config.getLineSeparator());
        Thread.sleep(500);
        String output = connection.getOutputBuffer();
        assertTrue("Echo should have run after successful pipe chain",
                output.contains("echo-ran"));

        console.stop();
    }

    // ---- Test commands ----

    @CommandDefinition(name = "large-producer", description = "produces many lines")
    public static class LargeProducerCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            for (int i = 0; i < 10000; i++) {
                ci.println("line-" + i);
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "line-counter", description = "counts lines from stdin")
    public static class LineCounterCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            try {
                java.io.InputStream stdin = ci.getStdin();
                if (stdin == null) {
                    ci.println("lines=0");
                    return CommandResult.SUCCESS;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(stdin));
                int count = 0;
                while (reader.readLine() != null) {
                    count++;
                }
                ci.println("lines=" + count);
            } catch (IOException e) {
                throw new CommandException(e);
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "conc-producer", description = "producer for concurrency test")
    public static class ConcurrencyProducerCommand implements Command<CommandInvocation> {
        static final AtomicBoolean producerStarted = new AtomicBoolean(false);
        static final AtomicBoolean producerFinished = new AtomicBoolean(false);

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException, InterruptedException {
            producerStarted.set(true);
            // Write enough data to fill the pipe buffer, forcing back-pressure
            for (int i = 0; i < 1000; i++) {
                ci.println("data-" + i);
            }
            // Small delay to ensure consumer has time to observe concurrent state
            Thread.sleep(200);
            producerFinished.set(true);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "conc-consumer", description = "consumer for concurrency test")
    public static class ConcurrencyConsumerCommand implements Command<CommandInvocation> {
        static final AtomicBoolean consumerStartedBeforeProducerFinished = new AtomicBoolean(false);

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            // Check if producer is still running when we start reading
            if (ConcurrencyProducerCommand.producerStarted.get()
                    && !ConcurrencyProducerCommand.producerFinished.get()) {
                consumerStartedBeforeProducerFinished.set(true);
            }
            try {
                java.io.InputStream stdin = ci.getStdin();
                if (stdin != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stdin));
                    while (reader.readLine() != null) {
                        // Drain
                    }
                }
            } catch (IOException e) {
                // expected
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "slow-producer", description = "produces lines slowly")
    public static class SlowProducerCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException, InterruptedException {
            ci.println("first-line");
            for (int i = 1; i < 100; i++) {
                ci.println("line-" + i);
                Thread.sleep(100); // 10 seconds total if not interrupted
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "head-cmd", description = "reads only the first line")
    public static class HeadCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            try {
                java.io.InputStream stdin = ci.getStdin();
                if (stdin != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stdin));
                    String first = reader.readLine();
                    if (first != null)
                        ci.println(first);
                    // Close stdin — upstream should get pipe broken
                    stdin.close();
                }
            } catch (IOException e) {
                // expected
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "echo", description = "prints echo-ran")
    public static class EchoCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            ci.println("echo-ran");
            return CommandResult.SUCCESS;
        }
    }
}

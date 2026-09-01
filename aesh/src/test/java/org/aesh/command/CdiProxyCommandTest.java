package org.aesh.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.ReadlineConsole;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Tests that aesh handles CDI interceptor/proxy subclasses correctly (#608).
 * CDI frameworks create subclasses of command classes for method interception
 * (e.g., @Transactional). The subclass does not carry @CommandDefinition,
 * so aesh must walk the class hierarchy to find it.
 */
public class CdiProxyCommandTest {

    /**
     * Simulated CDI proxy: a plain subclass with no annotations.
     * This mimics what Quarkus ArC or WildFly CDI would generate.
     */
    public static class GreetCommand_Proxy extends GreetCommand {
        // CDI proxies typically add no fields and override intercepted methods.
        // For this test, the plain subclass is sufficient to trigger the bug.
    }

    /**
     * Verify that a proxy subclass instance can be registered and parsed
     * via the reflection path (AeshCommandContainerBuilder).
     */
    @Test
    public void testProxyCommandReflectionPath() throws Exception {
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();

        // Register the proxy instance — should find @CommandDefinition on the parent
        CommandContainer<CommandInvocation> container = builder.create(new GreetCommand_Proxy());

        assertNotNull("Container should be created for proxy subclass", container);
        assertEquals("greet", container.getParser().getProcessedCommand().name());

        // Parse and populate — options declared on the parent should be found
        container.getParser().populateObject("greet --name Alice",
                org.aesh.command.settings.SettingsBuilder.builder().build().invocationProviders(),
                null, org.aesh.command.impl.parser.CommandLineParser.Mode.VALIDATE);

        GreetCommand cmd = (GreetCommand) container.getParser().getCommand();
        assertEquals("Alice", cmd.name);
    }

    /**
     * Verify that a proxy subclass instance works end-to-end via
     * ReadlineConsole (interactive path).
     */
    @Test
    public void testProxyCommandInteractivePath() throws Exception {
        TestConnection connection = new TestConnection();
        CountDownLatch latch = new CountDownLatch(1);

        // Register the proxy instance via the command registry
        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(new GreetCommand_Proxy())
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .commandExecutionListener((line, result, durationMs) -> latch.countDown())
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("greet --name Bob" + Config.getLineSeparator());
        assertTrue("Command should complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Output should contain greeting",
                connection.getOutputBuffer().contains("Hello, Bob!"));

        console.stop();
    }

    /**
     * Verify that @CommandDefinition is inherited by subclasses
     * (the @Inherited annotation fix).
     */
    @Test
    public void testCommandDefinitionIsInherited() {
        // The proxy subclass should inherit @CommandDefinition from the parent
        CommandDefinition annotation = GreetCommand_Proxy.class.getAnnotation(CommandDefinition.class);
        assertNotNull("@CommandDefinition should be inherited by subclasses", annotation);
        assertEquals("greet", annotation.name());
    }

    // --- Test command ---

    @CommandDefinition(name = "greet", description = "Greet someone")
    public static class GreetCommand implements Command<CommandInvocation> {
        @Option(name = "name", defaultValue = "World")
        String name;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            ci.println("Hello, " + name + "!");
            return CommandResult.SUCCESS;
        }
    }
}

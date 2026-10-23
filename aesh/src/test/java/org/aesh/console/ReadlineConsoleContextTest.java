package org.aesh.console;

import static org.aesh.terminal.utils.Config.getLineSeparator;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.io.Resource;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Tests that ReadlineConsole propagates an explicitly configured AeshContext to its own runtime,
 * instead of always creating a fresh default context. Commands executed through the interactive
 * console must observe exactly the instance provided via SettingsBuilder.aeshContext(...).
 */
public class ReadlineConsoleContextTest {

    private static AeshContext customContext() {
        return new AeshContext() {
            @Override
            public Resource getCurrentWorkingDirectory() {
                return null;
            }

            @Override
            public void setCurrentWorkingDirectory(Resource cwd) {
            }

            @Override
            public Set<String> exportedVariableNames() {
                return Collections.emptySet();
            }

            @Override
            public String exportedVariable(String key) {
                return null;
            }
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void explicitSettingsContextIsUsedByConsole() {
        AeshContext custom = customContext();
        Settings settings = SettingsBuilder.builder().aeshContext(custom).build();
        ReadlineConsole console = new ReadlineConsole(settings);

        assertSame("console must use the explicitly configured context, not a fresh default",
                custom, console.context());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void unsetSettingsFallBackToDefaultContext() {
        Settings settings = SettingsBuilder.builder().build();
        ReadlineConsole console = new ReadlineConsole(settings);

        assertNotNull(console.context());
        assertTrue("without an explicit context the console keeps its managed default",
                console.context() instanceof DefaultAeshContext);
    }

    @CommandDefinition(name = "ctxprobe", description = "observes the runtime context")
    public static class ContextProbeCommand implements Command<CommandInvocation> {
        static volatile AeshContext observed;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            observed = invocation.getConfiguration().getAeshContext();
            invocation.println("ctx-seen");
            return CommandResult.SUCCESS;
        }
    }

    /**
     * A command executed through the console must observe the explicitly
     * configured context, not a fresh managed default. The probe command
     * captures what the runtime hands it; identity with the configured
     * instance proves the console stopped shadowing it.
     */
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void executedCommandObservesConfiguredContext() throws Exception {
        ContextProbeCommand.observed = null;
        AeshContext custom = customContext();
        TestConnection connection = new TestConnection();

        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(ContextProbeCommand.class).create();
        Settings settings = SettingsBuilder.builder()
                .connection(connection)
                .aeshContext(custom)
                .commandRegistry(registry)
                .build();

        AeshConsoleRunner runner = AeshConsoleRunner.builder().settings(settings);
        runner.start();

        connection.read("ctxprobe" + getLineSeparator());
        connection.assertBufferEndsWith("ctx-seen" + getLineSeparator());
        assertSame("executed command must see the explicitly configured context",
                custom, ContextProbeCommand.observed);

        runner.stop();
    }
}

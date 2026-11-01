package org.aesh.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.junit.Before;
import org.junit.Test;

/**
 * A group command can have arguments: a word that names a child command selects
 * it, any other word is an argument of the group (#702).
 */
public class GroupCommandArgumentsTest {

    private static String executed;
    private static String groupName;
    private static boolean groupMcp;
    private static String childName;
    private static List<String> groupFiles;

    @Before
    public void reset() {
        executed = null;
        groupName = null;
        groupMcp = false;
        childName = null;
        groupFiles = null;
    }

    private CommandRuntime<CommandInvocation> runtime(Class<? extends Command> command) throws Exception {
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(command).create();
        return AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();
    }

    @Test
    public void testGroupWithoutArgument() throws Exception {
        CommandResult result = runtime(TuiGroup.class).executeCommand("tui");
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("tui", executed);
        assertNull(groupName);
    }

    @Test
    public void testGroupArgument() throws Exception {
        CommandResult result = runtime(TuiGroup.class).executeCommand("tui myapp");
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("tui", executed);
        assertEquals("myapp", groupName);
    }

    @Test
    public void testGroupOptionAndArgument() throws Exception {
        CommandRuntime<CommandInvocation> runtime = runtime(TuiGroup.class);
        runtime.executeCommand("tui --mcp myapp");
        assertEquals("tui", executed);
        assertEquals("myapp", groupName);
        assertTrue(groupMcp);

        reset();
        runtime.executeCommand("tui myapp --mcp");
        assertEquals("tui", executed);
        assertEquals("myapp", groupName);
        assertTrue(groupMcp);
    }

    @Test
    public void testChildCommand() throws Exception {
        CommandRuntime<CommandInvocation> runtime = runtime(TuiGroup.class);
        CommandResult result = runtime.executeCommand("tui monitor");
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("monitor", executed);
        assertNull(childName);

        reset();
        runtime.executeCommand("tui monitor myapp");
        assertEquals("monitor", executed);
        assertEquals("myapp", childName);
        assertNull(groupName);
    }

    @Test
    public void testChildCommandAfterGroupOption() throws Exception {
        runtime(TuiGroup.class).executeCommand("tui --mcp monitor myapp");
        assertEquals("monitor", executed);
        assertEquals("myapp", childName);
        assertFalse(groupMcp);
    }

    @Test
    public void testGroupArgumentsList() throws Exception {
        CommandRuntime<CommandInvocation> runtime = runtime(FilesGroup.class);
        runtime.executeCommand("files a.yaml b.yaml");
        assertEquals("files", executed);
        assertEquals(Arrays.asList("a.yaml", "b.yaml"), groupFiles);

        reset();
        runtime.executeCommand("files monitor x");
        assertEquals("monitor", executed);
        assertEquals("x", childName);
        assertNull(groupFiles);
    }

    @CommandDefinition(name = "monitor", description = "")
    public static class MonitorCommand implements Command<CommandInvocation> {
        @Argument
        String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            executed = "monitor";
            childName = name;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "tui", description = "", groupCommands = { MonitorCommand.class })
    public static class TuiGroup implements Command<CommandInvocation> {
        @Option(hasValue = false)
        boolean mcp;

        @Argument
        String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            executed = "tui";
            groupName = name;
            groupMcp = mcp;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "files", description = "", groupCommands = { MonitorCommand.class })
    public static class FilesGroup implements Command<CommandInvocation> {
        @Arguments
        List<String> files;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            executed = "files";
            groupFiles = files;
            return CommandResult.SUCCESS;
        }
    }
}

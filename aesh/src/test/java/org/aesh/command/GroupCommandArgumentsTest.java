package org.aesh.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
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

    @Test
    public void testArgumentBeforeChildName() throws Exception {
        // doParse re-checks child names per word, so the child still wins. The
        // earlier word must have been consumed as the group argument — anything
        // else would fail — while the child gets no argument of its own.
        CommandResult result = runtime(TuiGroup.class).executeCommand("tui myapp monitor");
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("monitor", executed);
        assertNull(childName);
        assertNull(groupName);
    }

    @Test
    public void testRequiredGroupArgument() throws Exception {
        try {
            runtime(TuiReqGroup.class).executeCommand("tuireq");
            fail("expected required group argument to fail");
        } catch (CommandLineParserException expected) {
            assertTrue(expected.getMessage().contains("required"));
        }

        // Selecting a child bypasses the group's required argument: the group's
        // doParse (and its required check) never runs on the child path.
        CommandResult result = runtime(TuiReqGroup.class).executeCommand("tuireq monitor");
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("monitor", executed);
    }

    @Test
    public void testLazyChildNameStillSelectsChild() throws Exception {
        // With lazy child resolution the child is materialized on demand during
        // parsing; the name still wins over the group argument (#702).
        AeshCommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
        builder.setLazyChildResolution(true);
        CommandContainer<CommandInvocation> container = builder.create(new TuiGroup());
        assertTrue("child must still be unresolved before parsing",
                container.getParser().getChildParsers() == null
                        || container.getParser().getChildParsers().isEmpty());
        CommandRegistry<CommandInvocation> registry = AeshCommandRegistryBuilder.builder()
                .command(container).create();
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.builder()
                .commandRegistry(registry).build();

        CommandResult result = runtime.executeCommand("tui monitor myapp");
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("monitor", executed);
        assertEquals("myapp", childName);
        assertNull(groupName);
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

    @CommandDefinition(name = "tuireq", description = "", groupCommands = { MonitorCommand.class })
    public static class TuiReqGroup implements Command<CommandInvocation> {
        @Argument(required = true)
        String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            executed = "tuireq";
            groupName = name;
            return CommandResult.SUCCESS;
        }
    }
}

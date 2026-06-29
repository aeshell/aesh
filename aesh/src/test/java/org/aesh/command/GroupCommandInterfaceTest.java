package org.aesh.command;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.aesh.AeshRuntimeRunner;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.junit.Test;

/**
 * Tests for the GroupCommand interface pattern — commands that provide
 * subcommands dynamically via getCommands() without listing them in
 * the @CommandDefinition annotation (#542).
 * <p>
 * This is the pattern used by Quarkus Operator SDK and similar frameworks.
 */
public class GroupCommandInterfaceTest {

    private static String lastOutput;

    @Test
    public void testGroupCommandWithDynamicSubcommands() {
        // The Quarkus pattern: GroupCommand with getCommands(), no groupCommands={}
        lastOutput = null;
        CommandResult result = AeshRuntimeRunner.builder()
                .command(new DynamicGroupCmd())
                .args(new String[] { "greet", "--name", "world" })
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("Hello world", lastOutput);
    }

    @Test
    public void testGroupCommandWithDynamicSubcommands_MultipleChildren() {
        // Execute a second dynamic subcommand
        lastOutput = null;
        CommandResult result = AeshRuntimeRunner.builder()
                .command(new MultiDynamicGroupCmd())
                .args(new String[] { "greet", "--name", "aesh" })
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("Hello aesh", lastOutput);
    }

    @Test
    public void testGroupCommandWithStaticAndDynamic() {
        // Combined: static groupCommands={} AND GroupCommand.getCommands()
        lastOutput = null;
        CommandResult result = AeshRuntimeRunner.builder()
                .command(new CombinedGroupCmd())
                .args(new String[] { "greet", "--name", "world" })
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("Hello world", lastOutput);
    }

    @Test
    public void testGroupCommandWithStaticAndDynamic_StaticChild() {
        // The static child should also work
        lastOutput = null;
        CommandResult result = AeshRuntimeRunner.builder()
                .command(new CombinedGroupCmd())
                .args(new String[] { "static-child" })
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("static", lastOutput);
    }

    // ========== Constructor-injected subcommands (Quarkus pattern) ==========

    @Test
    public void testGroupCommandWithConstructorArgs() {
        // The Quarkus operator SDK pattern: subcommands receive injected state
        lastOutput = null;
        CommandResult result = AeshRuntimeRunner.builder()
                .command(new InjectedGroupCmd("v1.2.3"))
                .args(new String[] { "version" })
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("Version: v1.2.3", lastOutput);
    }

    @Test
    public void testGroupCommandWithConstructorArgs_MultipleChildren() {
        lastOutput = null;
        CommandResult result = AeshRuntimeRunner.builder()
                .command(new InjectedGroupCmd("v2.0"))
                .args(new String[] { "info", "--detail", "full" })
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("Info(v2.0): full", lastOutput);
    }

    @Test
    public void testGroupCommandWithMixedInjectedAndStatic() {
        // Combine constructor-injected dynamic children with static children
        lastOutput = null;
        CommandResult result = AeshRuntimeRunner.builder()
                .command(new MixedInjectedGroupCmd("conf-data"))
                .args(new String[] { "config" })
                .execute();

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals("Config: conf-data", lastOutput);
    }

    // ---- Test commands ----

    @CommandDefinition(name = "app", description = "Dynamic group", generateHelp = true)
    public static class DynamicGroupCmd implements GroupCommand<CommandInvocation> {
        @Override
        public List<Command<CommandInvocation>> getCommands() {
            return Arrays.<Command<CommandInvocation>> asList(new GreetCmd());
        }

        @Override
        public CommandResult execute(CommandInvocation ci) {
            ci.println(ci.getHelpInfo());
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "app", description = "Combined group", generateHelp = true, groupCommands = {
            StaticChildCmd.class })
    public static class CombinedGroupCmd implements GroupCommand<CommandInvocation> {
        @Override
        public List<Command<CommandInvocation>> getCommands() {
            return Arrays.<Command<CommandInvocation>> asList(new GreetCmd());
        }

        @Override
        public CommandResult execute(CommandInvocation ci) {
            ci.println(ci.getHelpInfo());
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "app", description = "Multi dynamic group", generateHelp = true)
    public static class MultiDynamicGroupCmd implements GroupCommand<CommandInvocation> {
        @Override
        public List<Command<CommandInvocation>> getCommands() {
            return Arrays.<Command<CommandInvocation>> asList(new GreetCmd(), new ByeCmd());
        }

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "bye", description = "Say goodbye")
    public static class ByeCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            lastOutput = "Goodbye";
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "greet", description = "Say hello")
    public static class GreetCmd implements Command<CommandInvocation> {
        @Option(shortName = 'n', required = true)
        private String name;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            lastOutput = "Hello " + name;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "static-child", description = "Static child")
    public static class StaticChildCmd implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            lastOutput = "static";
            return CommandResult.SUCCESS;
        }
    }

    // --- Constructor-injected subcommand commands (Quarkus pattern) ---

    @CommandDefinition(name = "qosdk", description = "Injected group")
    public static class InjectedGroupCmd implements GroupCommand<CommandInvocation> {
        private final String version;

        public InjectedGroupCmd(String version) {
            this.version = version;
        }

        @Override
        public List<Command<CommandInvocation>> getCommands() {
            return Arrays.<Command<CommandInvocation>> asList(
                    new VersionCmd(version),
                    new InfoCmd(version));
        }

        @Override
        public CommandResult execute(CommandInvocation ci) {
            ci.println(ci.getHelpInfo());
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "version", description = "Show version")
    public static class VersionCmd implements Command<CommandInvocation> {
        private final String version;

        public VersionCmd(String version) {
            this.version = version;
        }

        @Override
        public CommandResult execute(CommandInvocation ci) {
            lastOutput = "Version: " + version;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "info", description = "Show info")
    public static class InfoCmd implements Command<CommandInvocation> {
        private final String version;

        @Option(description = "Detail level")
        private String detail;

        public InfoCmd(String version) {
            this.version = version;
        }

        @Override
        public CommandResult execute(CommandInvocation ci) {
            lastOutput = "Info(" + version + "): " + detail;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mixed", description = "Mixed injected", groupCommands = { StaticChildCmd.class })
    public static class MixedInjectedGroupCmd implements GroupCommand<CommandInvocation> {
        private final String data;

        public MixedInjectedGroupCmd(String data) {
            this.data = data;
        }

        @Override
        public List<Command<CommandInvocation>> getCommands() {
            return Arrays.<Command<CommandInvocation>> asList(new ConfigCmd(data));
        }

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "config", description = "Show config")
    public static class ConfigCmd implements Command<CommandInvocation> {
        private final String data;

        public ConfigCmd(String data) {
            this.data = data;
        }

        @Override
        public CommandResult execute(CommandInvocation ci) {
            lastOutput = "Config: " + data;
            return CommandResult.SUCCESS;
        }
    }
}

package org.aesh.util.completer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.junit.Test;

/**
 * Tests that ${...} description variables are resolved in completion scripts (#531).
 */
public class CompletionVariableResolutionTest {

    @Test
    public void testZshCommandDescriptionResolved() throws Exception {
        CommandLineParser<?> parser = createParser(ToolGroupCommand.class);
        String script = new ZshCompletionGenerator().generate(parser, "mytool");
        // The child command description uses ${ROOT-COMMAND-NAME}
        assertTrue("Zsh script should contain resolved root command name",
                script.contains("Generate completion for mytool"));
        assertFalse("Zsh script should NOT contain raw variable",
                script.contains("${ROOT-COMMAND-NAME}"));
    }

    @Test
    public void testZshOptionDescriptionResolved() throws Exception {
        CommandLineParser<?> parser = createParser(ToolGroupCommand.class);
        String script = new ZshCompletionGenerator().generate(parser, "mytool");
        // The option description uses ${COMMAND-NAME}
        assertTrue("Zsh script should contain resolved command name in option desc",
                script.contains("Shell type for completion"));
        assertFalse("Zsh script should NOT contain raw ${COMMAND-NAME}",
                script.contains("${COMMAND-NAME}"));
    }

    @Test
    public void testFishCommandDescriptionResolved() throws Exception {
        CommandLineParser<?> parser = createParser(ToolGroupCommand.class);
        String script = new FishCompletionGenerator().generate(parser, "mytool");
        assertTrue("Fish script should contain resolved root command name",
                script.contains("Generate completion for mytool"));
        assertFalse("Fish script should NOT contain raw variable",
                script.contains("${ROOT-COMMAND-NAME}"));
    }

    @Test
    public void testFishOptionDescriptionResolved() throws Exception {
        CommandLineParser<?> parser = createParser(ToolGroupCommand.class);
        String script = new FishCompletionGenerator().generate(parser, "mytool");
        assertTrue("Fish script should contain resolved command name in option desc",
                script.contains("Shell type for completion"));
        assertFalse("Fish script should NOT contain raw ${COMMAND-NAME}",
                script.contains("${COMMAND-NAME}"));
    }

    @Test
    public void testPowerShellCommandDescriptionResolved() throws Exception {
        CommandLineParser<?> parser = createParser(ToolGroupCommand.class);
        String script = new PowerShellCompletionGenerator().generate(parser, "mytool");
        assertTrue("PowerShell script should contain resolved root command name",
                script.contains("Generate completion for mytool"));
        assertFalse("PowerShell script should NOT contain raw variable",
                script.contains("${ROOT-COMMAND-NAME}"));
    }

    @Test
    public void testPowerShellOptionDescriptionResolved() throws Exception {
        CommandLineParser<?> parser = createParser(ToolGroupCommand.class);
        String script = new PowerShellCompletionGenerator().generate(parser, "mytool");
        assertTrue("PowerShell script should contain resolved command name in option desc",
                script.contains("Shell type for completion"));
        assertFalse("PowerShell script should NOT contain raw ${COMMAND-NAME}",
                script.contains("${COMMAND-NAME}"));
    }

    @Test
    public void testSimpleCommandDescriptionResolved() throws Exception {
        CommandLineParser<?> parser = createParser(SimpleVarCommand.class);
        String script = new ZshCompletionGenerator().generate(parser, "simple");
        // Option description uses ${ROOT-COMMAND-NAME}
        assertTrue("Simple command option desc should resolve ${ROOT-COMMAND-NAME}",
                script.contains("Output file for simple"));
        assertFalse("Should NOT contain raw ${ROOT-COMMAND-NAME}",
                script.contains("${ROOT-COMMAND-NAME}"));
    }

    @Test
    public void testDescriptionWithoutVariablesPassesThrough() throws Exception {
        CommandLineParser<?> parser = createParser(PlainCommand.class);
        String script = new ZshCompletionGenerator().generate(parser, "plain");
        // Plain descriptions should pass through unchanged
        assertTrue("Plain description should appear as-is",
                script.contains("A plain option"));
    }

    // ---- Helper ----

    @SuppressWarnings("unchecked")
    private CommandLineParser<?> createParser(Class<? extends Command> cmdClass) throws Exception {
        CommandContainer<?> container = new AeshCommandContainerBuilder<>().create(cmdClass);
        return container.getParser();
    }

    // ---- Test commands ----

    @CommandDefinition(name = "tool", description = "Tool suite for ${ROOT-COMMAND-NAME}", generateHelp = true, groupCommands = {
            CompletionSubCommand.class })
    public static class ToolGroupCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "completion", description = "Generate completion for ${ROOT-COMMAND-NAME}", generateHelp = true)
    public static class CompletionSubCommand implements Command<CommandInvocation> {

        @Option(name = "shell", description = "Shell type for ${COMMAND-NAME}")
        private String shell;

        @Argument(description = "Output path for ${ROOT-COMMAND-NAME}")
        private String output;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "simple", description = "Simple command", generateHelp = true)
    public static class SimpleVarCommand implements Command<CommandInvocation> {

        @Option(description = "Output file for ${ROOT-COMMAND-NAME}")
        private String output;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "plain", description = "Plain command", generateHelp = true)
    public static class PlainCommand implements Command<CommandInvocation> {

        @Option(description = "A plain option")
        private String value;

        @Override
        public CommandResult execute(CommandInvocation ci) throws CommandException {
            return CommandResult.SUCCESS;
        }
    }
}

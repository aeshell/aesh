package org.aesh.command.help;

import static org.junit.Assert.*;

import java.io.IOException;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionVisibility;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.ReadlineConsole;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;
import org.junit.Test;

public class OptionVisibilityTest {

    @Test
    public void testHiddenOptionNotInHelp() throws Exception {
        ProcessedCommand pc = ProcessedCommandBuilder.builder()
                .name("test")
                .description("test command")
                .generateHelp(true)
                .create();

        pc.addOption(ProcessedOptionBuilder.builder()
                .name("visible")
                .type(String.class)
                .fieldName("visible")
                .description("a visible option")
                .optionType(OptionType.NORMAL)
                .visibility(OptionVisibility.BRIEF)
                .build());

        pc.addOption(ProcessedOptionBuilder.builder()
                .name("secret")
                .type(String.class)
                .fieldName("secret")
                .description("a hidden option")
                .optionType(OptionType.NORMAL)
                .visibility(OptionVisibility.HIDDEN)
                .build());

        String help = pc.printHelp("test");
        assertTrue(help.contains("--visible"));
        assertFalse("HIDDEN option should not appear in help", help.contains("--secret"));
    }

    @Test
    public void testFullOptionNotInDefaultHelp() throws Exception {
        ProcessedCommand pc = ProcessedCommandBuilder.builder()
                .name("test")
                .description("test command")
                .generateHelp(true)
                .create();

        pc.addOption(ProcessedOptionBuilder.builder()
                .name("basic")
                .type(String.class)
                .fieldName("basic")
                .description("a basic option")
                .optionType(OptionType.NORMAL)
                .visibility(OptionVisibility.BRIEF)
                .build());

        pc.addOption(ProcessedOptionBuilder.builder()
                .name("advanced")
                .type(String.class)
                .fieldName("advanced")
                .description("an advanced option")
                .optionType(OptionType.NORMAL)
                .visibility(OptionVisibility.FULL)
                .build());

        String defaultHelp = pc.printHelp("test", false, false);
        assertTrue(defaultHelp.contains("--basic"));
        assertFalse("FULL option should not appear in default help", defaultHelp.contains("--advanced"));

        String fullHelp = pc.printHelp("test", false, true);
        assertTrue(fullHelp.contains("--basic"));
        assertTrue("FULL option should appear with showAll=true", fullHelp.contains("--advanced"));
    }

    @Test
    public void testHiddenOptionNeverInHelp() throws Exception {
        ProcessedCommand pc = ProcessedCommandBuilder.builder()
                .name("test")
                .description("test command")
                .generateHelp(true)
                .create();

        pc.addOption(ProcessedOptionBuilder.builder()
                .name("hidden")
                .type(String.class)
                .fieldName("hidden")
                .description("a hidden option")
                .optionType(OptionType.NORMAL)
                .visibility(OptionVisibility.HIDDEN)
                .build());

        String defaultHelp = pc.printHelp("test", false, false);
        assertFalse(defaultHelp.contains("--hidden"));

        String fullHelp = pc.printHelp("test", false, true);
        assertFalse("HIDDEN option should not appear even with showAll", fullHelp.contains("--hidden"));
    }

    @Test
    public void testHiddenOptionExcludedFromCompletion() throws Exception {
        ProcessedCommand pc = ProcessedCommandBuilder.builder()
                .name("test")
                .description("test command")
                .generateHelp(true)
                .create();

        pc.addOption(ProcessedOptionBuilder.builder()
                .name("visible")
                .type(String.class)
                .fieldName("visible")
                .description("visible")
                .optionType(OptionType.NORMAL)
                .visibility(OptionVisibility.BRIEF)
                .build());

        pc.addOption(ProcessedOptionBuilder.builder()
                .name("hidden")
                .type(String.class)
                .fieldName("hidden")
                .description("hidden")
                .optionType(OptionType.NORMAL)
                .visibility(OptionVisibility.HIDDEN)
                .build());

        java.util.List<org.aesh.terminal.formatting.TerminalString> longNames = pc.getOptionLongNamesWithDash();
        boolean hasVisible = longNames.stream().anyMatch(ts -> ts.toString().contains("visible"));
        boolean hasHidden = longNames.stream().anyMatch(ts -> ts.toString().contains("hidden"));
        assertTrue("BRIEF option should appear in completion", hasVisible);
        assertFalse("HIDDEN option should not appear in completion", hasHidden);

        java.util.List<org.aesh.terminal.formatting.TerminalString> possible = pc.findPossibleLongNamesWithDash("h");
        boolean possibleHasHidden = possible.stream().anyMatch(ts -> ts.toString().contains("hidden"));
        assertFalse("HIDDEN option should not appear in partial completion", possibleHasHidden);
    }

    @Test
    public void testDefaultVisibilityIsBrief() throws Exception {
        ProcessedCommand pc = ProcessedCommandBuilder.builder()
                .name("test")
                .description("test command")
                .generateHelp(true)
                .create();

        pc.addOption(ProcessedOptionBuilder.builder()
                .name("normal")
                .type(String.class)
                .fieldName("normal")
                .description("a normal option")
                .optionType(OptionType.NORMAL)
                .build());

        String help = pc.printHelp("test");
        assertTrue("Default visibility (BRIEF) options should always appear", help.contains("--normal"));
    }

    @Test
    public void testHelpAllIntegration() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(VisibilityCommand.class)
                .create();

        Settings<CommandInvocation> settings = SettingsBuilder
                .builder()
                .commandRegistry(registry)
                .enableOperatorParser(true)
                .connection(connection)
                .setPersistExport(false)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("vis --help" + Config.getLineSeparator());
        Thread.sleep(100);
        String defaultOutput = connection.getOutputBuffer();
        assertTrue(defaultOutput.contains("--name"));
        assertFalse("--debug should not appear in default help", defaultOutput.contains("--debug"));
        assertFalse("--internal should not appear in default help", defaultOutput.contains("--internal"));
        connection.clearOutputBuffer();

        connection.read("vis --help=all" + Config.getLineSeparator());
        Thread.sleep(100);
        String fullOutput = connection.getOutputBuffer();
        assertTrue(fullOutput.contains("--name"));
        assertTrue("--debug should appear with --help=all", fullOutput.contains("--debug"));
        assertFalse("--internal should never appear in help", fullOutput.contains("--internal"));

        console.stop();
    }

    @CommandDefinition(name = "vis", generateHelp = true, description = "visibility test")
    public static class VisibilityCommand implements Command {

        @Option(description = "your name")
        String name;

        @Option(description = "enable debug", visibility = OptionVisibility.FULL)
        boolean debug;

        @Option(description = "internal use only", visibility = OptionVisibility.HIDDEN)
        String internal;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }
}

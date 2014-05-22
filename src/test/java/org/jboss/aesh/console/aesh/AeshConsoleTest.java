/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.aesh;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.OptionList;
import org.jboss.aesh.cl.builder.CommandBuilder;
import org.jboss.aesh.cl.builder.OptionBuilder;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.parser.AeshCommandLineParser;
import org.jboss.aesh.cl.validator.OptionValidator;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.validator.ValidatorInvocation;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.KeyOperation;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshConsoleTest extends BaseConsoleTest {

    private KeyOperation completeChar =  new KeyOperation(Key.CTRL_I, Operation.COMPLETE);

    @Test
    public void testAeshConsole() throws IOException, InterruptedException, CommandLineParserException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(new ByteArrayOutputStream()))
                .logging(true)
                .create();

        ProcessedCommand fooCommand = new CommandBuilder()
                .name("foo")
                .description("fooing")
                .addOption(new OptionBuilder()
                        .name("bar")
                        .addDefaultValue("en")
                        .addDefaultValue("to")
                        .type(String.class)
                        .fieldName("bar")
                        .create())
                .generateCommand();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(new AeshCommandLineParser(fooCommand), FooTestCommand.class)
                .command(LsCommand.class)
                .create();

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry);

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();

        outputStream.write(("foo").getBytes());
        //outputStream.write(completeChar.getFirstValue());
        outputStream.write(Config.getLineSeparator().getBytes());
        outputStream.flush();

        outputStream.write("ls --files /home:/tmp".getBytes());
        outputStream.write(Config.getLineSeparator().getBytes());
        outputStream.flush();

        Thread.sleep(100);
        aeshConsole.stop();
    }

    public static class FooTestCommand implements Command {

        private String bar;

        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            assertEquals("en", bar);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="ls", description = "[OPTION]... [FILE]...")
    public class LsCommand implements Command {

        @Option(hasValue = false, description = "set foo to true/false")
        private Boolean foo;

        @Option(hasValue = false, description = "set the bar")
        private boolean bar;

        @Option(defaultValue = {"MORE"}, argument = "SIZE")
        private String less;

        @OptionList(defaultValue = "/tmp", description = "file location", valueSeparator = ':',
                validator = DirectoryValidator.class)
        List<File> files;

        @Option(hasValue = false, description = "display this help and exit")
        private boolean help;

        @Arguments
        private java.util.List<File> arguments;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            assertEquals(2, files.size());
            return CommandResult.SUCCESS;
        }
    }

    public class DirectoryValidator implements OptionValidator<DirectoryValidatorInvocation> {
        @Override
        public void validate(DirectoryValidatorInvocation validatorInvocation) throws OptionValidatorException {
            if(!validatorInvocation.getValue().isDirectory()) {
                throw new OptionValidatorException("File validation failed, must be a directory.");
            }
        }
    }

    public class DirectoryValidatorInvocation implements ValidatorInvocation<File, Command> {

        private final File value;

        public DirectoryValidatorInvocation(File value) {
            this.value = value;
        }

        @Override
        public File getValue() {
            return value;
        }

        @Override
        public Command getCommand() {
            return null;
        }

        @Override
        public AeshContext getAeshContext() {
            return null;
        }
    }
}

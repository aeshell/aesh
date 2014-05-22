/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.aesh;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.validator.OptionValidator;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.validator.AeshValidatorInvocation;
import org.jboss.aesh.console.command.validator.ValidatorInvocation;
import org.jboss.aesh.console.command.validator.ValidatorInvocationProvider;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandOptionValidatorTest {

    @Test
    public void testOptionValidator() throws IOException, InterruptedException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .logging(true)
                .create();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ValCommand.class)
                .create();

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();

        aeshConsole.start();
        outputStream.write(("val --foo yay"+ Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(100);

        assertFalse(byteArrayOutputStream.toString().contains("Option value cannot"));
        outputStream.write(("val --foo doh\\ doh" + Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(100);
        assertTrue(byteArrayOutputStream.toString().contains("Option value cannot"));

        aeshConsole.stop();
    }

    @Test
    public void testMultipleOptionValidators() throws IOException, InterruptedException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .logging(true)
                .create();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ValCommand.class)
                .command(IntCommand.class)
                .create();

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();

        aeshConsole.start();

        outputStream.write(("val --foo yay"+ Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(100);
        assertFalse(byteArrayOutputStream.toString().contains("Option value cannot"));

        outputStream.write(("val --foo yay\\ nay" + Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(100);
        assertTrue(byteArrayOutputStream.toString().contains("Option value cannot"));

        outputStream.write(("int --num 43" + Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(100);
        assertTrue(byteArrayOutputStream.toString().contains("Number cannot be higher than 42"));

         aeshConsole.stop();
    }

    @Test
    public void testMultipleOptionWithProvidersValidators() throws IOException, InterruptedException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .logging(true)
                .create();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ValCommand.class)
                .command(Val2Command.class)
                .command(IntCommand.class)
                .create();

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .validatorInvocationProvider(new TestValidatorInvocationProvider())
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();

        aeshConsole.start();

        outputStream.write(("val2 --foo yay"+ Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(100);
        assertFalse(byteArrayOutputStream.toString().contains("Option value cannot"));

        outputStream.write(("val2 --foo Doh" + Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(100);
        assertTrue(byteArrayOutputStream.toString().contains("NO UPPER"));

        outputStream.write(("val --foo yay\\ nay" + Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(100);
        assertTrue(byteArrayOutputStream.toString().contains("Option value cannot"));

        outputStream.write(("int --num 43" + Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(100);
        assertTrue(byteArrayOutputStream.toString().contains("Number cannot be higher than 42"));

         aeshConsole.stop();
    }

    @CommandDefinition(name = "val", description = "")
    public static class ValCommand implements Command {
        @Option(validator = NoSpaceValidator.class)
        private String foo;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            commandInvocation.getShell().out().println("VAL");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "int", description = "")
    public static class IntCommand implements Command {
        @Option(validator = IntValidator.class)
        private Integer num;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            commandInvocation.getShell().out().println("NUM");
            return CommandResult.SUCCESS;
        }
    }

    public static class IntValidator implements OptionValidator {
        @Override
        public void validate(ValidatorInvocation validatorInvocation) throws OptionValidatorException {
            if(((Integer) validatorInvocation.getValue()) > 42)
                throw new OptionValidatorException("Number cannot be higher than 42");
        }
    }

    public static class NoSpaceValidator implements OptionValidator {

        @Override
        public void validate(ValidatorInvocation validatorInvocation) throws OptionValidatorException {
            String s = (String) validatorInvocation.getValue();
            if(s.contains(" "))
                throw new OptionValidatorException("Option value cannot contain spaces");
        }
    }

    @CommandDefinition(name = "val2", description = "")
    public static class Val2Command implements Command {
        @Option(validator = NoUpperCaseValidator.class)
        private String foo;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            commandInvocation.getShell().out().println("VAL2");
            return CommandResult.SUCCESS;
        }
    }

    public static class NoUpperCaseValidator implements OptionValidator<TestValidatorInvocation> {

        @Override
        public void validate(TestValidatorInvocation validatorInvocation) throws OptionValidatorException {
            if(!validatorInvocation.getValue().toLowerCase().equals(validatorInvocation.getValue()))
                throw new OptionValidatorException("NO UPPER CASE!");
        }
    }

    public static class TestValidatorInvocation implements ValidatorInvocation<String,Command> {
        private String value;
        private Command command;
        private AeshContext aeshContext;

        public TestValidatorInvocation(String value, Command command, AeshContext aeshContext) {
            this.value = value;
            this.command = command;
            this.aeshContext = aeshContext;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public Command getCommand() {
            return command;
        }

        @Override
        public AeshContext getAeshContext() {
            return aeshContext;
        }
    }

    public static class TestValidatorInvocationProvider implements ValidatorInvocationProvider<ValidatorInvocation<String, Command>, String, Command> {
        @Override
        public ValidatorInvocation<String, Command> enhanceValidatorInvocation(ValidatorInvocation<String, Command> validatorInvocation) {
            if(validatorInvocation.getValue() instanceof String )
                return new TestValidatorInvocation((String) validatorInvocation.getValue(),
                        (Command) validatorInvocation.getCommand(), validatorInvocation.getAeshContext());
            else
                return new AeshValidatorInvocation(validatorInvocation.getValue(),
                        validatorInvocation.getCommand(), validatorInvocation.getAeshContext());
        }
    }
}

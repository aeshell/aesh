/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.validator;

import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.readline.AeshContext;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.terminal.utils.Config;
import org.aesh.command.CommandDefinition;
import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.impl.validator.AeshValidatorInvocation;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.junit.Test;

import java.io.IOException;
import org.aesh.command.CommandException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandOptionValidatorTest {

    @Test
    public void testOptionValidator() throws IOException, CommandRegistryException, InterruptedException {
        TestConnection connection = new TestConnection();

       CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(ValCommand.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .commandRegistry(registry)
                        .connection(connection)
                        .logging(true)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);

        console.start();
        connection.read("val --foo yay"+ Config.getLineSeparator());
        connection.assertBuffer("val --foo yay"+Config.getLineSeparator()+"VAL"+Config.getLineSeparator());
        connection.clearOutputBuffer();
        connection.read("val --foo doh\\ doh" + Config.getLineSeparator());
        connection.assertBufferEndsWith("Option value cannot contain spaces"+Config.getLineSeparator());

        console.stop();
    }

    @Test
    public void testMultipleOptionValidators() throws IOException, CommandRegistryException, InterruptedException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(ValCommand.class)
                .command(IntCommand.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .commandRegistry(registry)
                        .connection(connection)
                        .logging(true)
                        .build();

        ReadlineConsole aeshConsole = new ReadlineConsole(settings);

        aeshConsole.start();

        connection.read("val --foo yay"+ Config.getLineSeparator());
        connection.assertBuffer("val --foo yay"+Config.getLineSeparator()+"VAL"+Config.getLineSeparator());
        connection.clearOutputBuffer();

        connection.read("val --foo yay\\ nay" + Config.getLineSeparator());
        connection.assertBufferEndsWith("Option value cannot contain spaces"+Config.getLineSeparator());

        connection.read("int --num 43" + Config.getLineSeparator());
        connection.assertBufferEndsWith("Number cannot be higher than 42"+Config.getLineSeparator());

         aeshConsole.stop();
    }

    @Test
    public void testMultipleOptionWithProvidersValidators() throws IOException, CommandRegistryException, InterruptedException {
        TestConnection connection = new TestConnection();

       CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(ValCommand.class)
                .command(Val2Command.class)
                .command(IntCommand.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .commandRegistry(registry)
                        .connection(connection)
                        .logging(true)
                        .validatorInvocationProvider(new TestValidatorInvocationProvider())
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);

        console.start();

        connection.read("val2 --foo yay"+Config.getLineSeparator());
        connection.assertBuffer("val2 --foo yay"+ Config.getLineSeparator()+"VAL2"+Config.getLineSeparator());

        connection.read("val2 --foo Doh" + Config.getLineSeparator());
        connection.assertBufferEndsWith("NO UPPER CASE!"+Config.getLineSeparator());

        connection.read("val --foo yay\\ nay" + Config.getLineSeparator());
        connection.assertBufferEndsWith("Option value cannot contain spaces"+Config.getLineSeparator());

        connection.read("int --num 43" + Config.getLineSeparator());
        connection.assertBufferEndsWith("Number cannot be higher than 42"+Config.getLineSeparator());

         console.stop();
    }

    @Test
    public void testRequiredOption() throws IOException, CommandRegistryException, InterruptedException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(ValidatorOptionCommand.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .commandRegistry(registry)
                        .connection(connection)
                        .logging(true)
                        .validatorInvocationProvider(new TestValidatorInvocationProvider())
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);

        console.start();
        connection.read("test argvalue"+Config.getLineSeparator());
        Thread.sleep(20);
        connection.assertBufferEndsWith("Option: --foo is required for this command."+Config.getLineSeparator());

        console.stop();
    }


    @CommandDefinition(name = "test", description = "")
    public static class ValidatorOptionCommand implements Command {

        @Option(required = true)
        private String foo;

        @Argument
        private String arg;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }


    @CommandDefinition(name = "val", description = "")
    public static class ValCommand implements Command {
        @Option(validator = NoSpaceValidator.class)
        private String foo;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.println("VAL");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "int", description = "")
    public static class IntCommand implements Command {
        @Option(validator = IntValidator.class)
        private Integer num;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.println("NUM");
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
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.println("VAL2");
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
        private final String value;
        private final Command command;
        private final AeshContext aeshContext;

        TestValidatorInvocation(String value, Command command, AeshContext aeshContext) {
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

    public static class TestValidatorInvocationProvider implements ValidatorInvocationProvider<ValidatorInvocation<String, Command>> {
        @Override
        @SuppressWarnings("unchecked")
        public ValidatorInvocation<String, Command> enhanceValidatorInvocation(ValidatorInvocation validatorInvocation) {
            if(validatorInvocation.getValue() instanceof String )
                return new TestValidatorInvocation((String) validatorInvocation.getValue(),
                        (Command) validatorInvocation.getCommand(), validatorInvocation.getAeshContext());
            else
                return new AeshValidatorInvocation(validatorInvocation.getValue(),
                        validatorInvocation.getCommand(), validatorInvocation.getAeshContext());
        }
    }
}

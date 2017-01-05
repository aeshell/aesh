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
package org.aesh.console.aesh;

import org.aesh.command.option.Option;
import org.aesh.command.validator.OptionValidator;
import org.aesh.console.AeshContext;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.console.settings.Settings;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.util.Config;
import org.aesh.command.CommandDefinition;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.impl.validator.AeshValidatorInvocation;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.command.validator.ValidatorInvocationProvider;
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
    public void testOptionValidator() throws IOException, InterruptedException {
        TestConnection connection = new TestConnection();

       CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ValCommand.class)
                .create();

        Settings settings = new SettingsBuilder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .create();

        ReadlineConsole console = new ReadlineConsole(settings);

        console.start();
        connection.read("val --foo yay"+ Config.getLineSeparator());
        Thread.sleep(50);
        connection.assertBufferEndsWith("VAL"+Config.getLineSeparator());
        connection.clearOutputBuffer();
        connection.read("val --foo doh\\ doh" + Config.getLineSeparator());
        Thread.sleep(50);
        connection.assertBufferEndsWith("Option value cannot contain spaces"+Config.getLineSeparator());

        console.stop();
    }

    @Test
    public void testMultipleOptionValidators() throws IOException, InterruptedException {
        TestConnection connection = new TestConnection();

       CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ValCommand.class)
                .command(IntCommand.class)
                .create();

        Settings settings = new SettingsBuilder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .create();

        ReadlineConsole aeshConsole = new ReadlineConsole(settings);

        aeshConsole.start();

        connection.read("val --foo yay"+ Config.getLineSeparator());
        //outputStream.flush();
        Thread.sleep(100);
        //assertFalse(byteArrayOutputStream.toString().contains("Option value cannot"));

        connection.read("val --foo yay\\ nay" + Config.getLineSeparator());
        //outputStream.flush();
        Thread.sleep(100);
        //assertTrue(byteArrayOutputStream.toString().contains("Option value cannot"));

        connection.read("int --num 43" + Config.getLineSeparator());
        //outputStream.flush();
        Thread.sleep(100);
        //assertTrue(byteArrayOutputStream.toString().contains("Number cannot be higher than 42"));

         aeshConsole.stop();
    }

    @Test
    public void testMultipleOptionWithProvidersValidators() throws IOException, InterruptedException {
        TestConnection connection = new TestConnection();

       CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ValCommand.class)
                .command(Val2Command.class)
                .command(IntCommand.class)
                .create();

        Settings settings = new SettingsBuilder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .validatorInvocationProvider(new TestValidatorInvocationProvider())
                .create();

        ReadlineConsole console = new ReadlineConsole(settings);

        console.start();

        connection.read("val2 --foo yay"+ Config.getLineSeparator());
        //outputStream.flush();
        Thread.sleep(100);
        //assertFalse(byteArrayOutputStream.toString().contains("Option value cannot"));

        connection.read("val2 --foo Doh" + Config.getLineSeparator());
        //outputStream.flush();
        Thread.sleep(100);
        //assertTrue(byteArrayOutputStream.toString().contains("NO UPPER"));

        connection.read("val --foo yay\\ nay" + Config.getLineSeparator());
        //outputStream.flush();
        Thread.sleep(100);
        //assertTrue(byteArrayOutputStream.toString().contains("Option value cannot"));

        connection.read("int --num 43" + Config.getLineSeparator());
        //outputStream.flush();
        Thread.sleep(100);
        //assertTrue(byteArrayOutputStream.toString().contains("Number cannot be higher than 42"));

         console.stop();
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

    public static class TestValidatorInvocationProvider implements ValidatorInvocationProvider<ValidatorInvocation<String, Command>> {
        @Override
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

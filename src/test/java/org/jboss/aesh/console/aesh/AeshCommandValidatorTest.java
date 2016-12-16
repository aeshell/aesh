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
package org.jboss.aesh.console.aesh;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.validator.CommandValidator;
import org.jboss.aesh.cl.validator.CommandValidatorException;
import org.jboss.aesh.console.command.completer.CompleterInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.readline.ReadlineConsole;
import org.jboss.aesh.tty.TestConnection;
import org.aesh.util.Config;
import org.junit.Test;

import java.io.IOException;
import org.jboss.aesh.console.command.CommandException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandValidatorTest {

    @Test
    public void testCommandValidator() throws IOException, InterruptedException {
        TestConnection connection = new TestConnection();

       CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(FooCommand.class)
                .create();

        Settings settings = new SettingsBuilder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .create();

        ReadlineConsole console = new ReadlineConsole(settings);

        console.start();
        connection.read("foo -l 12 -h 20"+ Config.getLineSeparator());
        Thread.sleep(100);

        connection.assertBufferEndsWith("Sum of high and low must be over 42!"+Config.getLineSeparator());

        console.stop();
    }

    @Test
    public void testGroupCommandValidator() throws IOException, InterruptedException {
        TestConnection connection = new TestConnection();

       CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(GitCommand.class)
                .create();

         Settings settings = new SettingsBuilder()
                 .connection(connection)
                 .commandRegistry(registry)
                 .logging(true)
                 .create();

        ReadlineConsole console = new ReadlineConsole(settings);

        console.start();
        connection.read("git commit"+ Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("Either all or message must be set"+Config.getLineSeparator());

        console.stop();
    }

    @CommandDefinition(name = "foo", description = "blah", validator = FooCommandValidator.class)
    public static class FooCommand implements Command {

        @Option(shortName = 'l')
        private int low;
        @Option(shortName = 'h')
        private int high;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.println("you got foooed");
            return CommandResult.SUCCESS;
        }
    }

    public static class FooCommandValidator implements CommandValidator<FooCommand> {
        @Override
        public void validate(FooCommand command) throws CommandValidatorException {
            if(command.low + command.high < 42)
                throw new CommandValidatorException("Sum of high and low must be over 42!");
        }
    }


    @GroupCommandDefinition(name = "git", description = "", groupCommands = {GitCommit.class, GitRebase.class})
    public static class GitCommand implements Command {

        @Option(hasValue = false)
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "commit", description = "", validator = GitCommitValidator.class)
    public static class GitCommit implements Command {

        @Option(shortName = 'a', hasValue = false)
        private boolean all;

        @Option(shortName = 'm')
        private String message;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    public class GitCommitValidator implements CommandValidator<GitCommit> {
        @Override
        public void validate(GitCommit command) throws CommandValidatorException {
            if(command.all == false && command.message == null)
                throw new CommandValidatorException("Either all or message must be set");
        }
    }

    @CommandDefinition(name = "rebase", description = "")
    public static class GitRebase implements Command {

        @Option(hasValue = false)
        private boolean force;

        @Option(completer = RebaseTestCompleter.class, activator = RebaseTestActivator.class)
        private String test;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            assertTrue(force);
            return CommandResult.SUCCESS;
        }

        public class RebaseTestCompleter implements OptionCompleter {

            @Override
            public void complete(CompleterInvocation completerInvocation) {
                assertEquals(true, ((GitRebase) completerInvocation.getCommand()).force);
                completerInvocation.addCompleterValue("barFOO");
            }
        }
    }

    public static class RebaseTestActivator implements OptionActivator {
        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            ProcessedOption option = processedCommand.findLongOption("force");
            return option != null && option.getValue() != null && option.getValue().equals("true");
        }
    }
}

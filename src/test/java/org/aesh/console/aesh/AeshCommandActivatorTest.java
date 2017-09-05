/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.aesh.console.aesh;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.readline.Prompt;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.aesh.utils.Config;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jdenise@redhat.com
 */
public class AeshCommandActivatorTest {

    @Test
    public void testActivatorFail() throws Exception {
        TestConnection connection = new TestConnection(false);

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(FooCommand.class)
                .create();

        Settings settings = SettingsBuilder.builder()
                .logging(true)
                .connection(connection)
                .commandRegistry(registry)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);

        console.setPrompt(new Prompt(""));
        console.start();

        connection.read("foo" + Config.getLineSeparator());
        Thread.sleep(200);
        Assert.assertTrue(connection.getOutputBuffer(),
                connection.getOutputBuffer().
                contains("The command is not available in the current context."));
    }

    public static class NotActived implements CommandActivator {

        @Override
        public boolean isActivated(ProcessedCommand command) {
            return false;
        }

    }
    @CommandDefinition(name = "foo", description = "", activator = NotActived.class)
    public static class FooCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }
}

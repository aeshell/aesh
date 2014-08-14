/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.aesh;

import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandDynamicTest {

    @Test
    public void testDynamic() throws Exception {
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
                //.command()
                .create();

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();



        aeshConsole.start();

        aeshConsole.stop();
    }

    private ProcessedCommand createGroupCommand() {
        return null;
        /*
        return new ProcessedCommandBuilder()
                .name("group")
                .description("")
                */
    }


    public class GroupCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    public class Child1Command implements Command<CommandInvocation> {

        private String foo;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }


    public class CommandAdapter implements Command<CommandInvocation> {

        private String name;

        private HashMap<String, String> fields;

        public CommandAdapter(String name, HashMap<String, String> fields) {
            this.name = name;
            this.fields = fields;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {

            StringBuilder builder = new StringBuilder();
            commandInvocation.getShell().out().println("creating data packet we're sending over the wire:");
            for(String key : fields.keySet()) {
                if(fields.get(key) != null) {
                    if(builder.length() > 0)
                        builder.append(Config.getLineSeparator());
                    builder.append(key).append(": ").append(fields.get(key));
                }
            }

            commandInvocation.getShell().out().println("Sending: " + builder.toString());
            return CommandResult.SUCCESS;
        }

        public String getField(String fieldName) {
            return fields.get(fieldName);
        }

        public String getName() {
            return name;
        }

        public void clearValues() {
            for(String key : fields.keySet())
                fields.put(key, null);
        }
    }

}

/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.aesh;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.builder.CommandBuilder;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.result.ResultHandler;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.CommandNotFoundHandler;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshScriptTest {

    @Test
    public void scriptPoc() throws IOException, CommandLineParserException, InterruptedException {

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

          Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .logging(true)
                .create();


        CommandResultHandler resultHandler = new CommandResultHandler();
        ProcessedCommand fooCommand = new CommandBuilder()
                .name("foo")
                .resultHandler(resultHandler)
                .generateCommand();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(fooCommand, FooCommand.class)
                .create();

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .commandNotFoundHandler(resultHandler)
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();

        aeshConsole.start();

        List<String> script = readScriptFile();

        for(String line : script) {
            if(resultHandler.failed)
                break;

            outputStream.write(line.getBytes());
            outputStream.write(Config.getLineSeparator().getBytes());
            outputStream.flush();
            Thread.sleep(80);
        }

        aeshConsole.stop();
    }

    private List<String> readScriptFile() throws IOException {
        List<String> lines = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader("src/test/resources/script1"));
        String line = br.readLine();
        while (line != null) {
            if (line.trim().length() > 0 && !line.trim().startsWith("#"))
                lines.add(line);
            line = br.readLine();
        }

        return lines;
    }

    @CommandDefinition(name = "foo", description = "")
    private static class FooCommand implements Command {

        private int counter = 0;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            if(counter < 1) {
                commandInvocation.getShell().out().println("computing...." + Config.getLineSeparator() + "finished computing, returning...");
                counter ++;
                return CommandResult.SUCCESS;
            }
            else {
                assertTrue(false);
                return CommandResult.FAILURE;
            }
        }
    }

    private static class CommandResultHandler implements ResultHandler, CommandNotFoundHandler {

        private boolean failed = false;
        private String failedString;

        @Override
        public void onSuccess() {

        }

        @Override
        public void onFailure(CommandResult result) {
            failed = true;
        }

        @Override
        public void onValidationFailure(CommandResult result, Exception exception) {
            failed = true;
            failedString = exception.toString();
        }

        public void setFailed(boolean f) {
            failed = f;
        }

        public boolean hasFailed() {
            return failed;
        }

        public String getFailedReason() {
            return failedString;
        }

        @Override
        public void handleCommandNotFound(String line, Shell shell) {
            failed = true;
            failedString = line;
        }
    }

}

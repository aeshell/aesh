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
package org.jboss.aesh.readline;

import org.jboss.aesh.AeshCommandResolver;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.validator.CommandValidatorException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.AeshInvocationProviders;
import org.jboss.aesh.console.CommandResolver;
import org.jboss.aesh.console.InvocationProviders;
import org.jboss.aesh.console.Shell;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.container.CommandContainerResult;
import org.jboss.aesh.console.command.invocation.AeshCommandInvocation;
import org.jboss.aesh.console.command.invocation.CommandInvocationServices;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.registry.MutableCommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.parser.AeshLine;
import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.readline.completion.Completion;
import org.jboss.aesh.readline.editing.EditMode;
import org.jboss.aesh.readline.editing.EditModeBuilder;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.formatting.Color;
import org.jboss.aesh.terminal.formatting.TerminalColor;
import org.jboss.aesh.terminal.formatting.TerminalString;
import org.jboss.aesh.terminal.utils.InfoCmp;
import org.jboss.aesh.tty.Connection;
import org.jboss.aesh.tty.Signal;
import org.jboss.aesh.tty.Size;
import org.jboss.aesh.tty.terminal.TerminalConnection;
import org.jboss.aesh.util.LoggerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href=mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ReadlineConsole {

    private Settings settings;
    private Prompt prompt;
    private List<Completion> completions;
    private TerminalConnection connection;
    private CommandResolver commandResolver;
    private AeshContext context;
    private Readline readline;
    private InvocationProviders invocationProviders;

    private final String commandInvocationProvider = CommandInvocationServices.DEFAULT_PROVIDER_NAME;
    private static final Pattern splitter = Pattern.compile("\\w+");
    private static final Logger LOGGER = LoggerUtil.getLogger(ReadlineConsole.class.getName());

    private volatile boolean running = false;

    public ReadlineConsole(Settings settings) {
        this.settings = settings;
        commandResolver = new AeshCommandResolver((MutableCommandRegistry) settings.commandRegistry());

        invocationProviders = new AeshInvocationProviders(settings);
    }

    public void start() {
        connection = new TerminalConnection();
        init(connection);
    }

    public void stop() {
        running = false;
    }

    public void init(final Connection conn) {
        readline = new Readline(EditModeBuilder.builder(EditMode.Mode.VI).create());
        running = true;
        read(conn, readline);
    }

    /**
     * Use {@link Readline} to startBlockingReader a user input and then process it
     *
     * @param conn the tty connection
     * @param readline the readline object
     */
    public void read(final Connection conn, final Readline readline) {
        // Just call readline and get a callback when line is startBlockingReader
        if(!running)
            return;

        readline.readline(conn, prompt, line -> {

            // Ctrl-D
            if (line == null) {
                //((TerminalConnection) conn).stop();
                conn.write("logout\n").close();
                return;
            }

            LOGGER.info("got: " + line);

            //change this and have an api work similar to:
            try( CommandContainer container = commandResolver.resolveCommand(line)) {

                /*
                if (cmd.equals("exit")) {
                    conn.write("exiting...\n").close();
                    return;
                }

                // Gather args
                List<String> args = new ArrayList<>();
                while (matcher.find()) {
                    args.add(matcher.group());
                }
                */

                try {
                    new Process(conn, readline, container, line).start();
                    return;
                }
                catch (IllegalArgumentException e) {
                    conn.write(line + ": command not found\n");
                }
            }
            catch(CommandNotFoundException cnfe) {
                conn.write(cnfe.getMessage());
                read(conn, readline);
            }
            catch (Exception e) {
                e.printStackTrace();
                read(conn, readline);
            }
        }, completions);
    }


    public Prompt getPrompt() {
        return prompt;
    }

    public void setPrompt(Prompt prompt) {
        this.prompt = prompt;
    }

    public AeshContext getAeshContext() {
        return context;
    }

    public String getHelpInfo(String commandName) {
        try (CommandContainer commandContainer = commandResolver.resolveCommand(commandName)) {
            if (commandContainer != null) {
                return commandContainer.printHelp(commandName);
            }
        } catch (Exception e) { // ignored
        }
        return "";
    }

    public void addCompletion(Completion completion) {
        if(completions == null)
            completions = new ArrayList<>();
        completions.add(completion);
    }

    public void addCompletions(List<Completion> completions) {
        if(this.completions == null)
            this.completions = new ArrayList<>();
        this.completions.addAll(completions);
    }

    class Process extends Thread implements Consumer<Signal> {

        final Connection conn;
        final Readline readline;
        final CommandContainer container;
        final String line;
        volatile boolean running;

        public Process(Connection conn, Readline readline, CommandContainer container, String line) {
            this.conn = conn;
            this.readline = readline;
            this.container = container;
            this.line = line;
        }

        @Override
        public void accept(Signal signal) {
            switch (signal) {
                case INT:
                    if (running) {
                        // Ctrl-C interrupt : we use Thread interrupts to signal the command to stop
                        LOGGER.info("got interrupted in Task");
                        interrupt();
                    }
            }
        }

        @Override
        public void run() {
            // Subscribe to events, in particular Ctrl-C
            conn.setSignalHandler(this);
            running = true;
            try {
                runCommand(container, line);
                //command.execute(conn, args);
                //command.execute(null);
            }
            catch (InterruptedException e) {
                // Ctlr-C interrupt
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                running = false;
                conn.setSignalHandler(null);

                LOGGER.info("trying to read again.");
                // Readline again
                read(conn, readline);
            }
        }

        private void runCommand(CommandContainer container, String aeshLine) throws InterruptedException, OptionValidatorException, CommandException, CommandLineParserException, CommandValidatorException {
            CommandContainerResult ccResult =
                    container.executeCommand(Parser.findAllWords(aeshLine), invocationProviders, getAeshContext(),
                            settings.commandInvocationServices().getCommandInvocationProvider(
                                    commandInvocationProvider).enhanceCommandInvocation(
                                    new AeshCommandInvocation(null, new ShellImpl(connection, readline)
                                    )));
        }
    }

    class ShellImpl implements Shell {

        private TerminalConnection connection;
        private Readline readline;
        private boolean mainBuffer = true;

        public ShellImpl(TerminalConnection connection, Readline readline) {
            this.connection = connection;
            this.readline = readline;
        }

        @Override
        public void write(String out) {
            connection.write(out);
        }

        @Override
        public void write(int[] out) {
            connection.stdoutHandler().accept(out);
        }

        @Override
        public String readLine() throws InterruptedException {
            return readLine(new Prompt(""));
        }

        @Override
        public String readLine(Prompt prompt) throws InterruptedException {
            final String[] out = {null};
            CountDownLatch latch = new CountDownLatch(1);
            readline.readline(connection, prompt, event -> {
                out[0] = event;
                latch.countDown();
            });
            try {
                // Wait until interrupted
                latch.await();
            }
            finally {
                connection.setStdinHandler(null);
            }
            return out[0];
        }

        @Override
        public Key read() {
            //TODO
            return null;
        }

        @Override
        public Key read(Prompt prompt) throws InterruptedException {
            //TODO
            return null;
        }

        @Override
        public void enableAlternateBuffer() {
            if(mainBuffer && connection.getTerminal().puts(InfoCmp.Capability.enter_ca_mode)) {
                connection.getTerminal().flush();
                mainBuffer = false;
            }
        }

        @Override
        public void enableMainBuffer() {
            if (!mainBuffer && connection.getTerminal().puts(InfoCmp.Capability.exit_ca_mode)) {
                connection.getTerminal().flush();
                mainBuffer = true;
            }
        }

        @Override
        public Size size() {
            return connection.size();
        }
    }

}

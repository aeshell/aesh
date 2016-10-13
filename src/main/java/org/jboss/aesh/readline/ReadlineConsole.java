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
import org.jboss.aesh.cl.parser.CommandLineCompletionParser;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.parser.ParsedCompleteObject;
import org.jboss.aesh.cl.validator.CommandValidatorException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.complete.AeshCompleteOperation;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.AeshInvocationProviders;
import org.jboss.aesh.console.CommandResolver;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.InvocationProviders;
import org.jboss.aesh.console.Shell;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.container.CommandContainerResult;
import org.jboss.aesh.console.command.invocation.AeshCommandInvocation;
import org.jboss.aesh.console.command.invocation.CommandInvocationServices;
import org.jboss.aesh.console.command.registry.MutableCommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.parser.AeshLine;
import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.readline.action.ActionDecoder;
import org.jboss.aesh.readline.completion.CompleteOperation;
import org.jboss.aesh.readline.completion.Completion;
import org.jboss.aesh.readline.editing.EditMode;
import org.jboss.aesh.readline.editing.EditModeBuilder;
import org.jboss.aesh.terminal.Key;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author <a href=mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ReadlineConsole implements Console {

    private Settings settings;
    private Prompt prompt;
    private List<Completion> completions;
    private TerminalConnection connection;
    private CommandResolver commandResolver;
    private AeshContext context;
    private Readline readline;
    private InvocationProviders invocationProviders;

    private final String commandInvocationProvider = CommandInvocationServices.DEFAULT_PROVIDER_NAME;
    private static final Logger LOGGER = LoggerUtil.getLogger(ReadlineConsole.class.getName());

    private volatile boolean running = false;

    public ReadlineConsole(Settings settings) {
        LoggerUtil.doLog();
        this.settings = settings;
        commandResolver = new AeshCommandResolver(settings.commandRegistry());

        invocationProviders = new AeshInvocationProviders(settings);
        completions = new ArrayList<>();
        addCompletion(new AeshCompletion());
    }

    public void start() {
        connection = new TerminalConnection();
        init();
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean running() {
        return running;
    }

    private void init() {
        readline = new Readline(EditModeBuilder.builder(EditMode.Mode.VI).create());
        running = true;
        read(connection, readline);

        connection.startBlockingReader();
    }

    /**
     * Use {@link Readline} to startBlockingReader a user input and then process it
     *
     * @param conn the tty connection
     * @param readline the readline object
     */
    public void read(final Connection conn, final Readline readline) {
        // Just call readline and get a callback when line is startBlockingReader
        if(!running) {
            LOGGER.info("not running, returning");
            conn.close();
            return;
        }

        if(connection.suspended())
            connection.startReading();

        LOGGER.info("calling readline.readline");
        readline.readline(conn, prompt, line -> {
            // Ctrl-D
            if (line == null) {
                //((TerminalConnection) conn).stop();
                conn.write("logout\n").close();
                return;
            }
            LOGGER.info("got: " + line);

            if(line.trim().length() > 0)
                processLine(line, conn);
            //else line is empty
            else
               read(conn, readline);
        }, completions);
        LOGGER.info("end of read()");
    }

    private void processLine(String line, Connection conn) {
        try (CommandContainer container = commandResolver.resolveCommand(line)) {
            try {
                connection.suspendReading();
                new Process(conn, this, readline, container, line).start();
            }
            catch (IllegalArgumentException e) {
                conn.write(line + ": command not found\n");
            }
        }
        catch (CommandNotFoundException cnfe) {
            conn.write(cnfe.getMessage()+Config.getLineSeparator());
            read(conn, readline);
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Got exception while starting new process", e);
            read(conn, readline);
        }
    }

    @Override
    public Prompt prompt() {
        return prompt;
    }

    @Override
    public void setPrompt(Prompt prompt) {
        this.prompt = prompt;
    }

    @Override
    public AeshContext context() {
        return context;
    }

    @Override
    public String helpInfo(String commandName) {
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
        final Console console;
        volatile boolean running;

        public Process(Connection conn, Console console, Readline readline,
                       CommandContainer container, String line) {
            this.conn = conn;
            this.console = console;
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
                conn.setStdinHandler(null);

                if(console.running()) {
                    LOGGER.info("trying to read again.");
                    // Readline again
                    read(conn, readline);
                }
                else {
                    conn.close();
                    LOGGER.info("we're exiting...");
                }
            }
        }

        private void runCommand(CommandContainer container, String aeshLine) throws InterruptedException, OptionValidatorException, CommandException, CommandLineParserException, CommandValidatorException {
            CommandContainerResult ccResult =
                    container.executeCommand(Parser.findAllWords(aeshLine), invocationProviders, context(),
                            settings.commandInvocationServices().getCommandInvocationProvider(
                                    commandInvocationProvider).enhanceCommandInvocation(
                                    new AeshCommandInvocation(console, new ShellImpl(connection, readline)
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
        public void writeln(String out) {
            connection.write(out+ Config.getLineSeparator());
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
                connection.suspendReading();
            });
            connection.startReading();
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
        public Key read() throws InterruptedException {
            ActionDecoder decoder = new ActionDecoder(settings.editMode());
            final Key[] key = {null};
            CountDownLatch latch = new CountDownLatch(1);
            connection.setStdinHandler(keys -> {
                decoder.add(keys);
                if(decoder.hasNext()) {
                    key[0] = Key.findStartKey( decoder.next().buffer().array());
                    latch.countDown();
                    connection.suspendReading();
                }
            });
            connection.startReading();
            try {
                // Wait until interrupted
                latch.await();
            }
            finally {
                connection.setStdinHandler(null);
            }
            return key[0];
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

        @Override
        public void clear() {
            connection.getTerminal().puts(InfoCmp.Capability.clear_screen);
        }
    }

    class AeshCompletion implements Completion {

        @Override
        public void complete(CompleteOperation completeOperation) {
            completeCommandName(completeOperation);
            if (completeOperation.getCompletionCandidates().size() < 1) {

                try (CommandContainer commandContainer = commandResolver.resolveCommand( completeOperation.getBuffer())) {

                    CommandLineCompletionParser completionParser = commandContainer
                            .getParser().getCompletionParser();

                    ParsedCompleteObject completeObject = completionParser
                            .findCompleteObject(completeOperation.getBuffer(),
                                    completeOperation.getCursor());
                    completeObject.getCompletionParser().injectValuesAndComplete(completeObject,
                            new AeshCompleteOperation(context, completeOperation.getBuffer(), completeOperation.getCursor()),
                            invocationProviders);
                }
                catch (CommandLineParserException e) {
                    LOGGER.warning(e.getMessage());
                }
                catch (CommandNotFoundException ignored) {
                }
                catch (Exception ex) {
                    LOGGER.log(Level.SEVERE,
                            "Runtime exception when completing: "
                                    + completeOperation, ex);
                }
            }
        }

        private void completeCommandName(CompleteOperation co) {
            commandResolver.getRegistry().completeCommandName(co);
        /* TODO
        if(internalRegistry != null) {
            for (String internalCommand : internalRegistry.getAllCommandNames())
                if (internalCommand.startsWith(co.getBuffer()))
                    co.addCompletionCandidate(internalCommand);
        }
        */
        }

    }

}

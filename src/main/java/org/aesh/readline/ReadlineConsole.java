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
package org.aesh.readline;

import org.aesh.AeshCommandResolver;
import org.aesh.cl.internal.ProcessedCommand;
import org.aesh.cl.parser.CommandLineCompletionParser;
import org.aesh.cl.parser.CommandLineParser;
import org.aesh.cl.parser.ParsedCompleteObject;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.AeshContext;
import org.aesh.console.AeshInvocationProviders;
import org.aesh.console.CommandResolver;
import org.aesh.console.InvocationProviders;
import org.aesh.console.command.activator.AeshOptionActivatorProvider;
import org.aesh.console.command.container.CommandContainer;
import org.aesh.console.settings.Settings;
import org.aesh.cl.parser.CommandLineParserException;
import org.aesh.console.AeshCompletionHandler;
import org.aesh.console.command.CommandNotFoundException;
import org.aesh.console.settings.DefaultAeshContext;
import org.aesh.readline.completion.CompleteOperation;
import org.aesh.readline.completion.Completion;
import org.aesh.readline.editing.EditModeBuilder;
import org.aesh.readline.history.InMemoryHistory;
import org.aesh.tty.Connection;
import org.aesh.tty.terminal.TerminalConnection;
import org.aesh.util.Config;
import org.aesh.util.LoggerUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href=mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ReadlineConsole implements Console {

    private Settings settings;
    private Prompt prompt;
    private List<Completion> completions;
    private Connection connection;
    private CommandResolver commandResolver;
    private AeshContext context;
    private Readline readline;
    private InvocationProviders invocationProviders;
    private AeshCompletionHandler completionHandler;

    private static final Logger LOGGER = LoggerUtil.getLogger(ReadlineConsole.class.getName());

    private volatile boolean running = false;

    public ReadlineConsole(Settings settings) {
        LoggerUtil.doLog();
        this.settings = settings;
        commandResolver = new AeshCommandResolver(settings.commandRegistry());

        invocationProviders = new AeshInvocationProviders(settings);
        addCompletion(new AeshCompletion());
        context = new DefaultAeshContext();
        if(settings.connection() != null)
            this.connection = settings.connection();

    }

    public void start() throws IOException {
        if(connection == null)
            connection = new TerminalConnection(settings.stdIn(), settings.stdOut());
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
        completionHandler = new AeshCompletionHandler(context, connection, true);
        initializeCommands();
        if(prompt == null)
            prompt = new Prompt("");
        readline = new Readline(EditModeBuilder.builder(settings.mode()).create(), new InMemoryHistory(50),
                completionHandler);
        running = true;
        read(connection, readline);

        connection.openBlocking();
    }

    private void initializeCommands() {
        if (settings.manEnabled()) {
            //internalRegistry = new AeshInternalCommandRegistry();
            //internalRegistry.addCommand(new Man(manProvider));
        }
        try {
            for (String commandName : commandResolver.getRegistry().getAllCommandNames()) {
                ProcessedCommand cmd = commandResolver.getRegistry().getCommand(commandName, "").getParser().getProcessedCommand();
                List<CommandLineParser<?>> childParsers = commandResolver.getRegistry().getChildCommandParsers(commandName);
                if(!(invocationProviders.getOptionActivatorProvider() instanceof AeshOptionActivatorProvider)) {
                    //we have a custom OptionActivatorProvider, and need to process all options
                    cmd.updateInvocationProviders(invocationProviders);
                    for (CommandLineParser<?> child : childParsers) {
                        child.getProcessedCommand().updateInvocationProviders(invocationProviders);
                    }
                }
            }
        }
        catch (Exception e) {
            //ignored for now..
        }
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
                connection.suspend();
                new Process(conn, this, readline, container, settings, line).start();
            }
            catch (IllegalArgumentException e) {
                conn.write(line + ": command not found\n");
            }
        }
        catch (CommandNotFoundException cnfe) {
            if(settings.commandNotFoundHandler() != null) {
                settings.commandNotFoundHandler().handleCommandNotFound(line, conn);
            }
            else {
                conn.write(cnfe.getMessage() + Config.getLineSeparator());
            }
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

    class AeshCompletion implements Completion<AeshCompleteOperation> {

        @Override
        public void complete(AeshCompleteOperation completeOperation) {
            completeCommandName(completeOperation);
            if (completeOperation.getCompletionCandidates().size() < 1) {

                try (CommandContainer commandContainer = commandResolver.resolveCommand( completeOperation.getBuffer())) {

                    CommandLineCompletionParser completionParser = commandContainer
                            .getParser().getCompletionParser();

                    ParsedCompleteObject completeObject = completionParser
                            .findCompleteObject(completeOperation.getBuffer(),
                                    completeOperation.getCursor());
                    completeObject.getCompletionParser().injectValuesAndComplete(completeObject,
                            completeOperation, invocationProviders);
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

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

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandRuntime;
import org.aesh.command.Executor;
import org.aesh.command.impl.AeshCommandResolver;
import org.aesh.command.impl.invocation.AeshCommandInvocation;
import org.aesh.command.impl.invocation.AeshCommandInvocationBuilder;
import org.aesh.command.operator.OperatorType;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.AeshContext;
import org.aesh.command.container.CommandContainer;
import org.aesh.console.settings.Settings;
import org.aesh.console.AeshCompletionHandler;
import org.aesh.console.settings.DefaultAeshContext;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.readline.completion.Completion;
import org.aesh.readline.editing.EditModeBuilder;
import org.aesh.readline.history.InMemoryHistory;
import org.aesh.readline.tty.terminal.TerminalConnection;
import org.aesh.terminal.Connection;
import org.aesh.util.LoggerUtil;
import org.aesh.utils.Config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href=mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ReadlineConsole implements Console, Consumer<Connection> {

    private Settings settings;
    private Prompt prompt;
    private List<Completion> completions;
    private Connection connection;
    private AeshCommandResolver<? extends Command> commandResolver;
    private AeshContext context;
    private Readline readline;
    private AeshCompletionHandler completionHandler;
    private CommandRuntime<AeshCommandInvocation> runtime;
    private ProcessManager processManager;

    private static final Logger LOGGER = LoggerUtil.getLogger(ReadlineConsole.class.getName());

    private volatile boolean running = false;

    public ReadlineConsole(Settings settings) {
        LoggerUtil.doLog();
        if(settings == null)
            this.settings = SettingsBuilder.builder().build();
        else
            this.settings = settings;
        commandResolver = new AeshCommandResolver<>(settings.commandRegistry());

        addCompletion(new AeshCompletion());
        context = new DefaultAeshContext();
        if(settings.connection() != null)
            this.connection = settings.connection();

    }

   public void start() throws IOException {
       init();

        if(connection == null)
            new TerminalConnection(Charset.defaultCharset(), settings.stdIn(), settings.stdOut(), this);
        else
            accept(connection);
   }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean running(){
        return running;
    }


    @Override
    public void accept(Connection connection) {
        if(this.connection == null)
            this.connection = connection;

        this.runtime = generateRuntime();
        read(this.connection, readline);
        processManager = new ProcessManager(this);
        this.connection.openBlocking();
    }

    private void init() {
        completionHandler = new AeshCompletionHandler(context);
        if(prompt == null)
            prompt = new Prompt("");
        readline = new Readline(EditModeBuilder.builder(settings.mode()).create(), new InMemoryHistory(50),
                completionHandler);
        running = true;
    }

    @Override
    public void read() {
        read(connection, readline);
    }

    /**
     * Use {@link Readline} to startBlockingReader a user input and then process it
     *
     * @param conn the tty connection
     * @param readline the readline object
     */
    @Override
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
    }

    private void processLine(String line, Connection conn) {
        try {
            Executor<AeshCommandInvocation> executor = runtime.buildExecutor(line);
            processManager.execute(executor, conn);
        }
        catch (IllegalArgumentException e) {
            conn.write(line + ": command not found\n");
        }
        catch (CommandNotFoundException cnfe) {
            if(settings.commandNotFoundHandler() != null) {
                //TODO: review CommandNotFoundHandler
                settings.commandNotFoundHandler().handleCommandNotFound(line, new ShellImpl(conn, readline));
            }
            else {
                conn.write(cnfe.getMessage() + Config.getLineSeparator());
            }
            read(conn, readline);
        }
        catch (OptionValidatorException | CommandValidatorException |CommandLineParserException e) {
            conn.write(e.getMessage() + Config.getLineSeparator());
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
        if(prompt != null)
            this.prompt = prompt;
    }

    @Override
    public void setPrompt(String prompt) {
        if(prompt != null)
            this.prompt = new Prompt(prompt);
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
            runtime.complete(completeOperation);
        }

        /* TODO
        if(internalRegistry != null) {
            for (String internalCommand : internalRegistry.getAllCommandNames())
                if (internalCommand.startsWith(co.getBuffer()))
                    co.addCompletionCandidate(internalCommand);
        }
        */
    }

    private CommandRuntime generateRuntime() {
        return AeshCommandRuntimeBuilder.builder()
                .settings(settings)
                .commandInvocationBuilder(new AeshCommandInvocationBuilder(new ShellImpl(connection, readline), this))
                .operators(EnumSet.allOf(OperatorType.class))
                .build();
    }

}

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
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.alias.AeshAliasManager;
import org.aesh.command.alias.AliasCommand;
import org.aesh.command.alias.UnAliasCommand;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.export.ExportCommand;
import org.aesh.command.export.ExportCompletion;
import org.aesh.command.export.ExportManager;
import org.aesh.command.export.ExportPreProcessor;
import org.aesh.command.impl.AeshCommandResolver;
import org.aesh.command.impl.completer.AeshCompletionHandler;
import org.aesh.command.impl.invocation.AeshCommandInvocationBuilder;
import org.aesh.command.impl.registry.MutableCommandRegistryImpl;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.operator.OperatorType;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.registry.MutableCommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.io.scanner.AnnotationDetector;
import org.aesh.io.scanner.CommandDefinitionReporter;
import org.aesh.readline.alias.AliasCompletion;
import org.aesh.readline.alias.AliasManager;
import org.aesh.readline.alias.AliasPreProcessor;
import org.aesh.readline.completion.Completion;
import org.aesh.readline.editing.EditModeBuilder;
import org.aesh.readline.history.FileHistory;
import org.aesh.readline.history.History;
import org.aesh.readline.history.InMemoryHistory;
import org.aesh.readline.tty.terminal.TerminalConnection;
import org.aesh.readline.util.FileAccessPermission;
import org.aesh.readline.util.LoggerUtil;
import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.Signal;
import org.aesh.terminal.utils.Config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href=mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ReadlineConsole implements Console, Consumer<Connection> {

    private AliasManager aliasManager;
    private Settings<? extends CommandInvocation,
        ? extends ConverterInvocation, ? extends CompleterInvocation,
        ? extends ValidatorInvocation, ? extends OptionActivator,
        ? extends CommandActivator> settings;
    private Prompt prompt;
    private List<Completion> completions;
    private Connection connection;
    private AeshCommandResolver<? extends CommandInvocation> commandResolver;
    private AeshContext context;
    private Readline readline;
    private AeshCompletionHandler completionHandler;
    private CommandRuntime<? extends CommandInvocation> runtime;
    private ProcessManager processManager;
    private ExportManager exportManager;
    private static List<Function<String, Optional<String>>> preProcessors = new ArrayList<>();

    private static final Logger LOGGER = LoggerUtil.getLogger(ReadlineConsole.class.getName());

    private volatile boolean running = false;
    private History history;

    private ShellImpl shell;

   private final EnumMap<ReadlineFlag, Integer> readlineFlags = new EnumMap<>(ReadlineFlag.class);

    public ReadlineConsole(Settings<? extends CommandInvocation, ? extends ConverterInvocation,
                                           ? extends CompleterInvocation, ? extends ValidatorInvocation,
                                           ? extends OptionActivator, ? extends CommandActivator> givenSettings) {
        if(givenSettings == null)
            settings = SettingsBuilder.builder().build();
        else
            settings = givenSettings;

        if(settings.getScanForCommandPackages() == null || settings.getScanForCommandPackages().length == 0)
            commandResolver = new AeshCommandResolver<>(settings.commandRegistry());
        else
            commandResolver = getCommandResolverThroughScan();

        addCompletion(new AeshCompletion());
        if(settings.connection() != null)
            connection = settings.connection();

        //enabling export
        if(this.settings.exportEnabled()) {
            exportManager = new ExportManager(settings.exportFile(), settings.exportUsesSystemEnvironment(), settings.exportListener());
            preProcessors.add(new ExportPreProcessor(exportManager));
            completions.add(new ExportCompletion(exportManager));
            if(commandResolver.getRegistry() != null &&
                    commandResolver.getRegistry() instanceof MutableCommandRegistry) {
                try {
                    ((MutableCommandRegistry) commandResolver.getRegistry()).addCommand(new ExportCommand(exportManager));
                }
                catch (CommandRegistryException e) {
                    e.printStackTrace();
                }
            }
        }
       if(this.settings.aliasEnabled()) {
           try {
               if(this.settings.aliasManager() != null)
                   aliasManager = this.settings.aliasManager();
               else
                   aliasManager = new AeshAliasManager(settings.aliasFile(), settings.persistAlias(), commandResolver.getRegistry());
               preProcessors.add(new AliasPreProcessor(aliasManager));
               completions.add(new AliasCompletion(aliasManager, false));
               if(commandResolver.getRegistry() != null &&
                          commandResolver.getRegistry() instanceof MutableCommandRegistry) {
                   try {
                       ((MutableCommandRegistry) commandResolver.getRegistry()).addCommand(new AliasCommand(aliasManager));
                       ((MutableCommandRegistry) commandResolver.getRegistry()).addCommand(new UnAliasCommand(aliasManager));
                   }
                   catch (CommandRegistryException e) {
                       e.printStackTrace();
                   }
               }
           }
           catch(IOException e) {
                e.printStackTrace();
            }
        }

        if (!this.settings.isRedrawPromptOnInterrupt()) {
            readlineFlags.put(ReadlineFlag.NO_PROMPT_REDRAW_ON_INTR, Integer.MAX_VALUE);
        }

        context = new DefaultAeshContext(exportManager);

    }

    public void start() throws IOException {
       init();

        if(connection == null)
            new TerminalConnection(Charset.defaultCharset(), settings.stdIn(), settings.stdOut(), this);
        else
            accept(connection);
   }

    private void doStop(boolean closeConnection) {
        if (running) {
            running = false;

            if(settings.connectionClosedHandler() != null) {
                connection.setCloseHandler(c -> {
                    settings.connectionClosedHandler().accept(null);
                });
            }

            if (history != null) {
                history.stop();
            }
            if (connection != null && closeConnection) {
                connection.close();
            }
            if(aliasManager != null && settings.persistAlias())
                aliasManager.persist();
        }
    }

    @Override
    public void stop() {
        doStop(true);
    }

    @Override
    public boolean running(){
        return running;
    }


    @Override
    public void accept(Connection connection) {
        if(this.connection == null)
            this.connection = connection;

        connection.setCloseHandler((Void t) -> {
            doStop(false);
        });
        if(!settings.isEchoCtrl()) {
            // Do not display ^C
            Attributes attr = connection.getAttributes();
            attr.setLocalFlag(Attributes.LocalFlag.ECHOCTL, false);
            connection.setAttributes(attr);
        }
        if(settings.getInterruptHandler() != null) {
            connection.setSignalHandler((Signal t) -> {
                settings.getInterruptHandler().accept(null);
            });
        }

        this.runtime = generateRuntime();
        read(this.connection, readline);
        processManager = new ProcessManager(this);
        this.connection.openBlocking();
    }

    private void init() {
        completionHandler = new AeshCompletionHandler(context);
        if(prompt == null)
            prompt = new Prompt("");
        if (settings.historyPersistent()) {
            history = new FileHistory(settings.historyFile(), settings.historySize(),
                    buildPermission(settings.historyFilePermission()), settings.logging());
        } else {
            history = new InMemoryHistory(settings.historySize());
        }
        readline = new Readline(EditModeBuilder.builder(settings.mode()).create(), history,
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
        // In case there is some collected ouput from previous command execution
        shell.printCollectedOutput();

        if(running) {
            readline.readline(conn, prompt, line -> {
                if (line != null && line.trim().length() > 0) {
                    shell.startCollectOutput();
                    processLine(line, conn);
                }
                else
                    read(conn, readline);
            }, completions, preProcessors, history, null, readlineFlags);
        }
        // Just call readline and get a callback when line is startBlockingReader
        else {
            if(settings.logging())
                LOGGER.info("not running, returning");
            conn.close();
            if (settings.quitHandler() != null)
                settings.quitHandler().quit();
        }
     }

    private void processLine(String line, Connection conn) {
        try {
            Executor<? extends CommandInvocation> executor = runtime.buildExecutor(line);
            processManager.execute(executor, conn);
        }
        catch (CommandNotFoundException cnfe) {
            if(settings.commandNotFoundHandler() != null) {
                //TODO: review CommandNotFoundHandler
                settings.commandNotFoundHandler().handleCommandNotFound(line, new ShellImpl(conn));
            }
            else {
                conn.write(cnfe.getMessage() + Config.getLineSeparator());
            }
            read(conn, readline);
        }
        catch (IllegalArgumentException | OptionValidatorException |
                CommandValidatorException | CommandLineParserException e) {
            conn.write(e.getMessage()+Config.getLineSeparator());
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

    private FileAccessPermission buildPermission(org.aesh.command.settings.FileAccessPermission historyFilePermission) {
        if (historyFilePermission == null) {
            return null;
        }
        FileAccessPermission perm = new FileAccessPermission();
        perm.setExecutable(historyFilePermission.isExecutable());
        perm.setExecutableOwnerOnly(historyFilePermission.isExecutableOwnerOnly());
        perm.setReadable(historyFilePermission.isReadable());
        perm.setReadableOwnerOnly(historyFilePermission.isReadableOwnerOnly());
        perm.setWritable(historyFilePermission.isWritable());
        perm.setWritableOwnerOnly(historyFilePermission.isWritableOwnerOnly());
        return perm;
    }

    @SuppressWarnings("unchecked")
    private AeshCommandResolver<? extends CommandInvocation> getCommandResolverThroughScan() {
        MutableCommandRegistry<CommandInvocation> registry = new MutableCommandRegistryImpl<>();

        CommandDefinitionReporter reporter = new CommandDefinitionReporter();
        AnnotationDetector detector = new AnnotationDetector(reporter);
        try {
            detector.detect(settings.getScanForCommandPackages());
            for(String command : reporter.getCommands()) {
                //registry.addCommand(Class.forName(command));
                final Class<Command> clazz = (Class<Command>) Class.forName(command);
                registry.addCommand(clazz);
            }
        }
        catch (IOException e) {
            LOGGER.log(Level.WARNING, "AnnotationDetector failed to scan for CommandDefinition annotations", e);
        }
        catch (ClassNotFoundException | CommandRegistryException e) {
            LOGGER.log(Level.WARNING, "Failed to load CommandDefinition class.", e);
        }

        return new AeshCommandResolver<>(registry);
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

    private CommandRuntime<? extends CommandInvocation> generateRuntime() {
        shell = new ShellImpl(connection, settings.enableSearchInPaging());
        return AeshCommandRuntimeBuilder.builder()
                .settings(settings)
                .commandInvocationBuilder(new AeshCommandInvocationBuilder(shell, this))
                .aeshContext(context)
                .operators(EnumSet.allOf(OperatorType.class))
                .build();
    }

}

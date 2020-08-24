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
package org.aesh;

import java.io.IOException;
import java.util.Set;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.readline.ShellImpl;
import org.aesh.readline.tty.terminal.TerminalConnection;
import org.aesh.terminal.Connection;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class AeshRuntimeRunner {

    private Class<? extends Command> command;

    private CommandRuntime runtime;

    private String[] args;
    private boolean interactive = false;

    private AeshRuntimeRunner() {
    }

    public static AeshRuntimeRunner builder() {
        return new AeshRuntimeRunner();
    }

    public AeshRuntimeRunner command(Class<? extends Command> command) {
        this.command = command;
        return this;
    }

    public AeshRuntimeRunner commandRuntime(CommandRuntime runtime) {
        this.runtime = runtime;
        return this;
    }

    public AeshRuntimeRunner interactive(boolean interactive) {
        this.interactive = interactive;
        return this;
    }

    public AeshRuntimeRunner args(String... args) {
        this.args = args;
        return this;
    }

    @SuppressWarnings("unchecked")
    public CommandResult execute() {
        Connection connection = null;
        if (command == null && runtime == null)
            throw new RuntimeException("Command needs to be added");
        try {
            if (runtime == null) {
                   AeshCommandRuntimeBuilder builder = AeshCommandRuntimeBuilder.builder();
                   if(interactive) {
                       connection = new TerminalConnection();
                       connection.openNonBlocking();
                       builder.shell(new ShellImpl(connection));
                   }
                   runtime = builder.commandRegistry(AeshCommandRegistryBuilder.builder().command(command).create()).build();
            }

            final Set<String> commandNames = runtime.getCommandRegistry().getAllCommandNames();
            if (commandNames.isEmpty())
                throw new RuntimeException("Command needs to be added to the registry.");
            else if (commandNames.size() > 1)
                throw new RuntimeException("Only one command can be added to the registry.");

            final String commandName = commandNames.iterator().next();
            StringBuilder sb = new StringBuilder(commandName);
            if (args != null && args.length > 0) {
                sb.append(" ");
                if (args.length == 1) {
                    sb.append(args[0]);
                } else {
                    for (String arg : args) {
                        if (arg.indexOf(' ') >= 0) {
                            sb.append('"').append(arg).append("\" ");
                        } else {
                            sb.append(arg).append(' ');
                        }
                    }
                }
            }

            CommandResult result = null;
            try {
                result = runtime.executeCommand(sb.toString());
            } catch (CommandNotFoundException e) {
                System.err.println("Command not found: " + sb.toString());
            } catch (CommandException | CommandLineParserException | CommandValidatorException | OptionValidatorException e) {
                showHelpIfNeeded(runtime, commandName, e);
            } catch (InterruptedException | IOException e) {
                System.err.println(e.getMessage());
            }
            if(connection != null)
                connection.close();

            return result;
        } catch (CommandRegistryException | IOException e) {
            throw new RuntimeException("Exception while executing command: " + e.getMessage());
        }
    }

    private static void showHelpIfNeeded(CommandRuntime runtime, String commandName, Exception e) {
        if (e != null) {
            System.err.println(e.getMessage());
        }
        System.err.println(runtime.commandInfo(commandName));
    }
}

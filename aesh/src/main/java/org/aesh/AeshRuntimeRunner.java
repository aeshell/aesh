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

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;

import java.io.IOException;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class AeshRuntimeRunner {

    private Class<? extends Command> command;
    private CommandRuntime runtime;
    private String[] args;

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

    public AeshRuntimeRunner args(String[] args) {
        this.args = args;
        return this;
    }

    public void execute() {
        if(command == null && runtime == null)
            throw new RuntimeException("Command needs to be added");
        try {
            if (runtime == null) {
                runtime = AeshCommandRuntimeBuilder.builder().
                        commandRegistry(AeshCommandRegistryBuilder.builder().command(command).create())
                        .build();
            }
            else {
                if(runtime.getCommandRegistry().getAllCommandNames().isEmpty())
                    throw new RuntimeException("Command needs to be added to the registry.");
                else if(runtime.getCommandRegistry().getAllCommandNames().size() > 1)
                    throw new RuntimeException("Only one command can be added to the registry.");
            }

            StringBuilder sb = new StringBuilder((String) runtime.getCommandRegistry().getAllCommandNames().iterator().next());
            if (args.length > 0) {
                sb.append(" ");
                if(args.length == 1) {
                    sb.append(args[0]);
                } else {
                    for(String arg : args) {
                        if(arg.indexOf(' ') >= 0) {
                            sb.append('"').append(arg).append("\" ");
                        } else {
                            sb.append(arg).append(' ');
                        }
                    }
                }
            }

            runtime.executeCommand(sb.toString());

        }
        catch (CommandRegistryException | IOException | CommandException |
                OptionValidatorException | InterruptedException |
                CommandNotFoundException | CommandLineParserException |
                CommandValidatorException e) {
            throw new RuntimeException("Exception while executing command: "+e.getMessage());
        }
    }
}

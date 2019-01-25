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
package org.aesh.util;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.util.completer.CompleterCommand;

import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CompleterGenerator {

    public static void main(String[] args) throws CommandRegistryException {
               CommandRuntime runtime = AeshCommandRuntimeBuilder
                                         .builder()
                                         .commandRegistry(AeshCommandRegistryBuilder.builder().command(CompleterCommand.class).create())
                                         .build();

        if (args.length > 0) {
            StringBuilder sb = new StringBuilder("completer ");
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

            try {
                runtime.executeCommand(sb.toString());
            }
            catch (CommandNotFoundException e) {
                System.err.println("Command not found: " + sb.toString());
            }
            catch (CommandException | CommandLineParserException | CommandValidatorException | OptionValidatorException | InterruptedException | IOException e) {
                System.err.println(e.getMessage());
            }
        }
        else {
            System.err.println(runtime.commandInfo("completer"));
        }
    }

    }

}

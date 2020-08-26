/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
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
package org.aesh.command;

import java.io.IOException;
import org.aesh.command.invocation.CommandInvocationBuilder;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.readline.AeshContext;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.readline.util.Parser;

/**
 * An Aesh Command processor.
 *
 * @author jdenise@redhat.com
 */
public interface CommandRuntime<CI extends CommandInvocation> {

    /**
     * The registry in which commands are registered.
     *
     * @return
     */
    CommandRegistry<CI> getCommandRegistry();

    /**
     * Build an Executor from a command line.
     *
     * @param line
     * @return The executor.
     * @throws CommandNotFoundException
     * @throws CommandLineParserException
     * @throws OptionValidatorException
     * @throws CommandValidatorException
     * @throws java.io.IOException
     */
    Executor<CI> buildExecutor(String line) throws CommandNotFoundException,
            CommandLineParserException,
            OptionValidatorException,
            CommandValidatorException,
            IOException;

    /**
     * Execute a command line.
     *
     * @param line
     * @throws CommandNotFoundException
     * @throws CommandLineParserException
     * @throws OptionValidatorException
     * @throws CommandValidatorException
     * @throws CommandException
     * @throws java.lang.InterruptedException
     * @throws java.io.IOException
     */
    CommandResult executeCommand(String line) throws CommandNotFoundException,
            CommandLineParserException,
            OptionValidatorException,
            CommandValidatorException,
            CommandException,
            InterruptedException,
            IOException;

    /**
     * Execute multiple lines sequentially.
     * If a line return CommandResult.FAILURE it will ignore the remaining lines.
     *
     * @param lines
     * @throws CommandNotFoundException
     * @throws CommandLineParserException
     * @throws OptionValidatorException
     * @throws CommandValidatorException
     * @throws CommandException
     * @throws java.lang.InterruptedException
     * @throws java.io.IOException
     */
    CommandResult executeCommand(String... lines) throws CommandNotFoundException,
            CommandLineParserException,
            OptionValidatorException,
            CommandValidatorException,
            CommandException,
            InterruptedException,
            IOException;

    /**
     * Returns the aesh context.
     *
     * @return The aesh context.
     */
    AeshContext getAeshContext();

    /**
     *
     * @param line input line
     * @return condensed information regarding the specific command
     */
    default String commandInfo(String line) {
        try {
            String name = Parser.findFirstWord(line);
            return getCommandRegistry().getCommand(name, line).printHelp(line);
        }
        catch (CommandNotFoundException e) {
            return null;
        }
    }

    CommandInvocationBuilder<CI> commandInvocationBuilder();

    InvocationProviders invocationProviders();

    void complete(AeshCompleteOperation completeOperation);
}

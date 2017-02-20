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

import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.AeshContext;
import org.aesh.command.registry.CommandRegistry;

/**
 * An Aesh Command processor.
 *
 * @author jdenise@redhat.com
 */
public interface CommandRuntime<T extends CommandInvocation, V extends AeshCompleteOperation> {

    /**
     * The registry in which commands are registered.
     *
     * @return
     */
    CommandRegistry getCommandRegistry();

    /**
     * Build an Executor from a command line.
     *
     * @param line
     * @return The executor.
     * @throws CommandNotFoundException
     * @throws CommandLineParserException
     * @throws OptionValidatorException
     * @throws CommandValidatorException
     */
    Executor<T> buildExecutor(String line) throws CommandNotFoundException,
            CommandLineParserException,
            OptionValidatorException,
            CommandValidatorException;

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
     */
    void executeCommand(String line) throws CommandNotFoundException,
            CommandLineParserException,
            OptionValidatorException,
            CommandValidatorException,
            CommandException,
            InterruptedException;

    /**
     * Complete a command.
     *
     * @param op The command to complete.
     */
    void complete(V op);

    /**
     * Returns the aesh context.
     *
     * @return The aesh context.
     */
    AeshContext getAeshContext();

}

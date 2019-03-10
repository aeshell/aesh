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
package org.aesh.command.container;

import org.aesh.command.Command;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.readline.AeshContext;
import org.aesh.command.CommandException;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.parser.ParsedLine;

/**
 * A CommandContainer hold reference to the Command and
 * the CommandLineParser generated from the Command.
 *
 * CommandRegistry will not put any CommandContainer objects in the registry
 * if it have any build errors.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CommandContainer<CI extends CommandInvocation> extends AutoCloseable {

    /**
     * @return parser generated from Command
     */
    CommandLineParser<CI> getParser();

    /**
     * @return true if the CommandLineParser or Command generation generated any errors
     */
    boolean haveBuildError();

    /**
     * @param childCommandName (for group commands)
     * @return help info
     */
    String printHelp(String childCommandName);

    /**
     * @return error message
     */
    String getBuildErrorMessage();

    ParsedLine pollLine();

    void emptyLine();

    ProcessedCommand<Command<CI>, CI> parseAndPopulate(InvocationProviders invocationProviders,
                                                       AeshContext aeshContext)
            throws CommandLineParserException, OptionValidatorException;

    CommandContainerResult executeCommand(ParsedLine line, InvocationProviders invocationProviders,
                                          AeshContext aeshContext,
                                          CI commandInvocation)
            throws CommandLineParserException, OptionValidatorException,
            CommandValidatorException, CommandException, InterruptedException;

    void addLine(ParsedLine aeshLine);
}

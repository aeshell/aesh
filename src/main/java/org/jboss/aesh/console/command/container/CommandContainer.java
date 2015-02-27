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
package org.jboss.aesh.console.command.container;

import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.validator.CommandValidatorException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.InvocationProviders;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.parser.AeshLine;

import java.io.IOException;
import java.util.List;

/**
 * A CommandContainer hold reference to the Command and
 * the CommandLineParser generated from the Command.
 *
 * CommandRegistry will not put any CommandContainer objects in the registry
 * if it have any create errors.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 * @author <a href="mailto:danielsoro@gmail.com">Daniel Cunha (soro)</a>
 */
public interface CommandContainer<T extends Command> extends AutoCloseable {

    /**
     * @return parser generated from Command
     */
    CommandLineParser<T> getParser();

    /**
     * @return true if the CommandLineParser or Command generation generated any errors
     */
    boolean haveBuildError();

    /**
     * @return error message
     */
    String getBuildErrorMessage();

    CommandContainerResult executeCommand(AeshLine line, InvocationProviders invocationProviders,
                                          AeshContext aeshContext,
                                          CommandInvocation commandInvocation) throws CommandLineParserException, OptionValidatorException, CommandValidatorException, IOException, InterruptedException;

    List<CommandLineParser<? extends Command>> getChildren();
}

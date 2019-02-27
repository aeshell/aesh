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

package org.aesh.command.populator;

import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.readline.AeshContext;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.Command;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CommandPopulator<T, CI extends CommandInvocation> {

    /**
     * Populate a Command instance with the values parsed from a command line
     * If any parser errors are detected it will throw an exception
     * @param processedCommand command line
     * @param aeshContext the context
     * @param mode based on rules given to the parser
     * @throws CommandLineParserException
     */
    void populateObject(ProcessedCommand<Command<CI>,CI> processedCommand, InvocationProviders invocationProviders,
                        AeshContext aeshContext, CommandLineParser.Mode mode) throws CommandLineParserException, OptionValidatorException;

    /**
     * @return the object instance that will be populated.
     */
    T getObject();
}

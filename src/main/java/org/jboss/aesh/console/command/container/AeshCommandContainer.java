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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.console.command.container;

import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.parser.AeshCommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.validator.CommandValidatorException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.InvocationProviders;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.parser.AeshLine;

import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandContainer implements CommandContainer {

    private CommandLineParser parser;
    private String errorMessage;

    public AeshCommandContainer(CommandLineParser parser) {
        if (parser != null && parser.getProcessedCommand() != null) {
            this.parser = parser;
        }
    }

    public AeshCommandContainer(ProcessedCommand processedCommand, Command command) {
        parser = new AeshCommandLineParser(processedCommand, command);
    }

    public AeshCommandContainer(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public CommandLineParser getParser() {
        return parser;
    }

    @Override
    public boolean haveBuildError() {
        return errorMessage != null;
    }

    @Override
    public String getBuildErrorMessage() {
        return errorMessage;
    }

    @Override
    public CommandContainerResult executeCommand(AeshLine line, InvocationProviders invocationProviders,
                                                 AeshContext aeshContext,
                                                 CommandInvocation commandInvocation)
            throws CommandLineParserException, OptionValidatorException, CommandValidatorException, IOException, InterruptedException {

        CommandLine commandLine = parser.parse(line, false);
        commandLine.getParser().getCommandPopulator().populateObject(commandLine.getParser().getCommand(), commandLine, invocationProviders, aeshContext, true);
        if(commandLine.getParser().getProcessedCommand().getValidator() != null &&
                !commandLine.hasOptionWithOverrideRequired())
            commandLine.getParser().getProcessedCommand().getValidator().validate(commandLine.getParser().getCommand());

        CommandResult result = commandLine.getParser().getCommand().execute(commandInvocation);

        return new CommandContainerResult(commandLine.getParser().getProcessedCommand().getResultHandler(), result);
    }

    @Override
    public void close() {

    }

    public void addChild(CommandContainer commandContainer) {
        ((AeshCommandLineParser) getParser()).addChildParser((AeshCommandLineParser) commandContainer.getParser());
    }

}

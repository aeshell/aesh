/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
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
            parser.getProcessedCommand().getValidator().validate(commandLine.getParser().getCommand());

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

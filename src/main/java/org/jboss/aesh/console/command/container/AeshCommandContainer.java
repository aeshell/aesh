/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.command.container;

import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.parser.AeshCommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.parser.AeshLine;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandContainer implements CommandContainer {

    private Command command;
    private CommandLineParser parser;
    private String errorMessage;
    private List<CommandContainer> childCommands;

    public AeshCommandContainer(CommandLineParser parser, Command command) {
        if (parser != null && parser.getProcessedCommand() != null) {
            this.parser = parser;
            this.command = command;
        }
    }

    public AeshCommandContainer(ProcessedCommand processedCommand,
                                Command command) {
        parser = new AeshCommandLineParser(processedCommand);
        this.command = command;
    }


    @Override
    public Command getCommand() {
        return command;
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
    public CommandContainerResult executeCommand(AeshLine line, CommandInvocation commandInvocation) {
        if(parser.getProcessedCommand().isGroupCommand()) {

        }

        return null;
    }

    @Override
    public void close() {

    }

    public void addChild(CommandContainer commandContainer) {
        if(childCommands == null)
            childCommands = new ArrayList<>();
        childCommands.add(commandContainer);
        ((AeshCommandLineParser) getParser()).addChildParser((AeshCommandLineParser) commandContainer.getParser());
    }
}

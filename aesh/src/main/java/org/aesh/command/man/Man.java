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
package org.aesh.command.man;

import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.CommandException;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.ManProvider;
import org.aesh.command.man.parser.ManFileParser;
import org.aesh.command.option.Arguments;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.terminal.utils.ANSI;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.aesh.terminal.utils.Config;

/**
 * A Man implementation for Aesh. ref: http://en.wikipedia.org/wiki/Man_page
 *
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@CommandDefinition(name = "man", description = "manuals")
public class Man extends AeshFileDisplayer {

    @Arguments(completer = ManCompleter.class)
    private final List<String> manPages;

    private final ManFileParser fileParser;
    private CommandRegistry<? extends CommandInvocation> registry;
    private final ManProvider manProvider;

    public Man(ManProvider manProvider) {
        super();
        this.manProvider = manProvider;
        manPages = new ArrayList<>();
        fileParser = new ManFileParser();
    }

    public void setRegistry(CommandRegistry<? extends CommandInvocation> registry) {
        this.registry = registry;
    }

    @Override
    public FileParser getFileParser() {
       return fileParser;
    }

    @Override
    public void displayBottom() throws IOException {
        if(getSearchStatus() == TerminalPage.Search.SEARCHING) {
            clearBottomLine();
           writeToConsole("/"+getSearchWord());
        }
        else if(getSearchStatus() == TerminalPage.Search.NOT_FOUND) {
            clearBottomLine();
            writeToConsole(ANSI.INVERT_BACKGROUND + "Pattern not found (press RETURN)" + ANSI.DEFAULT_TEXT);
        }
        else if(getSearchStatus() == TerminalPage.Search.NO_SEARCH ||
                getSearchStatus() == TerminalPage.Search.RESULT) {
            writeToConsole(ANSI.INVERT_BACKGROUND);
            writeToConsole("Manual page "+ fileParser.getName()+" line "+getTopVisibleRow()+
                    " (press h for help or q to quit)"+ ANSI.DEFAULT_TEXT);
        }
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if(manPages == null || manPages.size() == 0) {
            commandInvocation.getShell().write("What manual page do you want?"+ Config.getLineSeparator());
            return CommandResult.SUCCESS;
        }

        if(manPages.size() <= 0) {
            commandInvocation.getShell().write("No manual entry for "+manPages.get(0)+Config.getLineSeparator());
            return CommandResult.SUCCESS;
        }

        if(manProvider == null) {
            commandInvocation.getShell().write("No manual provider defined");
            return CommandResult.SUCCESS;
        }

        InputStream inputStream = manProvider.getManualDocument(manPages.get(0));
        if(inputStream != null) {
            setCommandInvocation(commandInvocation);
            try {
                fileParser.setInput(inputStream);
                afterAttach();
            } catch (IOException ex) {
                throw new CommandException(ex);
            }
        }

        return CommandResult.SUCCESS;
    }

    public class ManCompleter implements OptionCompleter {
        @Override
        public void complete(CompleterInvocation completerData) {
            List<String> completeValues = new ArrayList<>();
            if(registry != null) {
                for(String command : registry.getAllCommandNames()) {
                    if(command.startsWith(completerData.getGivenCompleteValue()))
                        completeValues.add(command);
                }
                completerData.setCompleterValues(completeValues);
            }
        }
    }

}

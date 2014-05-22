/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.man;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.console.command.completer.CompleterInvocation;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.helper.ManProvider;
import org.jboss.aesh.console.man.parser.ManFileParser;
import org.jboss.aesh.util.ANSI;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A Man implementation for Aesh. ref: http://en.wikipedia.org/wiki/Man_page
 *
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@CommandDefinition(name = "man", description = "manuals")
public class Man extends AeshFileDisplayer {

    @Arguments(completer = ManCompleter.class)
    private List<String> manPages;

    private ManFileParser fileParser;
    private static CommandRegistry registry;
    private final ManProvider manProvider;

    public Man(ManProvider manProvider) {
        super();
        this.manProvider = manProvider;
        manPages = new ArrayList<String>();
        fileParser = new ManFileParser();
    }

    public void setRegistry(CommandRegistry registry) {
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
            writeToConsole(ANSI.getInvertedBackground()+
                    "Pattern not found (press RETURN)"+
                    ANSI.defaultText());
        }
        else if(getSearchStatus() == TerminalPage.Search.NO_SEARCH ||
                getSearchStatus() == TerminalPage.Search.RESULT) {
            writeToConsole(ANSI.getInvertedBackground());
            writeToConsole("Manual page "+ fileParser.getName()+" line "+getTopVisibleRow()+
                    " (press h for help or q to quit)"+ ANSI.defaultText());
        }
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
        if(manPages == null || manPages.size() == 0) {
            commandInvocation.getShell().out().println("What manual page do you want?");
            return CommandResult.SUCCESS;
        }
        if(manPages.size() > 0) {
            InputStream inputStream = manProvider.getManualDocument(manPages.get(0));
            if(inputStream != null) {
                setCommandInvocation(commandInvocation);
                fileParser.setInput(inputStream);
                afterAttach();
            }
            else {
                commandInvocation.getShell().out().println("No manual entry for "+manPages.get(0));
                return CommandResult.SUCCESS;
            }
        }

        return CommandResult.SUCCESS;
    }

    public static class ManCompleter implements OptionCompleter {
        @Override
        public void complete(CompleterInvocation completerData) {
            List<String> completeValues = new ArrayList<String>();
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

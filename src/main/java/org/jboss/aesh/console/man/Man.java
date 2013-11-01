/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.man;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.completer.CompleterData;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.helper.ManProvider;
import org.jboss.aesh.console.man.parser.ManPageLoader;
import org.jboss.aesh.util.ANSI;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
    private ManPageLoader loader;
    private static CommandRegistry registry;
    private final ManProvider manProvider;

    public Man(ManProvider manProvider) {
        super();
        this.manProvider = manProvider;
        manPages = new ArrayList<String>();
        loader = new ManPageLoader();
    }

    public void setRegistry(CommandRegistry registry) {
        this.registry = registry;
    }

    private void setFile(String name) throws IOException {
        loader.setFile(name);
    }

    private void setFile(URL url) throws IOException {
        loader.setUrlFile(url);
    }

    private void setFile(InputStream input, String fileName) throws IOException {
        loader.setFile(input, fileName);
    }

    @Override
    public PageLoader getPageLoader() {
       return loader;
    }

    @Override
    public void displayBottom() throws IOException {
        writeToConsole(ANSI.getInvertedBackground());
        writeToConsole("Manual page "+loader.getName()+" line "+getTopVisibleRow()+
        " (press h for help or q to quit)"+ ANSI.defaultText());
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws IOException {
        setCommandInvocation(commandInvocation);
        if(manPages != null && manPages.size() > 0) {
            InputStream inputStream = manProvider.getManualDocument(manPages.get(0));
            if(inputStream != null) {
                setFile(inputStream, null);
                getCommandInvocation().attachConsoleCommand(this);
                afterAttach();
            }
        }

        return CommandResult.SUCCESS;
    }

    public static class ManCompleter implements OptionCompleter {
        @Override
        public void complete(CompleterData completerData) {
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

/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.command.container;

import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.parser.ParserGenerator;
import org.jboss.aesh.console.command.Command;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandContainerBuilder implements CommandContainerBuilder {

    @Override
    public CommandContainer build(Command command) {
        try {
            return ParserGenerator.generateCommandLineParser(command);
        }
        catch (CommandLineParserException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public CommandContainer build(Class<? extends Command> command) {
        try {
            return ParserGenerator.generateCommandLineParser(command);
        }

        catch (CommandLineParserException e) {
            e.printStackTrace();
            return null;
        }
    }
}

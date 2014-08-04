/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.parser;

import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.console.command.Command;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineParserBuilder {

    private ProcessedCommand param;
    private boolean isChild = false;
    private Command command;

    public CommandLineParserBuilder() {
    }

    public CommandLineParserBuilder child(boolean isChild) {
        this.isChild = isChild;
        return this;
    }

    public CommandLineParserBuilder processedCommand(ProcessedCommand param) {
        this.param = param;
        return this;
    }

    public CommandLineParserBuilder command(Command command) {
        this.command = command;
        return this;
    }

    public CommandLineParser create() throws IllegalArgumentException {
        return new AeshCommandLineParser( param, command, isChild);
    }

}

/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.parser;

import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.util.ReflectionUtil;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineParserBuilder {

    private ProcessedCommand param;
    private Command command;

    public CommandLineParserBuilder() {
    }

    public CommandLineParserBuilder processedCommand(ProcessedCommand param) {
        this.param = param;
        return this;
    }

    public CommandLineParserBuilder command(Command command) {
        this.command = command;
        return this;
    }

    public CommandLineParserBuilder command(Class<? extends Command> command) {
        this.command = ReflectionUtil.newInstance(command);
        return this;
    }


    public CommandLineParser create() throws IllegalArgumentException {
        return new AeshCommandLineParser( param, command);
    }

}

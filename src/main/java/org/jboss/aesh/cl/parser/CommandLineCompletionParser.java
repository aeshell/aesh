/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.parser;

import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.InvocationProviders;
import org.jboss.aesh.console.command.Command;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CommandLineCompletionParser {
    /**
     * 1. find the "word" connected with cursor
     *   if it starts with '-', we need to check if its a value or name
     * @param line buffer
     * @return ParsedCompleteObject
     */
    ParsedCompleteObject findCompleteObject(String line, int cursor) throws CommandLineParserException;

    void injectValuesAndComplete(ParsedCompleteObject completeObject, Command command,
                                 CompleteOperation completeOperation,
                                 InvocationProviders invocationProviders);
}

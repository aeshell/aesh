/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.command;

import org.jboss.aesh.console.command.invocation.CommandInvocation;

import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface Command<T extends CommandInvocation> {

    /**
     * Will be executed when this command is triggered by the command line.
     *
     * @param commandInvocation invocation
     * @return success or failure depending on how the execution went.
     * @throws IOException
     * @throws InterruptedException
     */
    CommandResult execute(T commandInvocation) throws IOException, InterruptedException;
}

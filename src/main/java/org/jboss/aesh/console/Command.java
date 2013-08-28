/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.operator.ControlOperator;

import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface Command {
    /**
     * Will be called when this command is triggered by the command line.
     *
     * @param aeshConsole the current console
     * @param operator specifies if the current command is run as a sequence
     *                 of pipelines or redirections.
     *                 Can be ignored if the command do not read from the
     *                 input stream.
     * @return success or failure depending on how the execution went.
     * @throws IOException
     */
    CommandResult execute(AeshConsole aeshConsole,
                          ControlOperator operator) throws IOException;
}

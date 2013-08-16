/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import java.io.PrintWriter;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface AeshConsole {

    void start();

    void stop();

    void addCommand(Class<? extends Command> command);

    void removeCommand(Class<? extends Command> command);

    PrintWriter out();

    PrintWriter err();

    void setPrompt(Prompt prompt);
}

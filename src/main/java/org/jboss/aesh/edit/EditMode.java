/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit;

import org.jboss.aesh.console.Console;
import org.jboss.aesh.edit.actions.Action;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public interface EditMode {

    Operation parseInput(Key input, String buffer);

    Action getCurrentAction();

    Mode getMode();

    void init(Console console);

    void setAskForCompletions(boolean askForCompletions);
}

/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.helper;

import org.jboss.aesh.console.Console;
import org.jboss.aesh.edit.actions.Action;

/**
 * InterruptHook is the handler thats called when an interrupt has occurred.
 * Its implementation need to be added to Settings.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface InterruptHook {

    void handleInterrupt(Console console, Action action);
}

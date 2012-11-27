/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.helper;

import org.jboss.aesh.console.Console;

import java.io.IOException;

/**
 * InterruptHook is the handler thats called when an interrupt has occured.
 * Its implementation need to be added to Settings.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface InterruptHook {

    public void handleInterrupt(Console console);
}

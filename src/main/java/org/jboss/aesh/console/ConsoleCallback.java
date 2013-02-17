/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

/**
 * Implementation of this interface will be called when a user press the
 * "enter/return" key.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface ConsoleCallback {

    public void readConsoleOutput(ConsoleOutput output);
}

/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.settings;

/**
 * A simple Callback thats called when the program quits
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public interface QuitHandler {
    void quit();
}

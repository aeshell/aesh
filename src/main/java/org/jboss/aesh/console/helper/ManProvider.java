/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.helper;

import java.io.InputStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface ManProvider {
    /**
     * Based on a command name, return the InputStream of
     * the man documentation for the specific command.
     * @param commandName command
     * @return stream
     */
    InputStream getManualDocument(String commandName);
}

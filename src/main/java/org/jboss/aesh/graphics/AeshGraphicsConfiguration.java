/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.graphics;

import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.terminal.TerminalSize;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshGraphicsConfiguration implements GraphicsConfiguration {

    private Shell shell;

    public AeshGraphicsConfiguration(Shell shell) {
        this.shell = shell;
    }

    @Override
    public TerminalSize getBounds() {
        return shell.getSize();
    }

    @Override
    public Graphics getGraphics() {
        return new AeshGraphics(shell, this);
    }
}

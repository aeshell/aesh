/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.graphics;

import org.jboss.aesh.terminal.Shell;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ShellFrame {

    private RootPane rootPane;

    private String title;

    private Graphics graphics;


    public ShellFrame(Shell shell) {
        graphics = new AeshGraphics(shell);
        rootPane = new RootPane("", graphics);
    }


    public ShellFrame(String title, Shell shell) {
        graphics = new AeshGraphics(shell);
        setTitle(title);
        rootPane = new RootPane(title, graphics);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void paint() {
        rootPane.paint();
    }
}

/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.aesh.console;

/**
 * @author <a href="mailto:danielsoro@gmail.com">Daniel Cunha (soro)</a>
 */
public class ProcessDelegater {

    private Console console;

    public ProcessDelegater(Console console) {
        this.console = console;
    }

    public void putProcessInBackground(int pid) {
        console.putProcessInBackground(pid);
    }

    public void putProcessInForeground(int pid) {
        console.putProcessInForeground(pid);
    }

    public void execute(String input) {
        console.pushToInputStream(input);
    }
}

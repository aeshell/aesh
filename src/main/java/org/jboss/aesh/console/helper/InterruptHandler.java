/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.helper;

import org.jboss.aesh.console.Console;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.IOException;

/**
 * A simple InterruptHandler, for now it only handles INT.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@SuppressWarnings("restriction")
public class InterruptHandler {

    private Console console;
    private InterruptHook interruptHook;

    public InterruptHandler(Console console, InterruptHook interruptHook) {
        this.interruptHook = interruptHook;
        this.console = console;
    }

    public void initInterrupt() throws IOException {
        SignalHandler handler = new SignalHandler () {
            public void handle(Signal sig) {
                interruptHook.handleInterrupt(console);
            }
        };
        Signal.handle(new Signal("INT"), handler);
    }
}

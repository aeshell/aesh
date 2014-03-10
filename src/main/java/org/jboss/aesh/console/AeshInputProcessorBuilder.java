/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.history.FileHistory;
import org.jboss.aesh.history.History;
import org.jboss.aesh.history.InMemoryHistory;

import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshInputProcessorBuilder {

    private ConsoleBuffer consoleBuffer;
    private Settings settings;
    private History history;
    private CompletionHandler completion;
    private InputProcessorInterruptHook interruptHook;

    public AeshInputProcessorBuilder() {
    }

    public AeshInputProcessorBuilder consoleBuffer(ConsoleBuffer consoleBuffer) {
        this.consoleBuffer = consoleBuffer;
        return this;
    }

    public AeshInputProcessorBuilder settings(Settings settings) {
        this.settings = settings;
        return this;
    }


    public AeshInputProcessorBuilder completion(CompletionHandler completion) {
        this.completion = completion;
        return this;
    }

    public AeshInputProcessorBuilder interruptHook(InputProcessorInterruptHook interruptHook) {
        this.interruptHook = interruptHook;
        return this;
    }

    public InputProcessor create() {
        try {
            if(settings == null)
                throw new IllegalArgumentException("Settings must be provided to create InputProcessor");
            if(consoleBuffer == null)
                throw new IllegalArgumentException("ConsoleBuffer must be provided to create InputProcessor");

            if(!settings.isHistoryDisabled()) {
                if(settings.isHistoryPersistent())
                    history = new FileHistory(settings.getHistoryFile(), settings.getHistorySize());
                else
                    history = new InMemoryHistory(settings.getHistorySize());
            }

            return new AeshInputProcessor(consoleBuffer, history, settings, completion, interruptHook);
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Failed to create InputProcessor: "+e.getMessage());
        }
    }

}

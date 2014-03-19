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

import java.io.File;
import java.io.IOException;

/**
 * InputProcessor builder
 * If settings is set, there is no need to specify any of the history/search fields
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshInputProcessorBuilder {

    private ConsoleBuffer consoleBuffer;
    private Settings settings;
    private History history;
    private CompletionHandler completion;
    private InputProcessorInterruptHook interruptHook;
    private boolean enableSearch = true;
    private boolean enableHistory = true;
    private boolean persistHistory = true;
    private File historyFile;
    private int historySize = 100;

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

    public AeshInputProcessorBuilder enableHistory(boolean enableHistory) {
        this.enableHistory = enableHistory;
        return this;
    }

    public AeshInputProcessorBuilder persistHistory(boolean persistHistory) {
        this.persistHistory = persistHistory;
        return this;
    }

    public AeshInputProcessorBuilder historyFile(File historyFile) {
        this.historyFile = historyFile;
        return this;
    }

    public AeshInputProcessorBuilder historySize(int historySize) {
        this.historySize = historySize;
        return this;
    }

    public AeshInputProcessorBuilder enableSearch(boolean enableSearch) {
        this.enableSearch = enableSearch;
        return this;
    }

    public InputProcessor create() {
        try {
            if(consoleBuffer == null)
                throw new IllegalArgumentException("ConsoleBuffer must be provided to create InputProcessor");

            if(settings != null && !settings.isHistoryDisabled()) {
                if(settings != null) {
                    if(settings.isHistoryPersistent())
                        history = new FileHistory(settings.getHistoryFile(), settings.getHistorySize());
                    else
                        history = new InMemoryHistory(settings.getHistorySize());

                }

            }
            else {
                if(persistHistory && historyFile != null)
                    history = new FileHistory(historyFile, historySize);
                else
                    history = new InMemoryHistory(historySize);
            }

            return new AeshInputProcessor(consoleBuffer, history, completion, interruptHook, enableHistory, enableSearch);
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Failed to create InputProcessor: "+e.getMessage());
        }
    }

}

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.console;

import org.jboss.aesh.history.History;
import org.jboss.aesh.readline.KeyEvent;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.util.LoggerUtil;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshInputProcessor implements InputProcessor {

    private final InputProcessorInterruptHook interruptHook;

    private final History history;

    private final ConsoleBuffer consoleBuffer;

    private final CompletionHandler completionHandler;

    private String returnValue;

    private static final Logger LOGGER = LoggerUtil.getLogger(AeshInputProcessor.class.getName());

    AeshInputProcessor(ConsoleBuffer consoleBuffer,
                       History history,
                       CompletionHandler completionHandler,
                       InputProcessorInterruptHook interruptHook) {

        this.consoleBuffer = consoleBuffer;
        this.history = history;

        history.enable();
        this.completionHandler = completionHandler;
        this.interruptHook = interruptHook;
    }

    @Override
    public void resetBuffer() {
        consoleBuffer.getBuffer().reset();
    }

    @Override
    public ConsoleBuffer getBuffer() {
        return consoleBuffer;
    }

    @Override
    public void setReturnValue(String value) {
        returnValue = value;
    }

    @Override
    public InputProcessorInterruptHook getInterruptHook() {
        return interruptHook;
    }

    @Override
    public synchronized String parseOperation(KeyEvent event) throws IOException {

        returnValue = null;

        LOGGER.info("input key: " + event.name());

        org.jboss.aesh.readline.Action action = consoleBuffer.parse(event);
        if(action != null) {
            LOGGER.info("action: " + action.name());
            action.apply(this);
        }
        else {
            //TODO: probably dont need this if check when we have all keys mapped
            if(Key.isPrintable(event.buffer().array()))
                consoleBuffer.writeChars(event.buffer().array());
        }

        return returnValue;
    }

    @Override
    public History getHistory() {
        return history;
    }

    @Override
    public CompletionHandler getCompleter(){
        return completionHandler;
    }

    @Override
    public void clearBufferAndDisplayPrompt() {
        consoleBuffer.getBuffer().reset();
        consoleBuffer.getUndoManager().clear();
        consoleBuffer.displayPrompt();
    }

}

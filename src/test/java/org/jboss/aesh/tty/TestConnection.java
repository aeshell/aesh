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

package org.jboss.aesh.tty;

import org.jboss.aesh.readline.Prompt;
import org.jboss.aesh.readline.Readline;
import org.jboss.aesh.readline.completion.Completion;
import org.jboss.aesh.readline.editing.EditMode;
import org.jboss.aesh.readline.editing.EditModeBuilder;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.util.Parser;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TestConnection implements Connection {

    private Consumer<Size> sizeHandler;
    private Consumer<Signal> signalHandler;
    private Consumer<int[]> stdinHandler;
    private Consumer<int[]> stdOutHandler;
    private Consumer<Void> closeHandler;

    private StringBuilder bufferBuilder;
    private String out;
    private Size size;

    private Prompt prompt = new Prompt(": ");
    private CountDownLatch latch;
    private volatile boolean waiting = false;
    private volatile boolean reading = false;

    public TestConnection() {
        //default emacs mode
        this(EditModeBuilder.builder().create(), null);
    }

    public TestConnection(EditMode editMode) {
        this(editMode, null);
    }

    public TestConnection(Size size) {
        this(EditModeBuilder.builder().create(), null, size);
    }

    public TestConnection(List<Completion> completions) {
        this(EditModeBuilder.builder().create(), completions);
    }

    public TestConnection(EditMode editMode, List<Completion> completions) {
        this(editMode, completions, null);
    }

    public TestConnection(EditMode editMode, List<Completion> completions, Size size) {
        this(editMode, completions, size, null);
    }

    public TestConnection(EditMode editMode, List<Completion> completions, Size size, Prompt prompt) {
        if(editMode == null)
            editMode = EditModeBuilder.builder().create();
        bufferBuilder = new StringBuilder();
        stdOutHandler = ints -> {
           bufferBuilder.append(Parser.stripAwayAnsiCodes(Parser.fromCodePoints(ints)));
        };

        if(size == null)
            this.size = new Size(80, 20);
        else
            this.size = size;

        if(prompt != null)
            this.prompt = prompt;

    }

    public void clearOutputBuffer() {
        if(bufferBuilder.length() > 0)
            bufferBuilder.delete(0, bufferBuilder.length());
    }

    public String getOutputBuffer() {
        return bufferBuilder.toString();
    }

    public String getPrompt() {
        return Parser.fromCodePoints(prompt.getPromptAsString());
    }

    public void setPrompt(Prompt prompt) {
        if(prompt != null)
            this.prompt = prompt;
    }

    public String getLine() {
        return out;
    }

    @Override
    public String terminalType() {
        return "fooTerm";
    }

    @Override
    public Size size() {
        return size;
    }

    @Override
    public Consumer<Size> getSizeHandler() {
        return sizeHandler;
    }

    @Override
    public void setSizeHandler(Consumer<Size> handler) {
        this.sizeHandler = handler;
    }

    @Override
    public Consumer<Signal> getSignalHandler() {
        return signalHandler;
    }

    @Override
    public void setSignalHandler(Consumer<Signal> handler) {
        signalHandler = handler;
    }

    @Override
    public Consumer<int[]> getStdinHandler() {
        return stdinHandler;
    }

    @Override
    public void setStdinHandler(Consumer<int[]> handler) {
        stdinHandler = handler;
    }

    @Override
    public Consumer<int[]> stdoutHandler() {
        return stdOutHandler;
    }

    @Override
    public void setCloseHandler(Consumer<Void> closeHandler) {
        this.closeHandler = closeHandler;
    }

    @Override
    public Consumer<Void> getCloseHandler() {
        return closeHandler;
    }

    @Override
    public void close() {
        reading = false;
        closeHandler.accept(null);
    }

    @Override
    public void open() {
        //we're not doing anything here, all input will come from the read(..) methods
        reading = true;
    }

    private void doRead(int[] input) {
        if(reading) {
            if (waiting) {
                try {
                    latch.await();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(stdinHandler != null) {
                stdinHandler.accept(input);
            }
            else {
                try {
                    Thread.sleep(10);
                    doRead(input);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        else
            throw new RuntimeException("Got input when not reading: "+ Arrays.toString(input));
    }

    @Override
    public void openNonBlockingReader() {
    }

    @Override
    public void suspend() {
        latch = new CountDownLatch(1);
        waiting = true;
     }

    @Override
    public boolean suspended() {
        return waiting;
    }

    @Override
    public void awake() {
        if(waiting) {
            waiting = false;
            latch.countDown();
        }
     }

    @Override
    public boolean put(Capability capability, Object... params) {
        return false;
    }

    public void assertLine(String expected) {
        assertEquals(expected, out);
    }

    public void read(int... data) {
        doRead(data);
    }

    public void read(Key key) {
        doRead(key.getKeyValues());
    }

    public void read(String data) {
        doRead(Parser.toCodePoints(data));
    }

}

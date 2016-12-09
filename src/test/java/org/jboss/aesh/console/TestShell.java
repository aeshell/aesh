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

import org.aesh.readline.Prompt;
import org.aesh.terminal.Key;
import org.aesh.tty.Size;

import java.io.PrintStream;


public class TestShell implements Shell {

    private final PrintStream out;
    private final PrintStream err;

    public TestShell(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    @Override
    public void clear() {
    }

    @Override
    public Size size() {
        return new Size(80, 20);
    }

    @Override
    public void write(String out) {

    }

    @Override
    public void writeln(String out) {

    }

    @Override
    public void write(int[] out) {

    }

    @Override
    public String readLine() throws InterruptedException {
        return null;
    }

    @Override
    public String readLine(Prompt prompt) throws InterruptedException {
        return null;
    }

    @Override
    public Key read() throws InterruptedException {
        return null;
    }

    @Override
    public Key read(Prompt prompt) throws InterruptedException {
        return null;
    }

    @Override
    public boolean enableAlternateBuffer() {
        return false;
    }

    @Override
    public boolean enableMainBuffer() {
        return false;
    }
}

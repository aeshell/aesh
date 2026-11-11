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
package org.aesh.command.impl.shell;

import java.io.Console;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.aesh.command.shell.Shell;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.Key;
import org.aesh.terminal.tty.Size;
import org.aesh.terminal.utils.ANSI;
import org.aesh.terminal.utils.Config;
import org.aesh.terminal.utils.Parser;

/**
 * {@link Shell} that writes to {@code System.out}, used when no shell is set
 * on the runtime. Extracted from {@code DefaultCommandInvocation} so the
 * output handler can tee over it instead of silently receiving nothing.
 */
public class StdoutShell implements Shell {

    @Override
    public void write(String out, boolean paging) {
        System.out.print(out);
    }

    @Override
    public void writeln(String out, boolean paging) {
        System.out.println(out);
    }

    @Override
    public void write(int[] out) {
        System.out.print(Parser.fromCodePoints(out));
    }

    @Override
    public void write(char out) {
        System.out.println(out);
    }

    @Override
    public String readLine() {
        return readLine(new Prompt());
    }

    @Override
    public String readLine(Prompt prompt) {
        if (Config.isWindows())
            return null;
        Console console = System.console();
        if (console != null) {
            if (prompt != null) {
                console.writer().print(Parser.fromCodePoints(prompt.getANSI()));
                console.writer().flush();
                if (prompt.isMasking()) {
                    return new String(console.readPassword());
                }
            }
            return console.readLine();
        }
        return null;
    }

    @Override
    public Key read() {
        return read(null);
    }

    @Override
    public Key read(long timeout, TimeUnit unit) throws InterruptedException {
        return read(null);
    }

    @Override
    public Key read(Prompt prompt) {
        if (Config.isWindows())
            return null;
        Console console = System.console();
        if (console != null) {
            try {
                if (prompt != null) {
                    console.writer().print(Parser.fromCodePoints(prompt.getANSI()));
                    console.writer().flush();
                }
                int input = console.reader().read();
                return Key.getKey(new int[] { input });
            } catch (IOException e) {
                // I/O error reading key, return null
            }
        }
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

    @Override
    public Size size() {
        return new Size(1, -1);
    }

    @Override
    public void clear() {
        System.out.print(Parser.fromCodePoints(ANSI.CLEAR_SCREEN));
    }
}

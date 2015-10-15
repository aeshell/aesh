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
package org.jboss.aesh.terminal.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.jboss.aesh.terminal.impl.CygwinPty;
import org.jboss.aesh.terminal.impl.ExecPty;
import org.jboss.aesh.terminal.impl.ExternalConsole;
import org.jboss.aesh.terminal.impl.PosixSysConsole;
import org.jboss.aesh.terminal.impl.Pty;
import org.jboss.aesh.terminal.impl.WinSysConsole;
import org.jboss.aesh.terminal.utils.OSUtils;

public final class ConsoleBuilder {

    public static Console console() throws IOException {
        return builder().build();
    }

    public static ConsoleBuilder builder() {
        return new ConsoleBuilder();
    }

    private String name;
    private InputStream in;
    private OutputStream out;
    private String type;
    private String encoding;
    private Boolean system;
    private boolean nativeSignals = true;

    private ConsoleBuilder() {
    }

    public ConsoleBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ConsoleBuilder streams(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        return this;
    }

    public ConsoleBuilder system(boolean system) {
        this.system = system;
        return this;
    }

    public ConsoleBuilder type(String type) {
        this.type = type;
        return this;
    }

    public ConsoleBuilder encoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    public Console build() throws IOException {
        String name = this.name;
        if (name == null) {
            name = "JLine console";
        }
        if ((system != null && system)
                || (system == null
                    && (in == null || in == System.in)
                    && (out == null || out == System.out))) {
            //
            // Cygwin support
            //
            if (OSUtils.IS_CYGWIN) {
                String type = this.type;
                if (type == null) {
                    type = System.getenv("TERM");
                }
                String encoding = this.encoding;
                if (encoding == null) {
                    encoding = Charset.defaultCharset().name();
                }
                Pty pty = CygwinPty.current();
                return new PosixSysConsole(name, type, pty, encoding, nativeSignals);
            }
            else if (OSUtils.IS_WINDOWS) {
                return new WinSysConsole(name, nativeSignals);
            } else {
                String type = this.type;
                if (type == null) {
                    type = System.getenv("TERM");
                }
                String encoding = this.encoding;
                if (encoding == null) {
                    encoding = Charset.defaultCharset().name();
                }
                Pty pty = ExecPty.current();
                return new PosixSysConsole(name, type, pty, encoding, nativeSignals);
            }
        } else {
            return new ExternalConsole(name, type, in, out, encoding);
        }
    }
}

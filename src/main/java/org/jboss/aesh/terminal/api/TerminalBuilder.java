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
import org.jboss.aesh.terminal.impl.ExternalTerminal;
import org.jboss.aesh.terminal.impl.PosixSysTerminal;
import org.jboss.aesh.terminal.impl.Pty;
import org.jboss.aesh.terminal.impl.WinSysTerminal;
import org.jboss.aesh.terminal.utils.OSUtils;

public final class TerminalBuilder {

    public static Terminal console() throws IOException {
        return builder().build();
    }

    public static TerminalBuilder builder() {
        return new TerminalBuilder();
    }

    private String name;
    private InputStream in;
    private OutputStream out;
    private String type;
    private String encoding;
    private Boolean system;
    private boolean nativeSignals = true;

    private TerminalBuilder() {
    }

    public TerminalBuilder name(String name) {
        this.name = name;
        return this;
    }

    public TerminalBuilder streams(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        return this;
    }

    public TerminalBuilder system(boolean system) {
        this.system = system;
        return this;
    }

    public TerminalBuilder type(String type) {
        this.type = type;
        return this;
    }

    public TerminalBuilder encoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    public Terminal build() throws IOException {
        String name = this.name;
        if (name == null) {
            name = "JLine console";
        }
        String encoding = this.encoding;
        if (encoding == null) {
            encoding = Charset.defaultCharset().name();
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
                Pty pty = CygwinPty.current();
                return new PosixSysTerminal(name, type, pty, encoding, nativeSignals);
            }
            else if (OSUtils.IS_WINDOWS) {
                return new WinSysTerminal(name, nativeSignals);
            } else {
                String type = this.type;
                if (type == null) {
                    type = System.getenv("TERM");
                }
                Pty pty = ExecPty.current();
                return new PosixSysTerminal(name, type, pty, encoding, nativeSignals);
            }
        } else {
            return new ExternalTerminal(name, type, in, out, encoding);
        }
    }
}

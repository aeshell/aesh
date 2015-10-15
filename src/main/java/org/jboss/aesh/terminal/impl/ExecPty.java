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
package org.jboss.aesh.terminal.impl;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.aesh.terminal.api.Attributes;
import org.jboss.aesh.terminal.api.Attributes.ControlChar;
import org.jboss.aesh.terminal.api.Attributes.ControlFlag;
import org.jboss.aesh.terminal.api.Attributes.InputFlag;
import org.jboss.aesh.terminal.api.Attributes.LocalFlag;
import org.jboss.aesh.terminal.api.Attributes.OutputFlag;
import org.jboss.aesh.terminal.api.Size;
import org.jboss.aesh.terminal.utils.ExecHelper;
import org.jboss.aesh.terminal.utils.OSUtils;
import org.jboss.aesh.util.LoggerUtil;


public class ExecPty implements Pty {

    private static final Logger LOGGER = LoggerUtil.getLogger(CygwinPty.class.getName());

    private final String name;

    public static Pty current() throws IOException {
        try {
            Process p = new ProcessBuilder(OSUtils.TTY_COMMAND)
                    .redirectInput(Redirect.INHERIT)
                    .start();
            String result = ExecHelper.waitAndCapture(p).trim();
            if (p.exitValue() != 0) {
                throw new IOException("Not a tty");
            }
            return new ExecPty(result);
        } catch (InterruptedException e) {
            throw (IOException) new InterruptedIOException("Command interrupted").initCause(e);
        }
    }

    protected ExecPty(String name) {
        this.name = name;
    }

    @Override
    public void close() throws IOException {
    }

    public String getName() {
        return name;
    }

    @Override
    public InputStream getMasterInput() {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream getMasterOutput() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getSlaveInput() throws IOException {
        return new FileInputStream(getName());
    }

    @Override
    public OutputStream getSlaveOutput() throws IOException {
        return new FileOutputStream(getName());
    }

    @Override
    public Attributes getAttr() throws IOException {
        String cfg = doGetConfig();
        return doGetAttr(cfg);
    }

    @Override
    public void setAttr(Attributes attr) throws IOException {
        Attributes current = getAttr();
        List<String> commands = new ArrayList<>();
        for (InputFlag flag : InputFlag.values()) {
            if (attr.getInputFlag(flag) != current.getInputFlag(flag)) {
                commands.add((attr.getInputFlag(flag) ? flag.name() : "-" + flag.name()).toLowerCase());
            }
        }
        for (OutputFlag flag : OutputFlag.values()) {
            if (attr.getOutputFlag(flag) != current.getOutputFlag(flag)) {
                commands.add((attr.getOutputFlag(flag) ? flag.name() : "-" + flag.name()).toLowerCase());
            }
        }
        for (ControlFlag flag : ControlFlag.values()) {
            if (attr.getControlFlag(flag) != current.getControlFlag(flag)) {
                commands.add((attr.getControlFlag(flag) ? flag.name() : "-" + flag.name()).toLowerCase());
            }
        }
        for (LocalFlag flag : LocalFlag.values()) {
            if (attr.getLocalFlag(flag) != current.getLocalFlag(flag)) {
                commands.add((attr.getLocalFlag(flag) ? flag.name() : "-" + flag.name()).toLowerCase());
            }
        }
        String undef = System.getProperty("os.name").toLowerCase().startsWith("hp") ? "^-" : "undef";
        for (ControlChar cchar : ControlChar.values()) {
            if (attr.getControlChar(cchar) != current.getControlChar(cchar)) {
                String str = "";
                int v = attr.getControlChar(cchar);
                commands.add(cchar.name().toLowerCase().substring(1));
                if (cchar == ControlChar.VMIN || cchar == ControlChar.VTIME) {
                    commands.add(Integer.toBinaryString(v));
                }
                else if (v == 0) {
                    commands.add(undef);
                }
                else {
                    if (v >= 128) {
                        v -= 128;
                        str += "M-";
                    }
                    if (v < 32 || v == 127) {
                        v ^= 0x40;
                        str += "^";
                    }
                    str += (char) v;
                    commands.add(str);
                }
            }
        }
        if (!commands.isEmpty()) {
            commands.add(0, OSUtils.STTY_COMMAND);
            commands.add(1, OSUtils.STTY_F_OPTION);
            commands.add(2, getName());
            exec(commands.toArray(new String[commands.size()]));
        }
    }

    @Override
    public Size getSize() throws IOException {
        String cfg = doGetConfig();
        return doGetSize(cfg);
    }

    protected String doGetConfig() throws IOException {
        return exec(OSUtils.STTY_COMMAND, OSUtils.STTY_F_OPTION, getName(), "-a");
        //return exec(OSUtils.STTY_COMMAND, "-a");
    }

    static Attributes doGetAttr(String cfg) throws IOException {
        Attributes attributes = new Attributes();
        for (InputFlag flag : InputFlag.values()) {
            Boolean value = doGetFlag(cfg, flag);
            if (value != null) {
                attributes.setInputFlag(flag, value);
            }
        }
        for (OutputFlag flag : OutputFlag.values()) {
            Boolean value = doGetFlag(cfg, flag);
            if (value != null) {
                attributes.setOutputFlag(flag, value);
            }
        }
        for (ControlFlag flag : ControlFlag.values()) {
            Boolean value = doGetFlag(cfg, flag);
            if (value != null) {
                attributes.setControlFlag(flag, value);
            }
        }
        for (LocalFlag flag : LocalFlag.values()) {
            Boolean value = doGetFlag(cfg, flag);
            if (value != null) {
                attributes.setLocalFlag(flag, value);
            }
        }
        for (ControlChar cchar : ControlChar.values()) {
            String name = cchar.name().toLowerCase().substring(1);
            if ("reprint".endsWith(name)) {
                name = "(?:reprint|rprnt)";
            }
            Matcher matcher = Pattern.compile("[\\s;]" + name + "\\s*=\\s*(.+?)[\\s;]").matcher(cfg);
            if (matcher.find()) {
                attributes.setControlChar(cchar, parseControlChar(matcher.group(1).toUpperCase()));
            }
        }
        return attributes;
    }

    private static Boolean doGetFlag(String cfg, Enum<?> flag) {
        Matcher matcher = Pattern.compile("(?:^|[\\s;])(\\-?" + flag.name().toLowerCase() + ")(?:[\\s;]|$)").matcher(cfg);
        return matcher.find() ? !matcher.group(1).startsWith("-") : null;
    }

    static int parseControlChar(String str) {
        // undef
        if ("<UNDEF>".equals(str)) {
            return -1;
        }
        // del
        if ("DEL".equalsIgnoreCase(str)) {
            return 127;
        }
        // octal
        if (str.charAt(0) == '0') {
            return Integer.parseInt(str, 8);
        }
        // decimal
        if (str.charAt(0) >= '1' && str.charAt(0) <= '9') {
            return Integer.parseInt(str, 10);
        }
        // control char
        if (str.charAt(0) == '^') {
            if (str.charAt(1) == '?') {
                return 127;
            } else {
                return str.charAt(1) - 64;
            }
        } else if (str.charAt(0) == 'M' && str.charAt(1) == '-') {
            if (str.charAt(2) == '^') {
                if (str.charAt(3) == '?') {
                    return 127 + 128;
                } else {
                    return str.charAt(3) - 64 + 128;
                }
            } else {
                return str.charAt(2) + 128;
            }
        } else {
            return str.charAt(0);
        }
    }

    static Size doGetSize(String cfg) throws IOException {
        return new Size(doGetInt("rows", cfg), doGetInt("columns", cfg));
    }

    static int doGetInt(String name, String cfg) throws IOException {
        String[] patterns = new String[] {
                "\\b([0-9]+)\\s+" + name + "\\b",
                "\\b" + name + "\\s+([0-9]+)\\b",
                "\\b" + name + "\\s*=\\s*([0-9]+)\\b"
        };
        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(cfg);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        throw new IOException("Unable to parse " + name);
    }

    @Override
    public void setSize(Size size) throws IOException {
        exec(OSUtils.STTY_COMMAND,
             OSUtils.STTY_F_OPTION, getName(),
             "rows", Integer.toString(size.getHeight()),
             "columns", Integer.toString(size.getWidth()));
    }

    private static String exec(final String... cmd) throws IOException {
        assert cmd != null;
        try {
            LOGGER.log(Level.FINEST, "Running: ", cmd);
            Process p = new ProcessBuilder(cmd).start();
            String result = ExecHelper.waitAndCapture(p);
            LOGGER.log(Level.FINEST, "Result: ", result);
            if (p.exitValue() != 0) {
                throw new IOException("Error executing '" + String.join(" ", cmd) + "': " + result);
            }
            return result;
        } catch (InterruptedException e) {
            throw (IOException) new InterruptedIOException("Command interrupted").initCause(e);
        }
    }

}

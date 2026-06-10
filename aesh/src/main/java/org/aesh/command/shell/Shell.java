/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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

package org.aesh.command.shell;

import java.util.concurrent.TimeUnit;

import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.Connection;
import org.aesh.terminal.Key;
import org.aesh.terminal.tty.ScreenRegion;
import org.aesh.terminal.tty.Size;
import org.aesh.terminal.tty.SplitScreen;
import org.aesh.terminal.utils.ANSI;

/**
 * @author Aesh team
 */
public interface Shell {

    /**
     * @param out write out to the output stream
     */
    default void write(String out) {
        write(out, false);
    }

    /**
     * @param out write out to the output stream including a line separator at the end
     */
    default void writeln(String out) {
        writeln(out, false);
    }

    /**
     * Print a message on console
     *
     * @param msg
     * @param paging True means that output longer than terminal height should be paused.
     */
    void write(String msg, boolean paging);

    /**
     * Print a new line with a message on console;
     *
     * @param msg
     * @param paging True means that output longer than terminal height should be paused.
     */
    void writeln(String msg, boolean paging);

    /**
     * @param out write out to the output stream
     */
    void write(int[] out);

    /**
     * @param out write out to the output stream
     */
    void write(char out);

    /**
     * Blocking call which will return when user press the "enter" key
     *
     * @return input line
     */
    String readLine() throws InterruptedException;

    /**
     * Blocking call which will return when user press the "enter" key
     *
     * @param prompt display prompt
     * @return input line
     */
    String readLine(Prompt prompt) throws InterruptedException;

    /**
     * Blocking call which will return when user press the "enter" key
     *
     * @param prompt display prompt
     * @return input line
     */
    default String readLine(String prompt) throws InterruptedException {
        return readLine(new Prompt(prompt));
    }

    /**
     * Blocking call that will return after the first key press
     *
     * @return input
     */
    Key read() throws InterruptedException;

    /**
     * Blocking call that will return after the first key press
     *
     * @return input or null if it times out
     * @throws InterruptedException when interrupted
     */
    Key read(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Blocking call that will return after the first key press
     *
     * @return input
     */
    Key read(Prompt prompt) throws InterruptedException;

    boolean enableAlternateBuffer();

    boolean enableMainBuffer();

    Size size();

    /**
     * Clear the terminal
     */
    void clear();

    /**
     * Returns the underlying terminal connection.
     * Useful for integrating with frameworks that need direct terminal access
     * (e.g., TamboUI TUI framework).
     *
     * @return the Connection, or null if not available
     */
    default Connection connection() {
        return null;
    }

    /**
     * Write a hyperlink to the terminal. If the terminal supports OSC 8 hyperlinks,
     * the text will be rendered as a clickable link. Otherwise, plain text is written.
     * Routes through write(String) so paging is preserved.
     *
     * @param url the URL target of the hyperlink
     * @param text the visible text for the hyperlink
     */
    default void writeHyperlink(String url, String text) {
        Connection conn = connection();
        if (conn != null && conn.terminal().supportsHyperlinks()) {
            write(ANSI.hyperlink(url, text));
        } else {
            write(text);
        }
    }

    /**
     * Check if the terminal supports OSC 8 hyperlinks.
     *
     * @return true if hyperlinks are supported
     */
    default boolean supportsHyperlinks() {
        Connection conn = connection();
        return conn != null && conn.terminal().supportsHyperlinks();
    }

    /**
     * Enable split-screen mode, dividing the terminal into a scrolling
     * output region (top) and a readline prompt region (bottom).
     * <p>
     * The ratio controls the fraction of screen allocated to the top region.
     * For example, 0.7 gives 70% to output and 30% to the prompt.
     * <p>
     * When split-screen is not supported (e.g., runtime mode), returns null.
     *
     * @param ratio fraction of screen for the top region (0.0-1.0)
     * @return the SplitScreen manager, or null if not supported
     */
    default SplitScreen enableSplitScreen(double ratio) {
        Connection conn = connection();
        if (conn != null) {
            return conn.splitScreen(ratio);
        }
        return null;
    }

    /**
     * Returns the current split-screen manager, or null if split-screen
     * is not active.
     *
     * @return the SplitScreen, or null
     */
    default SplitScreen splitScreen() {
        Connection conn = connection();
        if (conn != null) {
            return conn.splitScreen();
        }
        return null;
    }

    /**
     * Set the current screen region for output routing.
     * <p>
     * When set, {@link #write(String)} routes output to the specified region
     * instead of the default terminal output. When split-screen is not active,
     * this is a no-op.
     *
     * @param region the region to route output to, or null for default
     */
    default void setCurrentRegion(ScreenRegion region) {
        Connection conn = connection();
        if (conn != null) {
            conn.setCurrentRegion(region);
        }
    }
}

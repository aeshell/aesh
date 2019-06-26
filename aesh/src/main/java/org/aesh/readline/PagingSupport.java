/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
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
package org.aesh.readline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import org.aesh.readline.action.ActionDecoder;
import org.aesh.readline.history.History;
import org.aesh.readline.history.InMemoryHistory;
import org.aesh.readline.terminal.Key;
import org.aesh.readline.terminal.impl.WinSysTerminal;
import org.aesh.readline.util.Parser;
import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.Signal;
import org.aesh.terminal.tty.Size;
import org.aesh.terminal.utils.ANSI;
import org.aesh.terminal.utils.Config;

/**
 *
 * @author jdenise@redhat.com
 */
public class PagingSupport {

    private class Paging {

        private boolean notFound;
        private boolean searchingMode;
        private int currentLine;
        private int allLines;
        private int lastScrolledLines;
        private List<String> lines;
        private final String[] splitLines;
        private int jumpIndex = -1;
        private String pattern;
        private int max;
        private boolean paging;

        // Starting Windows 10, alternate buffer is supported.
        private final boolean alternateSupported;

        Paging(String output, Size termSize) {
            // '\R' will match any line break.
            // -1 to keep empty lines at the end of content.
            splitLines = output.split("\\R", -1);
            lines = buildLines(termSize);
            lastScrolledLines = lines.size();
            max = termSize.getHeight() - 1;
            if (Config.isWindows()) {
                // forcePaging is used by tests.
                alternateSupported = WinSysTerminal.isVTSupported();
            } else {
                alternateSupported = true;
            }
            if (lines.size() > max) {
                if (alternateSupported) {
                    getConnection().write(ANSI.ALTERNATE_BUFFER);
                    clearScreen();
                }
                paging = true;
            }
        }

        private List<String> buildLines(Size size) {
            List<String> lst = new ArrayList<>();
            int width = Config.isWindows() ? size.getWidth() - 1 : size.getWidth();
            for (String l : splitLines) {
                String remaining = l;
                do {
                    String st = remaining.substring(0, Math.min(remaining.length(), width));
                    lst.add(st);
                    remaining = remaining.substring(Math.min(remaining.length(), width));
                } while (!remaining.isEmpty());
            }
            return lst;
        }

        int getMax() {
            return max;
        }

        void pagingDone() {
            if (paging && alternateSupported) {
                //Print the output to main buffer (from start until the last scrolled position)
                getConnection().write(ANSI.MAIN_BUFFER);
                printScrolledLines();
            }
        }

        boolean needPrompt() {
            return (currentLine > getMax() - 1 && jumpIndex == -1) || (endBuffer() && searchingMode);
        }

        boolean inWorkflow() {
            return allLines < lines.size() || searchingMode;
        }

        void exit() {
            lastScrolledLines = allLines;
            allLines = lines.size();
            searchingMode = false;
        }

        void pageDown() {
            notFound = false;
            currentLine = 0;
            // Exit the workflow.
            if (endBuffer()) {
                exit();
            }
        }

        void pageUp() {
            if (!alternateSupported) {
                return;
            }
            clearScreen();
            notFound = false;
            currentLine = 0;
            if (allLines > 2 * getMax()) {
                //Move one screen up
                allLines -= 2 * getMax();
            } else {
                //Move to the start of input
                allLines = 0;
            }
        }

        void previousMatch() {
            if (!alternateSupported) {
                return;
            }
            if (!searchingMode && searchHistory.size() != 0) {
                int[] p = searchHistory.get(searchHistory.size() - 1);
                pattern = Parser.fromCodePoints(p);
                searchingMode = true;
            }
            if (searchingMode) {
                if (allLines <= getMax()) {
                    notFound = true;
                }
                int previous = previousMatch(pattern, lines, allLines - getMax() - 1);
                if (previous >= 0) {
                    jumpIndex = allLines - previous - 1;
                    notFound = false;
                    resetScreen();
                } else {
                    notFound = true;
                }
            }
        }

        private int previousMatch(String pattern, List<String> lines, int currentLine) {
            int previous = 0;
            for (int i = currentLine; i >= 0; i--) {
                String l = lines.get(i);
                if (l.contains(pattern)) {
                    return previous;
                }
                previous += 1;
            }
            return -1;
        }

        private void nextMatch() {
            if (!alternateSupported) {
                return;
            }
            if (searchingMode) {
                if (endBuffer()) {
                    notFound = true;
                } else {
                    int start = allLines - getMax() < 0 ? 0 : allLines - getMax();
                    int next = nextMatch(pattern, lines, start + 1);
                    if (next >= 0) {
                        // We need to redraw everything from start in case
                        // some matches are already displayed and need highlighting
                        jumpIndex = Math.min(allLines + next + 1, lines.size());
                        notFound = false;
                        resetScreen();
                    } else {
                        notFound = true;
                    }
                }
            } else if (searchHistory.size() != 0) {
                int[] p = searchHistory.get(searchHistory.size() - 1);
                doSearch(Parser.fromCodePoints(p));
            }
        }

        private int nextMatch(String pattern, List<String> lines, int currentLine) {
            int next = 0;
            for (int i = currentLine; i < lines.size(); i++) {
                String l = lines.get(i);
                if (l.contains(pattern)) {
                    return next;
                }
                next += 1;
            }
            return -1;
        }

        private void search() throws InterruptedException, IOException {
            if (!alternateSupported) {
                return;
            }
            doSearch(readPattern());
        }

        private void doSearch(String pattern) {
            // The complete buffer needs to be redrawn to clear the pattern prompt
            // and to highlight possible matches already displayed.
            if (pattern == null || pattern.isEmpty()) {
                // needed to redraw in order to clear pattern prompt.
                jumpIndex = allLines;
            } else {
                this.pattern = pattern;
                int start = allLines - getMax() < 0 ? 0 : allLines - getMax();
                int next = nextMatch(pattern, lines, start);
                if (next >= 0) {
                    jumpIndex = Math.min(allLines + next, lines.size());
                    searchingMode = true;
                    notFound = false;
                } else {
                    notFound = true;
                    // do we have something from the beginning
                    int n = nextMatch(pattern, lines, 0);
                    if (n >= 0) {
                        searchingMode = true;
                    }
                    // needed to redraw in order to clear pattern prompt.
                    jumpIndex = allLines;
                }
            }
            resetScreen();
        }

        private void lineUp() {
            if (!alternateSupported) {
                return;
            }
            notFound = false;
            if (allLines > getMax()) {
                currentLine = 0;
                //Move one line up
                allLines -= getMax() + 1;
                clearScreen();
            }
        }

        private void lineDown() {
            notFound = false;
            // Exit the workflow
            if (endBuffer()) {
                exit();
            }
            currentLine -= 1;
        }

        private int getPercentage() {
            return (allLines * 100) / lines.size();
        }

        private String nextCurrentLine() {
            String line = lines.get(allLines);
            currentLine += 1;
            allLines += 1;
            if (jumpIndex == allLines) {
                currentLine = getMax();
                jumpIndex = -1;
            }
            return line;
        }

        private boolean endBuffer() {
            return allLines == lines.size();
        }

        private void redraw(Size size) {
            if (!alternateSupported) {
                return;
            }
            int oldMax = max;
            max = size.getHeight() - 1;
            lines = buildLines(size);
            if (lines.size() > max) {
                jumpIndex = allLines + (max - oldMax);
            } else {
                jumpIndex = -1;
            }
            lastScrolledLines = lines.size();
            resetScreen();
            while (inWorkflow() && !needPrompt()) {
                printCurrentLine();
            }
            // Redraw the prompt in all cases, even if resizing expose everything.
            drawPrompt();
        }

        private void printCurrentLine() {
            String l = nextCurrentLine();
            if (searchingMode) {
                displayHightlighted(pattern, l);
            } else {
                getConnection().write(l + Config.getLineSeparator());
            }
        }

        private void printScrolledLines() {
            //Print the output to main buffer (from start until the last scrolled position)
            for (int i = 0; i < lastScrolledLines; i++) {
                String l = lines.get(i);
                getConnection().write(l + Config.getLineSeparator());
            }
        }

        private void drawPrompt() {
            if (notFound) {
                getConnection().write(ANSI.INVERT_BACKGROUND);
                getConnection().write("Pattern not found");
                getConnection().write(ANSI.RESET);
            } else {
                getConnection().write("--More(" + getPercentage() + "%)--");
            }
        }

        private void goHome() {
            if (!alternateSupported) {
                return;
            }
            notFound = false;
            resetScreen();
        }

        private void goEnd() {
            notFound = false;
            // Jump to the size - 1 line to not exit
            // the paging.
            if (allLines < lines.size() - 1) {
                jumpIndex = lines.size() - 1;
            }
        }

        private void resetScreen() {
            currentLine = 0;
            allLines = 0;
            clearScreen();
        }
    }

    private static final EnumMap<ReadlineFlag, Integer> RED_PATTERN_READLINE_FLAGS
            = new EnumMap<>(ReadlineFlag.class);

    static {
        RED_PATTERN_READLINE_FLAGS.put(ReadlineFlag.NO_PROMPT_REDRAW_ON_INTR,
                Integer.MAX_VALUE);
    }

    private final History searchHistory = new InMemoryHistory();
    private Paging paging;
    private final Connection connection;
    private final Readline readline;
    private StringBuilder outputCollector;
    private final boolean search;

    public PagingSupport(Connection connection, boolean search) {
        this(connection, null, search);
    }

    @Deprecated
    public PagingSupport(Connection connection, Readline readline, boolean search) {
        this.connection = connection;
        this.readline = readline;
        this.search = search;
        Consumer<Size> consumer = connection.getSizeHandler();
        connection.setSizeHandler(new Consumer<Size>() {
            @Override
            public void accept(Size t) {
                if (consumer != null) {
                    consumer.accept(t);
                }
                if (paging != null) {
                    paging.redraw(t);
                }
            }
        });
    }

    public boolean isPagingOutputActive() {
        return paging != null && paging.paging;
    }

    public void reset() {
        outputCollector = null;
    }

    public void addContent(String content) {
        if (outputCollector == null) {
            outputCollector = new StringBuilder();
        }
        outputCollector.append(content);
    }

    private Connection getConnection() {
        return connection;
    }

    private Readline getReadline() {
        return readline == null ? new Readline() : readline;
    }

    private StringBuilder getOutputCollector() {
        return outputCollector;
    }

    private void clearScreen() {
        if (getConnection() != null) {
            getConnection().stdoutHandler().accept(ANSI.CLEAR_SCREEN);
        }
    }

    private void displayHightlighted(String pattern, String l) {
        int index = l.indexOf(pattern);
        while (index >= 0) {
            getConnection().write(l.substring(0, index));
            getConnection().write(ANSI.INVERT_BACKGROUND);
            getConnection().write(pattern);
            getConnection().write(ANSI.RESET);
            l = l.substring(index + pattern.length());
            index = l.indexOf(pattern);
        }
        getConnection().write(l + Config.getLineSeparator());
    }

    public void printCollectedOutput() {
        if (getOutputCollector() == null || getOutputCollector().length() == 0) {
            return;
        }
        String line = getOutputCollector().toString();
        if (line.isEmpty()) {
            return;
        }
        if (search) {
            printAndSearchCollectedOuput(line);
        } else {
            printCollectedOutput(line);
        }

    }

    private void printCollectedOutput(String line) {
        try {
            // '\R' will match any line break.
            // -1 to keep empty lines at the end of content.
            String[] lines = line.split("\\R", -1);
            int max = connection.size().getHeight();
            int currentLines = 0;
            int allLines = 0;
            while (allLines < lines.length) {
                if (currentLines > max - 2) {
                    try {
                        connection.write(ANSI.CURSOR_SAVE);
                        int percentage = (allLines * 100) / lines.length;
                        connection.write("--More(" + percentage + "%)--");
                        Key k = read();
                        connection.write(ANSI.CURSOR_RESTORE);
                        connection.stdoutHandler().accept(ANSI.ERASE_LINE_FROM_CURSOR);
                        if (k == null) { // interrupted, exit.
                            allLines = lines.length;
                        } else {
                            switch (k) {
                                case SPACE: {
                                    currentLines = 0;
                                    break;
                                }
                                case ENTER:
                                case CTRL_M: { // On Mac, CTRL_M...
                                    currentLines -= 1;
                                    break;
                                }
                                case q: {
                                    allLines = lines.length;
                                    break;
                                }
                            }
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ex);
                    }
                } else {
                    String l = lines[allLines];
                    currentLines += 1;
                    allLines += 1;
                    // Do not add an extra \n
                    // The \n has been added by the previous line.
                    if (allLines == lines.length) {
                        if (l.isEmpty()) {
                            continue;
                        }
                    }
                    connection.write(l + Config.getLineSeparator());
                }
            }
        } finally {
            outputCollector = null;
        }
    }

    private void printAndSearchCollectedOuput(String line) {
        try {
            paging = new Paging(line, getConnection().size());
            while (paging.inWorkflow()) {
                if (paging.needPrompt()) {
                    try {
                        getConnection().write(ANSI.CURSOR_SAVE);
                        paging.drawPrompt();
                        Key k = read();
                        getConnection().write(ANSI.CURSOR_RESTORE);
                        getConnection().stdoutHandler().accept(ANSI.ERASE_LINE_FROM_CURSOR);
                        if (k == null) { // interrupted, exit.
                            paging.exit();
                        } else {
                            switch (k) {
                                case SPACE:
                                case PGDOWN_2:
                                case PGDOWN: {
                                    paging.pageDown();
                                    break;
                                }
                                case BACKSLASH:
                                case PGUP_2:
                                case PGUP: {
                                    paging.pageUp();
                                    break;
                                }
                                case N: {
                                    paging.previousMatch();
                                    break;
                                }
                                case n: {
                                    paging.nextMatch();
                                    break;
                                }
                                case SLASH: {
                                    paging.search();
                                    break;
                                }
                                case SEMI_COLON:
                                case UP_2:
                                case UP: {
                                    paging.lineUp();
                                    break;
                                }
                                case DOWN:
                                case DOWN_2:
                                case ENTER:
                                case CTRL_M: { // On Mac, CTRL_M...
                                    paging.lineDown();
                                    break;
                                }
                                case HOME:
                                case HOME_2:
                                case g: {
                                    paging.goHome();
                                    break;
                                }
                                case END:
                                case END_2:
                                case END_3:
                                case G: {
                                    paging.goEnd();
                                    break;
                                }
                                case Q:
                                case ESC:
                                case q: {
                                    paging.exit();
                                    break;
                                }
                            }
                        }
                    } catch (InterruptedException ex) {
                        getConnection().write(ANSI.CURSOR_RESTORE);
                        getConnection().stdoutHandler().accept(ANSI.ERASE_LINE_FROM_CURSOR);
                        paging.exit();
                        //throw new RuntimeException(ex);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    paging.printCurrentLine();
                }
            }
        } finally {
            paging.pagingDone();
            paging = null;
            outputCollector = null;
        }
    }

    private Key read() throws InterruptedException {
        ActionDecoder decoder = new ActionDecoder();
        final Key[] key = {null};
        CountDownLatch latch = new CountDownLatch(1);
        Attributes attributes = getConnection().enterRawMode();
        // We need to set the interrupt SignalHandler to interrupt the reading.
        Consumer<Signal> prevHandler = getConnection().getSignalHandler();
        getConnection().setSignalHandler((signal) -> {
            // Interrupting the current reading thread.
            switch (signal) {
                case INT: {
                    latch.countDown();
                }
            }
        });
        try {
            getConnection().setStdinHandler(keys -> {
                decoder.add(keys);
                if (decoder.hasNext()) {
                    key[0] = Key.findStartKey(decoder.next().buffer().array());
                    latch.countDown();
                }
            });
            try {
                // Wait until interrupted
                latch.await();
            } finally {
                getConnection().setStdinHandler(null);
            }
        } finally {
            getConnection().setAttributes(attributes);
            getConnection().setSignalHandler(prevHandler);
        }
        return key[0];
    }

    private String readPattern() throws InterruptedException, IOException {
        CountDownLatch latch = new CountDownLatch(1);
        String[] out = new String[1];
        // We need to set the interrupt SignalHandler to be aware of the interrupt
        Consumer<Signal> prevHandler = getConnection().getSignalHandler();
        getConnection().setSignalHandler((signal) -> {
            prevHandler.accept(signal);
            switch (signal) {
                case INT: {
                    latch.countDown();
                }
            }
        });
        getReadline().readline(getConnection(), new Prompt("/", (Character) null), newLine -> {
            out[0] = newLine;
            latch.countDown();
        }, null, null, searchHistory, null, RED_PATTERN_READLINE_FLAGS);
        try {
            latch.await();
        } finally {
            getConnection().setSignalHandler(prevHandler);
        }
        // At this point, if the current thread has been interrupted
        // then an exception has been thrown and we are not reaching this point.
        // Throws interruptedException and not take into account the returned null
        // value.
        if (out[0] == null) {
            throw new InterruptedException();
        }
        return out[0];
    }
}

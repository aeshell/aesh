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
package org.aesh.command.impl;

import java.util.List;

import org.aesh.command.operator.OperatorType;
import org.aesh.parser.ParsedLine;
import org.aesh.parser.ParsedWord;
import org.aesh.parser.ParserStatus;

/**
 * A {@link ParsedLine} for pre-tokenized input whose display line is
 * materialized only when actually read (#662). The success path never
 * reads it — words, offsets and the command lookup all work off the
 * token list — so the per-call character copy is skipped; diagnostics
 * and error paths materialize it on first read, byte-identical to the
 * eager form.
 * <p>
 * Every {@code ParsedLine} method that reads the display text is
 * overridden to route through {@link #line()}, so observable behavior
 * matches eager construction exactly.
 */
final class LazyParsedLine extends ParsedLine {

    private final String commandName;
    private final String[] args;
    private volatile String materialized;

    LazyParsedLine(String commandName, String[] args, List<ParsedWord> words) {
        super("", words, -1, -1, -1, ParserStatus.OK, "", OperatorType.NONE);
        this.commandName = commandName;
        // Snapshot the caller-owned array: post-call mutation must not
        // change diagnostics assembled later.
        this.args = args == null ? null : args.clone();
    }

    @Override
    public String line() {
        String line = materialized;
        if (line == null) {
            StringBuilder displayLine = new StringBuilder(commandName);
            if (args != null) {
                for (String arg : args) {
                    displayLine.append(' ').append(arg);
                }
            }
            line = displayLine.toString();
            materialized = line;
        }
        return line;
    }

    @Override
    public boolean cursorAtEnd() {
        return cursor() == line().length();
    }

    @Override
    public boolean spaceAtEnd() {
        String line = line();
        if (line.length() > 1) {
            return line.charAt(line.length() - 1) == ' ' &&
                    line.charAt(line.length() - 2) != '\\';
        } else
            return (line.length() > 0 &&
                    line.charAt(line.length() - 1) == ' ');
    }

    @Override
    public String toString() {
        return "ParsedLine{" +
                "originalInput='" + line() + '\'' +
                ", errorMessage='" + errorMessage() + '\'' +
                ", words=" + words() +
                ", status=" + status() +
                ", cursor=" + cursor() +
                ", cursorWord=" + selectedIndex() +
                ", wordCursor=" + wordCursor() +
                ", operator=" + operator() +
                '}';
    }
}

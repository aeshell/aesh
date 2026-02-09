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

package org.aesh.command.impl.completer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.console.AeshContext;
import org.aesh.terminal.formatting.TerminalString;

/**
 * A payload object to store completion values for an Option
 * Offset is only needed to change if the there is only one completion value
 * and the value is not replacing the current given value, but appending.
 * If its only appending then set the offset to the length of completeValue
 * given in OptionCompleter.complete(String completeValue)
 *
 * @author Aesh team
 */
public class CompleterData implements CompleterInvocation {

    private List<TerminalString> completerValues;
    private boolean appendSpace = true;
    private final String completeValue;
    private final Command command;
    private final AeshContext aeshContext;
    private int offset = -1;
    private boolean ignoreOffset = false;
    private boolean ignoreStartsWith = false;

    public CompleterData(AeshContext aeshContext, String completeValue, Command command) {
        this.aeshContext = aeshContext;
        this.completeValue = completeValue;
        this.command = command;
        completerValues = new ArrayList<>();
    }

    @Override
    public String getGivenCompleteValue() {
        return completeValue;
    }

    @Override
    public Command getCommand() {
        return command;
    }

    @Override
    public List<TerminalString> getCompleterValues() {
        return completerValues;
    }

    @Override
    public void setCompleterValues(Collection<String> completerValues) {
        for (String s : completerValues)
            this.completerValues.add(new TerminalString(s, true));
    }

    @Override
    public void setCompleterValuesTerminalString(List<TerminalString> completerValues) {
        this.completerValues = completerValues;
    }

    @Override
    public void clearCompleterValues() {
        this.completerValues.clear();
    }

    @Override
    public void addAllCompleterValues(Collection<String> completerValues) {
        for (String s : completerValues)
            this.completerValues.add(new TerminalString(s, true));
    }

    @Override
    public void addCompleterValue(String value) {
        this.completerValues.add(new TerminalString(value, true));
    }

    @Override
    public void addCompleterValueTerminalString(TerminalString value) {
        this.completerValues.add(value);
    }

    @Override
    public boolean isAppendSpace() {
        return appendSpace;
    }

    @Override
    public void setAppendSpace(boolean appendSpace) {
        this.appendSpace = appendSpace;
    }

    @Override
    public boolean doIgnoreOffset() {
        return ignoreOffset;
    }

    @Override
    public void setIgnoreOffset(boolean ignoreOffset) {
        this.ignoreOffset = ignoreOffset;
    }

    @Override
    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public void setIgnoreStartsWith(boolean ignoreStartsWith) {
        this.ignoreStartsWith = ignoreStartsWith;
    }

    @Override
    public boolean isIgnoreStartsWith() {
        return ignoreStartsWith;
    }

    @Override
    public AeshContext getAeshContext() {
        return aeshContext;
    }
}

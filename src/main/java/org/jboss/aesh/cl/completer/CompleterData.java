/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.completer;

import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.completer.CompleterInvocation;
import org.jboss.aesh.terminal.TerminalString;

import java.util.ArrayList;
import java.util.List;

/**
 * A payload object to store completion values for an Option
 * Offset is only needed to change if the there is only one completion value
 * and the value is not replacing the current given value, but appending.
 * If its only appending then set the offset to the length of completeValue
 * given in OptionCompleter.complete(String completeValue)
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CompleterData implements CompleterInvocation {

    private List<TerminalString> completerValues;
    private boolean appendSpace = true;
    private String completeValue;
    private Command command;
    private AeshContext aeshContext;
    private int offset;
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
    public void setCompleterValues(List<String> completerValues) {
        for(String s : completerValues)
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
    public void addAllCompleterValues(List<String> completerValues) {
        for(String s : completerValues)
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

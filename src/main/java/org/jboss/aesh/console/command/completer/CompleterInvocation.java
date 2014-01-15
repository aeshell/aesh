/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.command.completer;

import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.terminal.TerminalString;

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
public interface CompleterInvocation {


    String getGivenCompleteValue();

    Command getCommand();

    List<TerminalString> getCompleterValues();

    void setCompleterValues(List<String> completerValues);

    void setCompleterValuesTerminalString(List<TerminalString> completerValues);

    void clearCompleterValues();

    void addAllCompleterValues(List<String> completerValues);

    void addCompleterValue(String value);

    void addCompleterValueTerminalString(TerminalString value);

    boolean isAppendSpace();

    void setAppendSpace(boolean appendSpace);

    void setIgnoreOffset(boolean ignoreOffset);

    boolean doIgnoreOffset();

    void setOffset(int offset);

    int getOffset();

    void setIgnoreStartsWith(boolean ignoreStartsWith);

    boolean isIgnoreStartsWith();

    AeshContext getAeshContext();
}

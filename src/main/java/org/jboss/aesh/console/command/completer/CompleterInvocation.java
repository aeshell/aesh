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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ackage org.jboss.aesh.console.command.completer;

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

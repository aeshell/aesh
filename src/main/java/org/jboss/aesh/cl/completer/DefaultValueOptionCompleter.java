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
 */
package org.jboss.aesh.cl.completer;

import org.jboss.aesh.console.command.completer.CompleterInvocation;
import org.jboss.aesh.parser.Parser;

import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class DefaultValueOptionCompleter implements OptionCompleter<CompleterInvocation> {

    private final List<String> defaultValues;

    public DefaultValueOptionCompleter(List<String> defaultValues) {
        this.defaultValues = defaultValues;
    }

    @Override
    public void complete(CompleterInvocation completerData) {
        completeDataWithoutValues(completerData);
        completeDataWithValues(completerData);
    }

    private void completeDataWithoutValues(CompleterInvocation completerData) {
        if(completerData.getGivenCompleteValue() == null ||
                completerData.getGivenCompleteValue().length() == 0) {
            completerData.addAllCompleterValues(defaultValues);
            return;
        }

        for(String value : defaultValues) {
            if(value.startsWith(completerData.getGivenCompleteValue())) {
                completerData.addCompleterValue(value);
            }
        }
    }

    private void completeDataWithValues(CompleterInvocation completerData) {
        if(completerData.getCompleterValues().size() == 1 &&
                completerData.getCompleterValues().get(0).containSpaces()) {
            String tmpData = Parser.switchSpacesToEscapedSpacesInWord(
                    completerData.getCompleterValues().get(0).getCharacters());
            completerData.clearCompleterValues();
            completerData.addCompleterValue(tmpData);
            completerData.setAppendSpace(true);
        }
    }
}

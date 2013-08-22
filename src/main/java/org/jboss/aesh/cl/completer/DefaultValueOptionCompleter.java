/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.completer;

import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class DefaultValueOptionCompleter implements OptionCompleter {

    private final List<String> defaultValues;

    public DefaultValueOptionCompleter(List<String> defaultValues) {
        this.defaultValues = defaultValues;
    }

    @Override
    public CompleterData complete(String completeValue) {
        CompleterData completerData = new CompleterData();
        if(completeValue == null || completeValue.length() == 0)
            completerData.addCompleterValue(defaultValues.get(0));
        else {
            for(String value : defaultValues) {
                if(value.startsWith(completeValue))
                    completerData.addCompleterValue(value);
            }
        }

        return completerData;
    }
}

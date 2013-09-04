/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.completer;


/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class BooleanOptionCompleter implements OptionCompleter {
    @Override
    public void complete(CompleterData completerData) {
        if(completerData.getGivenCompleteValue().length() == 0) {
            completerData.addCompleterValue("true");
            completerData.addCompleterValue("false");
        }
        else if("true".startsWith( completerData.getGivenCompleteValue().toLowerCase())) {
            completerData.addCompleterValue("true");

        }
        else if("false".startsWith( completerData.getGivenCompleteValue().toLowerCase())) {
            completerData.addCompleterValue("false");
        }
    }
}

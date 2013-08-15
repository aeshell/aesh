/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.completer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class BooleanOptionCompleter implements OptionCompleter {
    @Override
    public List<String> complete(String completeValue) {
        List<String> completeList = new ArrayList<String>();
        if("true".startsWith( completeValue.toLowerCase()))
             completeList.add("true");
        else if("false".startsWith( completeValue.toLowerCase()))
            completeList.add("false");

        return completeList;
    }
}

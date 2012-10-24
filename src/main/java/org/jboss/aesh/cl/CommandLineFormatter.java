/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.internal.OptionInt;
import org.jboss.aesh.cl.internal.ParameterInt;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineFormatter {

    private int width = 80;

    public String printHelp(ParameterInt parameter) {
        int maxLength = 0;
        for(OptionInt o : parameter.getOptions())
        if(o.getFormattedLength() > maxLength)
            maxLength = o.getFormattedLength();

        StringBuilder sb = new StringBuilder();
        for(OptionInt o : parameter.getOptions())
            sb.append(o.getFormattedOption(0, maxLength+2, width)).append("\n");
        return "Usage: "+parameter.getName()+" "+parameter.getUsage()+"\n"+sb.toString();
    }

}

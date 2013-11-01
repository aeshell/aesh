/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.man.parser;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.parser.Parser;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ManParameter {

    List<String> out = new ArrayList<String>();
    private static final int padding = 4;
    private static String argPad = "  ";
    private static String textPad = "    ";

    /**
     * First line is the param/option name
     * following lines are the description
     */
    public ManParameter parseParams(List<String> input, int columns) {
        out.add(argPad+ManParserUtil.convertStringToAnsi(input.get(0)));
        input.remove(0);
        if(!input.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for(String in : input) {
               if(in.trim().length() > 0)
                   builder.append(in.trim()).append(' ');
            }

            if(builder.length() > 0) {
               for(String s : Parser.splitBySizeKeepWords(builder.toString(), columns - textPad.length())) {
                  out.add(textPad+ManParserUtil.convertStringToAnsi(s));
               }
            }
            //add an empty line at the bottom to create a line separator between params
            if(out.size() > 0)
                out.add(" ");
        }
        return this;
    }

    public List<String> getAsList() {
        return out;
    }

    public String printToTerminal() {
        StringBuilder builder = new StringBuilder();
        for(String s : out)
            builder.append(s).append(Config.getLineSeparator());

        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ManParameter)) return false;

        ManParameter that = (ManParameter) o;

        if (out != null ? !out.equals(that.out) : that.out != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return out != null ? out.hashCode() : 0;
    }
}

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
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.man.parser;

import org.aesh.terminal.utils.Config;
import org.aesh.readline.util.Parser;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ManParameter {

    final List<String> out = new ArrayList<>();
    private static final String argPad = "  ";
    private static final String textPad = "    ";

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

        return !(out != null ? !out.equals(that.out) : that.out != null);

    }

    @Override
    public int hashCode() {
        return out != null ? out.hashCode() : 0;
    }
}

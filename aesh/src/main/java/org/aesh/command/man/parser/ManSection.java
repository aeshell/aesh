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

import org.aesh.terminal.utils.ANSI;
import org.aesh.terminal.utils.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ManSection {

    private String name;
    private final List<ManParameter> parameters;

    public ManSection() {
        parameters = new ArrayList<ManParameter>();
    }

    public ManSection parseSection(List<String> input, int columns) {
        //we ignore the links atm
        if(input.get(0).startsWith("[["))
            input.remove(0);
        //first line should be the name
        name = input.get(0);
        input.remove(0);
        //the first section, ignoring it for now
        //starting a new section
        if(input.get(0).startsWith("-") &&
                input.get(0).trim().length() == name.length()) {
            input.remove(0);
            //a new param
            List<String> newOption = new ArrayList<String>();
            boolean startingNewOption = false;
            boolean paramName = false;
            for(String in : input) {
                if(in.trim().length() > 0)
                    newOption.add(in);
                else {
                    if(newOption.size() > 0) {
                        parameters.add(new ManParameter().parseParams(newOption, columns));
                        newOption.clear();
                    }
                }
            }
            if(!newOption.isEmpty())
                parameters.add(new ManParameter().parseParams(newOption, columns));
        }

        return this;
    }

    public String getName() {
        return name;
    }

    public List<ManParameter> getParameters() {
        return parameters;
    }

    public List<String> getAsList() {
        List<String> out = new ArrayList<String>();
        out.add(ANSI.BOLD+name+ ANSI.DEFAULT_TEXT);
        for(ManParameter param : parameters)
            out.addAll(param.getAsList());

        // add an empty line as line separator between sections
        out.add(" ");
        return out;
    }

    public String printToTerminal() {
        StringBuilder builder = new StringBuilder();
        builder.append(ANSI.BOLD).append(name).append(ANSI.DEFAULT_TEXT);
        builder.append(Config.getLineSeparator());
        for(ManParameter param : parameters)
            builder.append(param.printToTerminal());

        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ManSection)) return false;

        ManSection that = (ManSection) o;

        return name.equals(that.name) && parameters.equals(that.parameters);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + parameters.hashCode();
        return result;
    }
}

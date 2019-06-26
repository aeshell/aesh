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

import org.aesh.command.man.FileParser;
import org.aesh.terminal.utils.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Read a asciidoc file and parse it to something that can be
 * displayed nicely in a terminal.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ManFileParser implements FileParser {

    private final List<ManSection> sections;
    private String name;
    private InputStreamReader reader;

    public ManFileParser() {
        sections = new ArrayList<ManSection>();
    }

    public void setInput(InputStream input) throws IOException {
        if(input != null) {
            reader = new InputStreamReader(input);
            this.name = null;
            sections.clear();
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public List<String> loadPage(int columns) throws IOException {
        //we already have the file loaded
        if(!sections.isEmpty())
            return getAsList();
        if(reader == null)
            throw new IOException("InputStreamReader is null, cannot read file.");
        //parse the file
        try (BufferedReader br = new BufferedReader(reader)) {
            String line = br.readLine();
            boolean foundHeader = false;
            boolean foundEmptyLine = true;
            List<String> section = new ArrayList<String>();
            while (line != null) {
                if(line.trim().isEmpty() && !foundEmptyLine) {
                    foundEmptyLine = true;
                    section.add(line);
                }
                //found two empty lines create a new section
                else if(line.isEmpty() && foundEmptyLine) {
                    if(!foundHeader) {
                        processHeader(section, columns);
                        foundHeader = true;
                    }
                    else {
                        ManSection manSection = new ManSection().parseSection(section, columns);
                        sections.add(manSection);
                    }
                    foundEmptyLine = false;
                    section.clear();
                }
                //add line to section
                else {
                    if(foundEmptyLine)
                        foundEmptyLine = false;
                    section.add(line);
                }

                line = br.readLine();
            }
            if(!section.isEmpty()) {
                ManSection manSection = new ManSection().parseSection(section, columns);
                sections.add(manSection);
            }
            return getAsList();
        }
    }

    private void processHeader(List<String> header, int columns) throws IOException {
        if(header.size() != 4)
            throw new IOException("File did not include the correct header.");
        name = header.get(0);
        if(!header.get(2).equals(":doctype: manpage"))
            throw new IOException("File did not include the correct header: \":doctype: manpage\"");
    }

    public List<ManSection> getSections() {
        return sections;
    }

    public List<String> getAsList() {
        List<String> out = new ArrayList<String>();
        for(ManSection section : sections)
            out.addAll(section.getAsList());

        return out;
    }

    public String print() {
        StringBuilder builder = new StringBuilder();
        for(ManSection section : sections) {
            builder.append(section.printToTerminal()).append(Config.getLineSeparator());
        }

        return builder.toString();
    }
}

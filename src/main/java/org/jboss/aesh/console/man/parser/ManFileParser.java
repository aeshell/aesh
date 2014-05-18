/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.man.parser;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.man.FileParser;

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

    private List<ManSection> sections;
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

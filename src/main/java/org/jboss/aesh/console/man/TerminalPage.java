/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.man;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * TerminalPage parse files or input string and prepare it to be displayed in a term
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TerminalPage {

    private List<String> lines;
    private FileParser fileParser;

    public TerminalPage(FileParser fileParser, int columns) throws IOException {
       this.fileParser = fileParser;
        lines = fileParser.loadPage(columns);
    }

    public String getLine(int num) {
        if(num < lines.size())
            return lines.get(num);
        else
            return "";
    }

    public List<Integer> findWord(String word) {
        List<Integer> wordLines = new ArrayList<Integer>();
        for(int i=0; i < lines.size();i++) {
            if(lines.get(i).contains(word))
                wordLines.add(i);
        }
        return wordLines;
    }

    public int size() {
        return lines.size();
    }

    public String getFileName() {
        return fileParser.getName();
    }

    public List<String> getLines() {
        return lines;
    }

    public boolean hasData() {
        return !lines.isEmpty();
    }

    public void clear() {
        lines.clear();
    }

    public static enum Search {
        SEARCHING,
        RESULT,
        NOT_FOUND,
        NO_SEARCH
    }

}

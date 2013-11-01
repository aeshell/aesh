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
 * Page parse files or input string and prepare it to be displayed in a term
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class Page {

    private List<String> lines;
    private PageLoader loader;

    public Page(PageLoader loader, int columns) {
       this.loader = loader;
        try {
            lines = loader.loadPage(columns);
        }
        catch(IOException ioe) {
            //need to properly log this
            ioe.printStackTrace();
        }
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
        return loader.getResourceName();
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

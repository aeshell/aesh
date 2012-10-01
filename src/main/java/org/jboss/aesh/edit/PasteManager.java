/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit;

import java.util.ArrayList;
import java.util.List;

/**
 * Keep track of edits for paste
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class PasteManager {

    private static final int PASTE_SIZE = 10;
    private List<StringBuilder> pasteStack;

    public PasteManager() {
        pasteStack = new ArrayList<StringBuilder>(PASTE_SIZE);
    }

    public void addText(StringBuilder buffer) {
        checkSize();
        pasteStack.add(buffer);
    }

    private void checkSize() {
        if(pasteStack.size() >= PASTE_SIZE) {
            pasteStack.remove(0);
        }
    }

    public StringBuilder get(int index) {
        if(index < pasteStack.size())
            return pasteStack.get((pasteStack.size()-index-1));
        else
            return pasteStack.get(0);
    }
}

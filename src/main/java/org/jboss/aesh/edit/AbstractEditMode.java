/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit;

import org.jboss.aesh.console.Console;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class AbstractEditMode implements EditMode {

    private Console console;

    private static final String IGNOREEOF = "IGNOREEOF";
    //counting how many times eof been pressed
    protected int eofCounter;
    //default value
    protected int ignoreEof = 0;

    private boolean askForCompletions = false;

    @Override
    public void init(final Console console) {
        this.console = console;
        eofCounter = 0;
    }

    @Override
    public void setAskForCompletions(boolean askForCompletions) {
        this.askForCompletions = askForCompletions;
    }

    protected boolean isAskingForCompletions() {
        return askForCompletions;
    }

    protected void checkEof() {
        String strValue = "1";
        if(console != null && console.getExportManager() != null)
            strValue = console.getExportManager().getValueIgnoreCase(IGNOREEOF);
        try {
            int eofValue = Integer.parseInt(strValue);
            if(eofValue > -1)
                ignoreEof = eofValue;
            else
                ignoreEof = 0; // standard value
        }
        catch (NumberFormatException ignored) {
        }
    }

}

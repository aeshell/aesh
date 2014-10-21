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

    @Override
    public void resetEOF() {
        this.eofCounter = 0;
    }
}

/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class AbstractTerminal implements Terminal {

    @Override
    public void writeChar(TerminalCharacter character) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(character.getTextType())
                .append(character.getBackgroundColor().getBackgroundColor())
                .append(character.getForegroundColor().getForegroundColor())
                .append(character.getCharacter());

        writeToStdOut(builder.toString());
    }

    @Override
    public void writeChars(List<TerminalCharacter> chars) throws IOException {
        StringBuilder builder = new StringBuilder();
        for(TerminalCharacter c : chars) {
            builder.append(c.getTextType())
                    .append(c.getBackgroundColor().getBackgroundColor())
                    .append(c.getForegroundColor().getForegroundColor())
                    .append(c.getCharacter());
        }
        writeToStdOut(builder.toString());
    }

}

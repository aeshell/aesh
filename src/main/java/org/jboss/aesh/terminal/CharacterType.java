/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

/**
 * Define what kind of character type to display
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public enum CharacterType {
    NORMAL(0),
    BOLD(1),
    PLAIN(2),
    ITALIC(3),
    UNDERLINE(4),
    BLINK(5),
    INVERT(7),
    CROSSED_OUT(9);

    private int value;
    private static int BOLD_OFF = 22;
    private static int ITALIC_OFF = 23;
    private static int UNDERLINE_OFF = 24;
    private static int BLINK_OFF = 25;
    private static int INVERT_OFF = 27;
    private static int CROSSED_OUT_OFF = 29;

    CharacterType(int c) {
        this.value = c;
    }

    public int getValue() {
        return value;
    }

    public String getValueComparedToPrev(CharacterType prev) {
        StringBuilder builder = new StringBuilder();
        if(this != prev) {
            if(prev == BOLD || prev == PLAIN)
                builder.append(BOLD_OFF).append(';');
            else if(prev == UNDERLINE)
                builder.append(UNDERLINE_OFF).append(';');
            else if(prev == ITALIC)
                builder.append(ITALIC_OFF).append(';');
            else if(prev == BLINK)
                builder.append(BLINK_OFF).append(';');
            else if(prev == INVERT)
                builder.append(INVERT_OFF).append(';');
            else if(prev == CROSSED_OUT)
                builder.append(CROSSED_OUT_OFF).append(';');
        }
        builder.append(this.getValue());

        return builder.toString();
    }
}

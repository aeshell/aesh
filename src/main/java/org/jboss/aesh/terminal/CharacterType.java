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
    BOLD(1),
    FAINT(2),
    ITALIC(3),
    UNDERLINE(4),
    BLINK(5),
    INVERT(7),
    CONCEAL(8),
    CROSSED_OUT(9);

    private int value;

    CharacterType(int c) {
        this.value = c;
    }

    public int getValue() {
        return value;
    }

}

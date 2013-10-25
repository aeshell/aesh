/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public enum Color {

    BLACK(0),
    RED(1),
    GREEN(2),
    YELLOW(3),
    BLUE(4),
    MAGENTA(5),
    CYAN(6),
    WHITE(7),
    DEFAULT(9);

    private int value;

    Color(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public enum Intensity {
        NORMAL,
        BRIGHT;

        public int getValue(Type type) {
            if(this == NORMAL) {
                if(type == Type.FOREGROUND)
                    return 3;
                else
                    return 4;
            }
            else {
               if(type == Type.FOREGROUND)
                   return 9;
                else
                   return 10;
            }
        }
    }

    public enum Type {
        FOREGROUND, // 3
        BACKGROUND // 4
    }
}

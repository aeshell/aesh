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

    DEFAULT_TEXT(39),
    BLACK_TEXT(30),
    RED_TEXT(31),
    GREEN_TEXT(32),
    YELLOW_TEXT(33),
    BLUE_TEXT(34),
    MAGENTA_TEXT(35),
    CYAN_TEXT(36),
    WHITE_TEXT(37),
    DEFAULT_BG(49),
    BLACK_BG(40),
    RED_BG(41),
    GREEN_BG(42),
    YELLOW_BG(43),
    BLUE_BG(44),
    MAGENTA_BG(45),
    CYAN_BG(46),
    WHITE_BG(47);

    private int value;

    Color(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

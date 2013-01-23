/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import org.jboss.aesh.util.ANSI;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public enum Color {

    DEFAULT,
    BLACK,
    WHITE,
    RED,
    GREEN,
    YELLOW,
    BLUE,
    CYAN;

    public String getBackgroundColor() {
        if(this == DEFAULT)
            return ANSI.defaultBackground();
        else if(this == BLACK)
            return ANSI.blackBackground();
        else if(this == WHITE)
            return ANSI.whiteBackground();
        else if(this == RED)
            return ANSI.redBackground();
        else if(this == GREEN)
            return ANSI.greenBackground();
        else if(this == YELLOW)
            return ANSI.yellowBackground();
        else if(this == BLUE)
            return ANSI.blueBackground();
        else if(this == CYAN)
            return ANSI.cyanBackground();
        else
            return ANSI.reset();
    }

    public String getForegroundColor() {
        if(this == DEFAULT)
            return ANSI.defaultText();
        else if(this == BLACK)
            return ANSI.blackText();
        else if(this == WHITE)
            return ANSI.whiteText();
        else if(this == RED)
            return ANSI.redText();
        else if(this == GREEN)
            return ANSI.greenText();
        else if(this == YELLOW)
            return ANSI.yellowText();
        else if(this == BLUE)
            return ANSI.blueText();
        else if(this == CYAN)
            return ANSI.cyanText();
        else
            return ANSI.reset();
    }

}

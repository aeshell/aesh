/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import org.jboss.aesh.console.Buffer;

/**
 * Utility class to provide ANSI codes for different operations
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class ANSI {

    private static String BLACK_TEXT = "\u001B[0;30m";
    private static String RED_TEXT = "\u001B[0;31m";
    private static String GREEN_TEXT = "\u001B[0;32m";
    private static String YELLOW_TEXT = "\u001B[0;33m";
    private static String BLUE_TEXT = "\u001B[0;34m";
    private static String MAGENTA_TEXT = "\u001B[0;35m";
    private static String CYAN_TEXT = "\u001B[0;36m";
    private static String WHITE_TEXT = "\u001B[0;37m";

    private static String BLACK_BG = "\u001B[0;40m";
    private static String RED_BG = "\u001B[0;41m";
    private static String GREEN_BG = "\u001B[0;42m";
    private static String YELLOW_BG = "\u001B[0;43m";
    private static String BLUE_BG = "\u001B[0;44m";
    private static String MAGENTA_BG = "\u001B[0;45m";
    private static String CYAN_BG = "\u001B[0;46m";
    private static String WHITE_BG = "\u001B[0;47m";
    private static String ALTERNATE_BUFFER =  "\u001B[?1049h";
    private static String MAIN_BUFFER =  "\u001B[?1049l";
    private static String INVERT_BACKGROUND =  "\u001B[7m";
    private static String NORMAL_BACKGROUND =  "\u001B[27m";
    private static String RESET = "\u001B[0;0m";
    private static String BOLD = "\u001B[0;1m";
    private static String CURSOR_START = "\u001B[1G";
    private static String CURSOR_ROW = "\u001B[6n";
    private static String CLEAR_SCREEN = "\u001B[2J";

    private ANSI() {
    }

    public static String blackText() {
        return BLACK_TEXT;
    }

    public static String redText() {
        return RED_TEXT;
    }

    public static String greenText() {
        return GREEN_TEXT;
    }

    public static String yellowText() {
        return YELLOW_TEXT;
    }

    public static String blueText() {
        return BLUE_TEXT;
    }

    public static String magentaText() {
        return MAGENTA_TEXT;
    }

    public static String  cyanText() {
        return CYAN_TEXT;
    }

    public static String whiteText() {
        return WHITE_TEXT;
    }

    public static String blackBackground() {
        return BLACK_BG;
    }

    public static String redBackground() {
        return RED_BG;
    }

    public static String greenBackground() {
        return GREEN_BG;
    }

    public static String yellowBackground() {
        return YELLOW_BG;
    }

    public static String blueBackground() {
        return BLUE_BG;
    }

    public static String magentaBackground() {
        return MAGENTA_BG;
    }

    public static String cyanBackground() {
        return CYAN_BG;
    }

    public static String whiteBackground() {
        return WHITE_BG;
    }

    public static String reset() {
        return RESET;
    }

    public static String getAlternateBufferScreen() {
       return ALTERNATE_BUFFER;
    }

    public static String getMainBufferScreen() {
        return MAIN_BUFFER;
    }

    public static String getInvertedBackground() {
        return INVERT_BACKGROUND;
    }

    public static String getNormalBackground() {
        return NORMAL_BACKGROUND;
    }

    public static String getBold() {
        return BOLD;
    }

    public static String moveCursorToBeginningOfLine() {
        return CURSOR_START;
    }

    public static String getCurrentCursorPos() {
       return CURSOR_ROW;
    }

    public static String clearScreen() {
        return CLEAR_SCREEN;
    }
}

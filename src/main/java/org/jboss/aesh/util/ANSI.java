/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.terminal.InfocmpManager;

/**
 * Utility class to provide ANSI codes for different operations
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class ANSI {

    private static final String START = "\u001B[";
    private static final String BLACK_TEXT = "\u001B[0;30m";
    private static final String RED_TEXT = "\u001B[0;31m";
    private static final String GREEN_TEXT = "\u001B[0;32m";
    private static final String YELLOW_TEXT = "\u001B[0;33m";
    private static final String BLUE_TEXT = "\u001B[0;34m";
    private static final String MAGENTA_TEXT = "\u001B[0;35m";
    private static final String CYAN_TEXT = "\u001B[0;36m";
    private static final String WHITE_TEXT = "\u001B[0;37m";
    private static final String DEFAULT_TEXT = "\u001B[0;39m";

    private static final String BLACK_BG = "\u001B[0;40m";
    private static final String RED_BG = "\u001B[0;41m";
    private static final String GREEN_BG = "\u001B[0;42m";
    private static final String YELLOW_BG = "\u001B[0;43m";
    private static final String BLUE_BG = "\u001B[0;44m";
    private static final String MAGENTA_BG = "\u001B[0;45m";
    private static final String CYAN_BG = "\u001B[0;46m";
    private static final String WHITE_BG = "\u001B[0;47m";
    private static final String DEFAULT_BG = "\u001B[0;49m";
    private static final String ALTERNATE_BUFFER = Config.isOSPOSIXCompatible() ?
            InfocmpManager.alternateBuffer() : "\u001B[?1049h";
    private static final String MAIN_BUFFER = Config.isOSPOSIXCompatible() ?
            InfocmpManager.mainBuffer() : "\u001B[?1049l";
    private static final String INVERT_BACKGROUND = Config.isOSPOSIXCompatible() ?
            InfocmpManager.invertBackground() : "\u001B[7m";
    private static final String NORMAL_BACKGROUND = Config.isOSPOSIXCompatible() ?
            InfocmpManager.normalBackground() : "\u001B[27m";
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = Config.isOSPOSIXCompatible() ?
            InfocmpManager.enableBold() : "\u001B[0;1m";
    private static final String BOLD_OFF = "\u001B[0;22m";
    private static final String UNDERLINE = Config.isOSPOSIXCompatible() ?
            InfocmpManager.enableUnderline() : "\u001B[0;4m";
    private static final String UNDERLINE_OFF = Config.isOSPOSIXCompatible() ?
            InfocmpManager.disableUnderline() : "\u001B[0;24m";
    private static final String BLINK = Config.isOSPOSIXCompatible() ?
            InfocmpManager.enableBlink() : "\u001B[5m";
    private static final String BLINK_OFF = "\u001B[25m";
    private static final String CURSOR_START = "\u001B[1G";
    private static final String CURSOR_ROW = "\u001B[6n";
    private static final String CLEAR_SCREEN = Config.isOSPOSIXCompatible() ?
            InfocmpManager.clearScreen() : "\u001B[2J";
    private static final String CURSOR_SAVE = Config.isOSPOSIXCompatible() ?
            InfocmpManager.saveCursor() : "\u001B[s";
    private static final String CURSOR_RESTORE = Config.isOSPOSIXCompatible() ?
            InfocmpManager.restoreCursor() : "\u001B[u";
    private static final String CURSOR_HIDE = "\u001B[?25l";
    private static final String CURSOR_SHOW = "\u001B[?25h";

    private ANSI() {
    }

    public static String getStart() {
        return START;
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

    public static String defaultText() {
        return DEFAULT_TEXT;
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

    public static String defaultBackground() {
        return DEFAULT_BG;
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

    public static String getBoldOff() {
        return BOLD_OFF;
    }

    public static String getUnderline() {
        return UNDERLINE;
    }

    public static String getUnderlineOff() {
        return UNDERLINE_OFF;
    }

    public static String getBlink() {
        return BLINK;
    }

    public static String getBlinkOff() {
        return BLINK_OFF;
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

    public static String saveCursor() {
        return CURSOR_SAVE;
    }

    public static String restoreCursor() {
        return CURSOR_RESTORE;
    }

    public static String hideCursor() {
        return CURSOR_HIDE;
    }

    public static String showCursor() {
        return CURSOR_SHOW;
    }

    public static String moveCursor(int x, int y) {
       return new StringBuilder().append(START).append(y).append(';').append(x).append('H').toString();
    }
}

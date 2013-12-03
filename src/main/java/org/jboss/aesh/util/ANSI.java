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

    private static String START = "\u001B[";
    private static String BLACK_TEXT = "\u001B[0;30m";
    private static String RED_TEXT = "\u001B[0;31m";
    private static String GREEN_TEXT = "\u001B[0;32m";
    private static String YELLOW_TEXT = "\u001B[0;33m";
    private static String BLUE_TEXT = "\u001B[0;34m";
    private static String MAGENTA_TEXT = "\u001B[0;35m";
    private static String CYAN_TEXT = "\u001B[0;36m";
    private static String WHITE_TEXT = "\u001B[0;37m";
    private static String DEFAULT_TEXT = "\u001B[0;39m";

    private static String BLACK_BG = "\u001B[0;40m";
    private static String RED_BG = "\u001B[0;41m";
    private static String GREEN_BG = "\u001B[0;42m";
    private static String YELLOW_BG = "\u001B[0;43m";
    private static String BLUE_BG = "\u001B[0;44m";
    private static String MAGENTA_BG = "\u001B[0;45m";
    private static String CYAN_BG = "\u001B[0;46m";
    private static String WHITE_BG = "\u001B[0;47m";
    private static String DEFAULT_BG = "\u001B[0;49m";
    private static String ALTERNATE_BUFFER = Config.isOSPOSIXCompatible() ?
            InfocmpManager.alternateBuffer() : "\u001B[?1049h";
    private static String MAIN_BUFFER = Config.isOSPOSIXCompatible() ?
            InfocmpManager.mainBuffer() : "\u001B[?1049l";
    private static String INVERT_BACKGROUND = Config.isOSPOSIXCompatible() ?
            InfocmpManager.invertBackground() : "\u001B[7m";
    private static String NORMAL_BACKGROUND = Config.isOSPOSIXCompatible() ?
            InfocmpManager.normalBackground() : "\u001B[27m";
    private static String RESET = "\u001B[0m";
    private static String BOLD = Config.isOSPOSIXCompatible() ?
            InfocmpManager.enableBold() : "\u001B[0;1m";
    private static String BOLD_OFF = "\u001B[0;22m";
    private static String UNDERLINE = Config.isOSPOSIXCompatible() ?
            InfocmpManager.enableUnderline() : "\u001B[0;4m";
    private static String UNDERLINE_OFF = Config.isOSPOSIXCompatible() ?
            InfocmpManager.disableUnderline() : "\u001B[0;24m";
    private static String BLINK = Config.isOSPOSIXCompatible() ?
            InfocmpManager.enableBlink() : "\u001B[5m";
    private static String BLINK_OFF = "\u001B[25m";
    private static String CURSOR_START = "\u001B[1G";
    private static String CURSOR_ROW = "\u001B[6n";
    private static String CLEAR_SCREEN = Config.isOSPOSIXCompatible() ?
            InfocmpManager.clearScreen() : "\u001B[2J";
    private static String CURSOR_SAVE = Config.isOSPOSIXCompatible() ?
            InfocmpManager.saveCursor() : "\u001B[s";
    private static String CURSOR_RESTORE = Config.isOSPOSIXCompatible() ?
            InfocmpManager.restoreCursor() : "\u001B[u";
    private static String CURSOR_HIDE = "\u001B[?25l";
    private static String CURSOR_SHOW = "\u001B[?25h";

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

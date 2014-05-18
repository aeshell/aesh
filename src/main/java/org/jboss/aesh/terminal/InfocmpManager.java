/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import org.jboss.aesh.util.LoggerUtil;

import java.util.logging.Logger;

/**
 * Fetch the values parsed from "infocmp".
 * The keys are defined in term.h
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class InfocmpManager {

    private static final Logger LOGGER = LoggerUtil.getLogger(InfocmpManager.class.getName());

    public static int[] getKeyHome() {
        int[] home = InfocmpHandler.getInstance().getAsInts("khome");
        if(home.length == 0) {
            LOGGER.warning("Failed to get key home from infocmp, using default");
            return new int[]{27,79,72}; //use default value
        }
        else
            return home;
    }

    public static int[] getCursorHome() {
        int[] home = InfocmpHandler.getInstance().getAsInts("home");
        if(home.length == 0) {
            LOGGER.warning("Failed to get cursor home from infocmp, using default");
            return new int[]{27,91,72}; //use default value
        }
        else
            return home;
    }

    public static int[] getEnd() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("end");
        if(infocmpValue.length == 0) {
            LOGGER.warning("Failed to get end from infocmp, using default");
            return new int[]{27,79,70}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getPgUp() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("kpp");
        if(infocmpValue.length == 0) {
            LOGGER.warning("Failed to get pgup from infocmp, using default");
            return new int[]{27,91,53,126}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getPgDown() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("knp");
        if(infocmpValue.length == 0) {
            LOGGER.warning("Failed to get pgdown from infocmp, using default");
            return new int[]{27,91,54,126}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getLeft() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("kcub1");
        if(infocmpValue.length == 0) {
            LOGGER.warning("Failed to get left from infocmp, using default");
            return new int[]{27,79,68}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getRight() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("cuf1");
        if(infocmpValue.length == 0) {
            LOGGER.warning("Failed to get right from infocmp, using default");
            return new int[]{27,79,68}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getUp() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("kcuu1");
        if(infocmpValue.length == 0) {
            LOGGER.warning("Failed to get up from infocmp, using default");
            return new int[]{27,79,65}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getDown() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("kcud1");
        if(infocmpValue.length == 0) {
            LOGGER.warning("Failed to get down from infocmp, using default");
            return new int[]{27,79,66}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getIns() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("kich1");
        if(infocmpValue.length == 0) {
            LOGGER.warning("Failed to get insert from infocmp, using default");
            return new int[]{27,91,50,126}; //use default value
        }
        else
            return infocmpValue;

    }

    public static int[] getDelete() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("kdch1");
        if(infocmpValue.length == 0) {
            LOGGER.warning("Failed to get delete from infocmp, using default");
            return new int[]{27,91,51,126}; //use default value
        }
        else
            return infocmpValue;
    }

    public static String saveCursor() {
        String cursor = InfocmpHandler.getInstance().get("sc");
        if(cursor.length() == 0) {
            LOGGER.warning("Failed to get save_cursor from infocmp, using default");
            return "\u001B[s";
        }
        else
            return cursor;
    }

    public static String restoreCursor() {
        String cursor = InfocmpHandler.getInstance().get("rc");
        if(cursor.length() == 0) {
            LOGGER.warning("Failed to get restore_cursor from infocmp, using default");
            return "\u001B[u";
        }
        else
            return cursor;
    }

    public static String clearScreen() {
        String clear = InfocmpHandler.getInstance().get("clear");
        if(clear.length() == 0) {
            LOGGER.warning("Failed to get clear from infocmp, using default");
            return "\u001B[2J";
        }
        else
            return clear;
    }

    public static String alternateBuffer() {
        String buffer = InfocmpHandler.getInstance().get("smcup");
        if(buffer.length() == 0) {
            LOGGER.warning("Failed to get alternate buffer from infocmp, using default");
            return "\u001B[?1049h";
        }
        else
            return buffer;
    }

    public static String mainBuffer() {
        String buffer = InfocmpHandler.getInstance().get("rmcup");
        if(buffer.length() == 0) {
            LOGGER.warning("Failed to get main buffer from infocmp, using default");
            return "\u001B[?1049l";
        }
        else
            return buffer;
    }

    public static String invertBackground() {
        String buffer = InfocmpHandler.getInstance().get("smso");
        if(buffer.length() == 0) {
            LOGGER.warning("Failed to invert background from infocmp, using default");
            return "\u001B[7m";
        }
        else
            return buffer;
    }

    public static String normalBackground() {
        String buffer = InfocmpHandler.getInstance().get("rmso");
        if(buffer.length() == 0) {
            LOGGER.warning("Failed to reset to normal background from infocmp, using default");
            return "\u001B[27m";
        }
        else
            return buffer;
    }

    public static String enableBold() {
        String bold = InfocmpHandler.getInstance().get("bold");
        if(bold.length() == 0) {
            LOGGER.warning("Failed to get bold from infocmp, using default");
            return "\u001B[0;1m";
        }
        else
            return bold;
    }

    public static String enableUnderline() {
        String underline = InfocmpHandler.getInstance().get("smul");
        if(underline.length() == 0) {
            LOGGER.warning("Failed to get underline from infocmp, using default");
            return "\u001B[0;4m";
        }
        else
            return underline;
    }

    public static String disableUnderline() {
        String underline = InfocmpHandler.getInstance().get("rmul");
        if(underline.length() == 0) {
            LOGGER.warning("Failed to exit underline from infocmp, using default");
            return "\u001B[0;24m";
        }
        else
            return underline;
    }

    public static String enableBlink() {
        String blink = InfocmpHandler.getInstance().get("blink");
        if(blink.length() == 0) {
            LOGGER.warning("Failed to enable blink from infocmp, using default");
            return "\u001B[0;5m";
        }
        else
            return blink;
    }

    public static String originalColors() {
        String reset = InfocmpHandler.getInstance().get("op");
        if(reset.length() == 0) {
            LOGGER.warning("Failed to reset from infocmp, using default");
            return "\u001B[0;0m";
        }
        else
            return reset;
    }



}

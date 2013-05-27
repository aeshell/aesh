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

    private static Logger logger = LoggerUtil.getLogger(InfocmpManager.class.getName());

    public static int[] getHome() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("khome");
        if(infocmpValue.length == 0) {
            logger.warning("Failed to get home from infocmp, using default");
            return new int[]{27,79,72}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getEnd() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("kend");
        if(infocmpValue.length == 0) {
            logger.warning("Failed to get end from infocmp, using default");
            return new int[]{27,79,70}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getPgUp() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("kpp");
        if(infocmpValue.length == 0) {
            logger.warning("Failed to get pgup from infocmp, using default");
            return new int[]{27,91,53,126}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getPgDown() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("knp");
        if(infocmpValue.length == 0) {
            logger.warning("Failed to get pgdown from infocmp, using default");
            return new int[]{27,91,54,126}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getLeft() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("kcub1");
        if(infocmpValue.length == 0) {
            logger.warning("Failed to get left from infocmp, using default");
            return new int[]{27,79,68}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getRight() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("kcuf1");
        if(infocmpValue.length == 0) {
            logger.warning("Failed to get right from infocmp, using default");
            return new int[]{27,79,68}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getUp() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("kcuu1");
        if(infocmpValue.length == 0) {
            logger.warning("Failed to get up from infocmp, using default");
            return new int[]{27,79,65}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getDown() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("kcud1");
        if(infocmpValue.length == 0) {
            logger.warning("Failed to get down from infocmp, using default");
            return new int[]{27,79,66}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getIns() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("kich1");
        if(infocmpValue.length == 0) {
            logger.warning("Failed to get insert from infocmp, using default");
            return new int[]{27,91,50,126}; //use default value
        }
        else
            return infocmpValue;

    }

    public static int[] getDelete() {
        int[] infocmpValue = InfocmpHandler.getInstance().getAsInts("kdch1");
        if(infocmpValue.length == 0) {
            logger.warning("Failed to get delete from infocmp, using default");
            return new int[]{27,91,51,126}; //use default value
        }
        else
            return infocmpValue;
    }

    public static String saveCursor() {
        String cursor = InfocmpHandler.getInstance().get("sc");
        if(cursor.length() == 0) {
            logger.warning("Failed to get save_cursor from infocmp, using default");
            return "\u001B7";
        }
        else
            return cursor;
    }

    public static String restoreCursor() {
        String cursor = InfocmpHandler.getInstance().get("rc");
        if(cursor.length() == 0) {
            logger.warning("Failed to get restore_cursor from infocmp, using default");
            return "\u001B8";
        }
        else
            return cursor;
    }

    public static String clearScreen() {
        String cursor = InfocmpHandler.getInstance().get("clear");
        if(cursor.length() == 0) {
            logger.warning("Failed to get clear from infocmp, using default");
            return "\u001B[2J";
        }
        else
            return cursor;
    }

}

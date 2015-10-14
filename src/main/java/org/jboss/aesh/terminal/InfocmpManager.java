/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.terminal;

import org.jboss.aesh.terminal.utils.Curses;
import org.jboss.aesh.terminal.utils.InfoCmp;
import org.jboss.aesh.terminal.utils.InfoCmp.Capability;
import org.jboss.aesh.util.LoggerUtil;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
        int[] home = getCurrentTranslatedCapabilityAsInts("khome");
        if(home.length == 0) {
            LOGGER.info("Failed to get key home from infocmp, using default");
            return new int[]{27,79,72}; //use default value
        }
        else
            return home;
    }

    public static int[] getHome() {
        int[] home = getCurrentTranslatedCapabilityAsInts("home");
        if(home.length == 0) {
            LOGGER.info("Failed to get cursor home from infocmp, using default");
            return new int[]{27,91,72}; //use default value
        }
        else
            return home;
    }

    public static int[] getEnd() {
        int[] infocmpValue = getCurrentTranslatedCapabilityAsInts("end");
        if(infocmpValue.length == 0) {
            LOGGER.info("Failed to get end from infocmp, using default");
            return new int[]{27,91,70}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getKeyEnd() {
        int[] infocmpValue = getCurrentTranslatedCapabilityAsInts("kend");
        if(infocmpValue.length == 0) {
            LOGGER.info("Failed to get end from infocmp, using default");
            return new int[]{27,79,70}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getPgUp() {
        int[] infocmpValue = getCurrentTranslatedCapabilityAsInts("kpp");
        if(infocmpValue.length == 0) {
            LOGGER.info("Failed to get pgup from infocmp, using default");
            return new int[]{27,91,53,126}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getPgDown() {
        int[] infocmpValue = getCurrentTranslatedCapabilityAsInts("knp");
        if(infocmpValue.length == 0) {
            LOGGER.info("Failed to get pgdown from infocmp, using default");
            return new int[]{27,91,54,126}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getLeft() {
        int[] infocmpValue = getCurrentTranslatedCapabilityAsInts("kcub1");
        if(infocmpValue.length == 0) {
            LOGGER.info("Failed to get left from infocmp, using default");
            return new int[]{27,79,68}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getRight() {
        int[] infocmpValue = getCurrentTranslatedCapabilityAsInts("cuf1");
        if(infocmpValue.length == 0) {
            LOGGER.info("Failed to get right from infocmp, using default");
            return new int[]{27,79,68}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getUp() {
        int[] infocmpValue = getCurrentTranslatedCapabilityAsInts("kcuu1");
        if(infocmpValue.length == 0) {
            LOGGER.info("Failed to get up from infocmp, using default");
            return new int[]{27,79,65}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getDown() {
        int[] infocmpValue = getCurrentTranslatedCapabilityAsInts("kcud1");
        if(infocmpValue.length == 0) {
            LOGGER.info("Failed to get down from infocmp, using default");
            return new int[]{27,79,66}; //use default value
        }
        else
            return infocmpValue;
    }

    public static int[] getIns() {
        int[] infocmpValue = getCurrentTranslatedCapabilityAsInts("kich1");
        if(infocmpValue.length == 0) {
            LOGGER.info("Failed to get insert from infocmp, using default");
            return new int[]{27,91,50,126}; //use default value
        }
        else
            return infocmpValue;

    }

    public static int[] getDelete() {
        int[] infocmpValue = getCurrentTranslatedCapabilityAsInts("kdch1");
        if(infocmpValue.length == 0) {
            LOGGER.info("Failed to get delete from infocmp, using default");
            return new int[]{27,91,51,126}; //use default value
        }
        else
            return infocmpValue;
    }

    public static String saveCursor() {
        String cursor = getCurrentTranslatedCapability("sc");
        if(cursor.length() == 0) {
            LOGGER.info("Failed to get save_cursor from infocmp, using default");
            return "\u001B[s";
        }
        else
            return cursor;
    }

    public static String restoreCursor() {
        String cursor = getCurrentTranslatedCapability("rc");
        if(cursor.length() == 0) {
            LOGGER.info("Failed to get restore_cursor from infocmp, using default");
            return "\u001B[u";
        }
        else
            return cursor;
    }

    public static String clearScreen() {
        String clear = getCurrentTranslatedCapability("clear");
        if(clear.length() == 0) {
            LOGGER.info("Failed to get clear from infocmp, using default");
            return "\u001B[2J";
        }
        else
            return clear;
    }

    public static String alternateBuffer() {
        String buffer = getCurrentTranslatedCapability("smcup");
        if(buffer.length() == 0) {
            LOGGER.info("Failed to get alternate buffer from infocmp, using default");
            return "\u001B[?1049h";
        }
        else
            return buffer;
    }

    public static String mainBuffer() {
        String buffer = getCurrentTranslatedCapability("rmcup");
        if(buffer.length() == 0) {
            LOGGER.info("Failed to get main buffer from infocmp, using default");
            return "\u001B[?1049l";
        }
        else
            return buffer;
    }

    public static String invertBackground() {
        String buffer = getCurrentTranslatedCapability("smso");
        if(buffer.length() == 0) {
            LOGGER.info("Failed to invert background from infocmp, using default");
            return "\u001B[7m";
        }
        else
            return buffer;
    }

    public static String normalBackground() {
        String buffer = getCurrentTranslatedCapability("rmso");
        if(buffer.length() == 0) {
            LOGGER.info("Failed to reset to normal background from infocmp, using default");
            return "\u001B[27m";
        }
        else
            return buffer;
    }

    public static String enableBold() {
        String bold = getCurrentTranslatedCapability("bold");
        if(bold.length() == 0) {
            LOGGER.info("Failed to get bold from infocmp, using default");
            return "\u001B[0;1m";
        }
        else
            return bold;
    }

    public static String enableUnderline() {
        String underline = getCurrentTranslatedCapability("smul");
        if(underline.length() == 0) {
            LOGGER.info("Failed to get underline from infocmp, using default");
            return "\u001B[0;4m";
        }
        else
            return underline;
    }

    public static String disableUnderline() {
        String underline = getCurrentTranslatedCapability("rmul");
        if(underline.length() == 0) {
            LOGGER.info("Failed to exit underline from infocmp, using default");
            return "\u001B[0;24m";
        }
        else
            return underline;
    }

    public static String enableBlink() {
        String blink = getCurrentTranslatedCapability("blink");
        if(blink.length() == 0) {
            LOGGER.info("Failed to enable blink from infocmp, using default");
            return "\u001B[0;5m";
        }
        else
            return blink;
    }

    public static String originalColors() {
        String reset = getCurrentTranslatedCapability("op");
        if(reset.length() == 0) {
            LOGGER.info("Failed to reset from infocmp, using default");
            return "\u001B[0;0m";
        }
        else
            return reset;
    }

    private static boolean initialized = false;
    private static Set<Capability> bools = new HashSet<>();
    private static Map<Capability, Integer> ints = new HashMap<>();
    private static Map<Capability, String> strings = new HashMap<>();

    private static int[] getCurrentTranslatedCapabilityAsInts(String cap) {
        String s = getCurrentTranslatedCapability(cap);
        return s.codePoints().toArray();
    }

    private static String getCurrentTranslatedCapability(String cap) {
        try {
            if (!initialized) {
                String term = System.getenv("TERM");
                if (term == null) {
                    term = "xterm-256color";
                }
                String infocmp = InfoCmp.getInfoCmp(term);
                InfoCmp.parseInfoCmp(infocmp, bools, ints, strings);
                initialized = true;
            }
            Capability capability = Capability.byName(cap);
            if (capability != null) {
                String capStr = strings.get(capability);
                if (capStr != null) {
                    StringWriter sw = new StringWriter();
                    Curses.tputs(sw, capStr);
                    return sw.toString();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "";
    }

}

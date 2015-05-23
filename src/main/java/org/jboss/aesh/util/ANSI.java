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
package org.jboss.aesh.util;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.terminal.InfocmpManager;

/**
 * Utility class to provide ANSI codes for different operations
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class ANSI {

    public static final String START = "\u001B[";
    public static final String BLACK_TEXT = "\u001B[0;30m";
    public static final String RED_TEXT = "\u001B[0;31m";
    public static final String GREEN_TEXT = "\u001B[0;32m";
    public static final String YELLOW_TEXT = "\u001B[0;33m";
    public static final String BLUE_TEXT = "\u001B[0;34m";
    public static final String MAGENTA_TEXT = "\u001B[0;35m";
    public static final String CYAN_TEXT = "\u001B[0;36m";
    public static final String WHITE_TEXT = "\u001B[0;37m";
    public static final String DEFAULT_TEXT = "\u001B[0;39m";

    public static final String BLACK_BG = "\u001B[0;40m";
    public static final String RED_BG = "\u001B[0;41m";
    public static final String GREEN_BG = "\u001B[0;42m";
    public static final String YELLOW_BG = "\u001B[0;43m";
    public static final String BLUE_BG = "\u001B[0;44m";
    public static final String MAGENTA_BG = "\u001B[0;45m";
    public static final String CYAN_BG = "\u001B[0;46m";
    public static final String WHITE_BG = "\u001B[0;47m";
    public static final String DEFAULT_BG = "\u001B[0;49m";
    public static final String ALTERNATE_BUFFER = Config.isOSPOSIXCompatible() ?
            InfocmpManager.alternateBuffer() : "\u001B[?1049h";
    public static final String MAIN_BUFFER = Config.isOSPOSIXCompatible() ?
            InfocmpManager.mainBuffer() : "\u001B[?1049l";
    public static final String INVERT_BACKGROUND = Config.isOSPOSIXCompatible() ?
            InfocmpManager.invertBackground() : "\u001B[7m";
    public static final String NORMAL_BACKGROUND = Config.isOSPOSIXCompatible() ?
            InfocmpManager.normalBackground() : "\u001B[27m";
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = Config.isOSPOSIXCompatible() ?
            InfocmpManager.enableBold() : "\u001B[0;1m";
    public static final String BOLD_OFF = "\u001B[0;22m";
    public static final String UNDERLINE = Config.isOSPOSIXCompatible() ?
            InfocmpManager.enableUnderline() : "\u001B[0;4m";
    public static final String UNDERLINE_OFF = Config.isOSPOSIXCompatible() ?
            InfocmpManager.disableUnderline() : "\u001B[0;24m";
    public static final String BLINK = Config.isOSPOSIXCompatible() ?
            InfocmpManager.enableBlink() : "\u001B[5m";
    public static final String BLINK_OFF = "\u001B[25m";
    public static final String CURSOR_START = "\u001B[1G";
    public static final String CURSOR_ROW = "\u001B[6n";
    public static final String CLEAR_SCREEN = Config.isOSPOSIXCompatible() ?
            InfocmpManager.clearScreen() : "\u001B[2J";
    public static final String CURSOR_SAVE = Config.isOSPOSIXCompatible() ?
            InfocmpManager.saveCursor() : "\u001B[s";
    public static final String CURSOR_RESTORE = Config.isOSPOSIXCompatible() ?
            InfocmpManager.restoreCursor() : "\u001B[u";
    public static final String CURSOR_HIDE = "\u001B[?25l";
    public static final String CURSOR_SHOW = "\u001B[?25h";

    private ANSI() {
    }
    
}

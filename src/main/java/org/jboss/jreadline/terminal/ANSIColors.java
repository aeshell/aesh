/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.jreadline.terminal;

import org.jboss.jreadline.console.Buffer;

/**
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class ANSIColors {

    private static char[] RESET = {'0','m'};

    private static char[] BLACK_TEXT = {'3','0','m'};
    private static char[] RED_TEXT = {'3','1','m'};
    private static char[] GREEN_TEXT = {'3','2','m'};
    private static char[] YELLOW_TEXT = {'3','3','m'};
    private static char[] BLUE_TEXT = {'3','4','m'};
    private static char[] MAGENTA_TEXT = {'3','5','m'};
    private static char[] CYAN_TEXT = {'3','6','m'};
    private static char[] WHITE_TEXT = {'3','7','m'};

    private static char[] BLACK_BG = {'4','0','m'};
    private static char[] RED_BG = {'4','1','m'};
    private static char[] GREEN_BG = {'4','2','m'};
    private static char[] YELLOW_BG = {'4','3','m'};
    private static char[] BLUE_BG = {'4','4','m'};
    private static char[] MAGENTA_BG = {'4','5','m'};
    private static char[] CYAN_BG = {'4','6','m'};
    private static char[] WHITE_BG = {'4','7','m'};

    public static final char[] BLACK_TEXT() {
        return Buffer.printAnsi(BLACK_TEXT);
    }

    public static final char[] RED_TEXT() {
        return Buffer.printAnsi(RED_TEXT);
    }

    public static final char[] GREEN_TEXT() {
        return Buffer.printAnsi(GREEN_TEXT);
    }

    public static final char[] YELLOW_TEXT() {
        return Buffer.printAnsi(YELLOW_TEXT);
    }

    public static final char[] BLUE_TEXT() {
        return Buffer.printAnsi(BLUE_TEXT);
    }

    public static final char[] MAGENTA_TEXT() {
        return Buffer.printAnsi(MAGENTA_TEXT);
    }

    public static final char[] CYAN_TEXT() {
        return Buffer.printAnsi(CYAN_TEXT);
    }

    public static final char[] WHITE_TEXT() {
        return Buffer.printAnsi(WHITE_TEXT);
    }

    public static final char[] BLACK_BG() {
        return Buffer.printAnsi(BLACK_BG);
    }

    public static final char[] RED_BG() {
        return Buffer.printAnsi(RED_BG);
    }

    public static final char[] GREEN_BG() {
        return Buffer.printAnsi(GREEN_BG);
    }

    public static final char[] YELLOW_BG() {
        return Buffer.printAnsi(YELLOW_BG);
    }

    public static final char[] BLUE_BG() {
        return Buffer.printAnsi(BLUE_BG);
    }

    public static final char[] MAGENTA_BG() {
        return Buffer.printAnsi(MAGENTA_BG);
    }

    public static final char[] CYAN_BG() {
        return Buffer.printAnsi(CYAN_BG);
    }

    public static final char[] WHITE_BG() {
        return Buffer.printAnsi(WHITE_BG);
    }

    public static final String RESET() {
        return new String(Buffer.printAnsi(RESET));
    }

}

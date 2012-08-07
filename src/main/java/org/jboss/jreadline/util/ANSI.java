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
package org.jboss.jreadline.util;

import org.jboss.jreadline.console.Buffer;

/**
 * Utility class to provide ANSI codes for different operations
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class ANSI {

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
    private static char[] ALTERNATE_BUFFER =  Buffer.printAnsi(new String("?1049h"));
    private static char[] MAIN_BUFFER =  Buffer.printAnsi(new String("?1049l"));
    private static char[] INVERT_BACKGROUND =  Buffer.printAnsi(new String("7m"));
    private static char[] RESET = Buffer.printAnsi(new char[]{'0','m'});

    public static char[] blackText() {
        return Buffer.printAnsi(BLACK_TEXT);
    }

    public static char[] redText() {
        return Buffer.printAnsi(RED_TEXT);
    }

    public static char[] greenText() {
        return Buffer.printAnsi(GREEN_TEXT);
    }

    public static char[] yellowText() {
        return Buffer.printAnsi(YELLOW_TEXT);
    }

    public static char[] blueText() {
        return Buffer.printAnsi(BLUE_TEXT);
    }

    public static char[] magentaText() {
        return Buffer.printAnsi(MAGENTA_TEXT);
    }

    public static char[] cyanText() {
        return Buffer.printAnsi(CYAN_TEXT);
    }

    public static char[] whiteText() {
        return Buffer.printAnsi(WHITE_TEXT);
    }

    public static char[] blackBackground() {
        return Buffer.printAnsi(BLACK_BG);
    }

    public static char[] redBackground() {
        return Buffer.printAnsi(RED_BG);
    }

    public static char[] greenBackground() {
        return Buffer.printAnsi(GREEN_BG);
    }

    public static char[] yellowBackground() {
        return Buffer.printAnsi(YELLOW_BG);
    }

    public static char[] blueBackground() {
        return Buffer.printAnsi(BLUE_BG);
    }

    public static char[] magentaBackground() {
        return Buffer.printAnsi(MAGENTA_BG);
    }

    public static char[] cyanBackground() {
        return Buffer.printAnsi(CYAN_BG);
    }

    public static char[] whiteBackground() {
        return Buffer.printAnsi(WHITE_BG);
    }

    public static char[] reset() {
        return RESET;
    }

    public static char[] getAlternateBufferScreen() {
       return ALTERNATE_BUFFER;
    }

    public static char[] getMainBufferScreen() {
        return MAIN_BUFFER;
    }

    public static char[] getInvertedBackground() {
        return INVERT_BACKGROUND;
    }

}

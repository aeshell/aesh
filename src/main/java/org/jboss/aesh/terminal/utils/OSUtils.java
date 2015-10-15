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
package org.jboss.aesh.terminal.utils;

import java.io.File;

public class OSUtils {

    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public static final boolean IS_CYGWIN = IS_WINDOWS
            && System.getenv("PWD") != null
            && System.getenv("PWD").startsWith("/");

    public static final boolean IS_OSX = System.getProperty("os.name").toLowerCase().contains("mac");

    public static String TTY_COMMAND;
    public static String STTY_COMMAND;
    public static String STTY_F_OPTION;
    public static String INFOCMP_COMMAND;

    static {
        String tty;
        String stty;
        String sttyfopt;
        String infocmp;
        if (OSUtils.IS_CYGWIN) {
            tty = "tty.exe";
            stty = "stty.exe";
            sttyfopt = null;
            infocmp = "infocmp.exe";
            String path = System.getenv("PATH");
            if (path != null) {
                String[] paths = path.split(";");
                for (String p : paths) {
                    if (new File(p, "tty.exe").exists()) {
                        tty = new File(p, "tty.exe").getAbsolutePath();
                    }
                    if (new File(p, "stty.exe").exists()) {
                        stty = new File(p, "stty.exe").getAbsolutePath();
                    }
                    if (new File(p, "infocmp.exe").exists()) {
                        infocmp = new File(p, "infocmp.exe").getAbsolutePath();
                    }
                }
            }
        } else {
            tty = "tty";
            stty = "stty";
            infocmp = "infocmp";
            if (IS_OSX) {
                sttyfopt = "-f";
            } else {
                sttyfopt = "-F";
            }
        }
        TTY_COMMAND = tty;
        STTY_COMMAND = stty;
        STTY_F_OPTION = sttyfopt;
        INFOCMP_COMMAND = infocmp;
    }

}

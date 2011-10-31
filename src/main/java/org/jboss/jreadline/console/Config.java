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
package org.jboss.jreadline.console;

/**
 * TODO: for posix systems, it should try to read .inputrc for edit mode
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class Config {

    private static String lineSeparator = null;
    private static String pathSeparator = null;
    private static boolean posixCompatible =
            !(System.getProperty("os.name").startsWith("Windows") ||
                    System.getProperty("os.name").startsWith("OS/2"));

    public static boolean isOSPOSIXCompatible() {
        return posixCompatible;
    }

    public static String getLineSeparator() {
        if(lineSeparator == null)
            lineSeparator = System.getProperty("line.separator");
        return lineSeparator;
    }

    public static String getPathSeparator() {
        if(pathSeparator == null)
            pathSeparator = System.getProperty("file.separator");
        return pathSeparator;
    }

    private void parseInputrc(String fileName) {

    }
}

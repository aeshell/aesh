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

import java.io.*;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO: for posix systems, it should try to read .inputrc for edit mode
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class Config {

    private static String lineSeparator = System.getProperty("line.separator");
    private static String pathSeparator = System.getProperty("file.separator");
    private static boolean posixCompatible =
            !(System.getProperty("os.name").startsWith("Windows") ||
                    System.getProperty("os.name").startsWith("OS/2"));

    public static boolean isOSPOSIXCompatible() {
        return posixCompatible;
    }

    public static String getLineSeparator() {
        return lineSeparator;
    }

    public static String getPathSeparator() {
        return pathSeparator;
    }

    /**
     * Must be able to parse:
     * set variablename value
     * keyname: function-name or macro
     * "keyseq": function-name or macro
     *
     * Lines starting with # are comments
     * Lines starting with $ are conditional init constructs
     *
     * @param fileName
     */
    protected void parseInputrc(String fileName, Settings settings) throws IOException {
        if(!new File(fileName).isFile()) {
            System.out.println("not a file");
            return;
        }

        Pattern variable = Pattern.compile("^set\\s+(\\S+)\\s+(\\S+)$");
        Pattern comment = Pattern.compile("^#");
        Pattern keyName = Pattern.compile("^\\b:\\s+\\b");
        Pattern keySeq = Pattern.compile("^\"keyseq:\\s+\\b");
        Pattern startConstructs = Pattern.compile("^\\$if");
        Pattern endConstructs = Pattern.compile("^\\$endif");


        BufferedReader reader =
                new BufferedReader( new FileReader(fileName));

        String line;
        boolean constructMode = false;
        while( (line = reader.readLine()) != null) {
            System.out.println("reading line:"+line);

            if(startConstructs.matcher(line).matches()) {
                constructMode = true;
                continue;
            }
            else if(endConstructs.matcher(line).matches()) {
                constructMode = false;
                continue;
            }

            if(constructMode) {

            }
            else {
                System.out.println("check if line matches variable");
                Matcher variableMatcher = variable.matcher(line);
                if(variableMatcher.matches()) {
                    System.out.println("found variable: "+variableMatcher.group(1));
                    System.out.println("value:"+variableMatcher.group(2));
                }

            }

        }

    }
}

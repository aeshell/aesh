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

import org.jboss.jreadline.console.Config;

import java.util.Arrays;
import java.util.List;

/**
 * String/Parser util methods
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class Parser {


    /**
     * Format completions so that they look similar to GNU Readline
     *
     * @param completions to format
     * @param termHeight max height
     * @param termWidth max width
     * @return formatted string to be outputted
     */
    public static String formatCompletions(String[] completions, int termHeight, int termWidth) {
        return formatCompletions(Arrays.asList(completions), termHeight, termWidth);
    }

    public static String formatCompletions(List<String> completions, int termHeight, int termWidth) {
        if(completions.size() < 1)
            return "";
        int maxLength = 0;
        for(String completion : completions)
            if(completion.length() > maxLength)
                maxLength = completion.length();

        maxLength = maxLength +2; //adding two spaces for better readability
        int numColumns = termWidth / maxLength;
        if(numColumns > completions.size()) // we dont need more columns than items
            numColumns = completions.size();
        int numRows = completions.size() / numColumns;

        // add a row if we cant display all the items
        if(numRows * numColumns < completions.size())
            numRows++;

        // build the completion listing
        StringBuilder completionOutput = new StringBuilder();
        for(int i=0; i < numRows; i++) {
            for(int c=0; c < numColumns; c++) {
                int fetch = i + (c * numRows);
                if(fetch < completions.size())
                    completionOutput.append(padRight(maxLength, completions.get(i + (c * numRows)))) ;
                else
                    break;
            }
            completionOutput.append(Config.getLineSeparator());
        }

        return completionOutput.toString();
    }


    private static String padRight(int n, String s) {
        return String.format("%1$-" + n + "s", s);
    }

    /**
     * Return the biggest common startsWith string
     *
     * @param completionList list to compare
     * @return biggest common startsWith string
     */
    public static String findStartsWith(List<String> completionList) {
        StringBuilder builder = new StringBuilder();
        for(String completion : completionList)
            while(builder.length() < completion.length() &&
                  startsWith(completion.substring(0, builder.length()+1), completionList))
                builder.append(completion.charAt(builder.length()));

        return builder.toString();
    }

    private static boolean startsWith(String criteria, List<String> completionList) {
        for(String completion : completionList)
            if(!completion.startsWith(criteria))
                return false;

        return true;
    }

    /**
     * Return the word "connected" to cursor
     * Note that cursor position starts at 0
     *
     * @param text to parse
     * @param cursor position
     * @return word connected to cursor
     */
    public static String findWordClosestToCursor(String text, int cursor) {
        if(text.length() <= cursor+1) {
            // return last word
            if(text.substring(0, cursor).contains(" ")) {
                if(text.lastIndexOf(" ") >= cursor) //cant use lastIndexOf
                    return text.substring(text.substring(0, cursor).lastIndexOf(" ")).trim();
                else
                    return text.trim().substring(text.lastIndexOf(" ")).trim();
            }
            else
                return text;
        }
        else {
            String rest = text.substring(0, cursor+1);
            if(cursor > 1 &&
                    text.charAt(cursor) == ' ' && text.charAt(cursor-1) == ' ')
                return "";
            //only if it contains a ' ' and its not at the end of the string
            if(rest.trim().contains(" "))
                return rest.substring(rest.trim().lastIndexOf(" ")).trim();
            else
                return rest.trim();
        }
    }
}

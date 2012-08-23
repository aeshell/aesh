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

import org.jboss.jreadline.complete.CompleteOperation;
import org.jboss.jreadline.console.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * String/Parser util methods
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class Parser {


    private static final String spaceEscapedMatcher = "\\ ";
    private static final String SPACE = " ";
    private static final char SLASH = '\\';
    private static final Pattern spaceEscapedPattern = Pattern.compile("\\\\ ");
    private static final Pattern spacePattern = Pattern.compile(" ");
    private static final Pattern onlyEscapedSpacePattern = Pattern.compile("[(\\\\ )&&[^(\\s)]]");

    /**
     * Format completions so that they look similar to GNU Readline
     *
     * @param displayList to format
     * @param termHeight max height
     * @param termWidth max width
     * @return formatted string to be outputted
     */
    public static String formatDisplayList(String[] displayList, int termHeight, int termWidth) {
        return formatDisplayList(Arrays.asList(displayList), termHeight, termWidth);
    }

    public static String formatDisplayList(List<String> displayList, int termHeight, int termWidth) {
        if(displayList == null || displayList.size() < 1)
            return "";
        int maxLength = 0;
        for(String completion : displayList)
            if(completion.length() > maxLength)
                maxLength = completion.length();

        maxLength = maxLength +2; //adding two spaces for better readability
        int numColumns = termWidth / maxLength;
        if(numColumns > displayList.size()) // we dont need more columns than items
            numColumns = displayList.size();
        int numRows = displayList.size() / numColumns;

        // add a row if we cant display all the items
        if(numRows * numColumns < displayList.size())
            numRows++;

        // build the completion listing
        StringBuilder completionOutput = new StringBuilder();
        for(int i=0; i < numRows; i++) {
            for(int c=0; c < numColumns; c++) {
                int fetch = i + (c * numRows);
                if(fetch < displayList.size())
                    completionOutput.append(padRight(maxLength, displayList.get(i + (c * numRows)))) ;
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
    
    public static String findStartsWithOperation(List<CompleteOperation> coList) {
        List<String> tmpList = new ArrayList<String>();
        for(CompleteOperation co : coList) {
            String s = findStartsWith(co.getFormattedCompletionCandidates());
            if(s.length() > 0)
                tmpList.add(s);
            else
                return "";
        }
        return findStartsWith(tmpList);
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
                if(doWordContainEscapedSpace(text)) {
                    if(doWordContainOnlyEscapedSpace(text))
                        return switchEscapedSpacesToSpacesInWord(text);
                    else {
                        return switchEscapedSpacesToSpacesInWord(findEscapedSpaceWordCloseToEnd(text));
                    }
                }
                else {
                    if(text.lastIndexOf(" ") >= cursor) //cant use lastIndexOf
                        return text.substring(text.substring(0, cursor).lastIndexOf(" ")).trim();
                    else
                        return text.trim().substring(text.lastIndexOf(" ")).trim();
                }
            }
            else
                return text.trim();
        }
        else {
            String rest = text.substring(0, cursor+1);
            if(doWordContainOnlyEscapedSpace(rest)) {
                if(cursor > 1 &&
                        text.charAt(cursor) == ' ' && text.charAt(cursor-1) == ' ')
                    return "";
                else
                    return switchEscapedSpacesToSpacesInWord(rest);
            }
            else {
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

    public static String findEscapedSpaceWordCloseToEnd(String text) {
        int index;
        String originalText = text;
        while((index = text.lastIndexOf(SPACE)) > -1) {
           if(index > 0 && text.charAt(index-1) == SLASH) {
               text = text.substring(0,index-1);
           }
            else
               return originalText.substring(index+1);
        }
        return originalText;
    }

    public static String findEscapedSpaceWordCloseToBeginning(String text) {
        int index;
        int totalIndex = 0;
        String originalText = text;
        while((index = text.indexOf(SPACE)) > -1) {
            if(index > 0 && text.charAt(index-1) == SLASH) {
                text = text.substring(index+1);
                totalIndex += index+1;
            }
            else
                return originalText.substring(0, totalIndex+index);
        }
        return originalText;
    }

    public static boolean doWordContainOnlyEscapedSpace(String word) {
        return (findAllOccurrences(word, spaceEscapedMatcher) == findAllOccurrences(word, SPACE));
    }

    public static boolean doWordContainEscapedSpace(String word) {
        return spaceEscapedPattern.matcher(word).find();
    }

    public static int findAllOccurrences(String word, String pattern) {
        int count = 0;
        while(word.contains(pattern)) {
            count++;
            word = word.substring(word.indexOf(pattern)+pattern.length());
        }
        return count;
    }

    public static List<String> switchEscapedSpacesToSpacesInList(List<String> list) {
        List<String> newList = new ArrayList<String>(list.size());
        for(String s : list)
            newList.add(switchEscapedSpacesToSpacesInWord(s));
        return newList;
    }

    public static String switchSpacesToEscapedSpacesInWord(String word) {
        return spacePattern.matcher(word).replaceAll("\\\\ ");
    }

    public static String switchEscapedSpacesToSpacesInWord(String word) {
        return spaceEscapedPattern.matcher(word).replaceAll(" ");
    }



    public static String findWordClosestToCursorDividedByRedirectOrPipe(String text, int cursor) {
        //1. find all occurrences of pipe/redirect.
        //2. find position thats closest to cursor.
        //3. return word closest to it
        String[] splitText = text.split(">|\\|");
        int length = 0;
        for(String s : splitText) {
            length += s.length()+1;
            if(cursor <= length) {
                return findWordClosestToCursor(s, cursor-(length-s.length()));
            }
        }
        return "";
    }
}

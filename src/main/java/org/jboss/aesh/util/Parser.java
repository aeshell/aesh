/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String/Parser util methods
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class Parser {

    private static final String spaceEscapedMatcher = "\\ ";
    private static final String SPACE = " ";
    private static final char SPACE_CHAR = ' ';
    private static final char SLASH = '\\';
    private static final Pattern spaceEscapedPattern = Pattern.compile("\\\\ ");
    private static final Pattern spacePattern = Pattern.compile(" ");

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

    /**
     * Format completions so that they look similar to GNU Readline
     *
     * @param displayList to format
     * @param termHeight max height
     * @param termWidth max width
     * @return formatted string to be outputted
     */
    public static String formatDisplayList(List<String> displayList, int termHeight, int termWidth) {
        if(displayList == null || displayList.size() < 1)
            return "";
        //make sure that termWidth is > 0
        if(termWidth < 1)
            termWidth = 80; //setting it to default

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


    /**
     * If there is any common start string in the completion list, return it
     *
     * @param coList completion list
     * @return common start string
     */
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
            if(text.contains(SPACE)) {
                if(doWordContainEscapedSpace(text)) {
                    if(doWordContainOnlyEscapedSpace(text))
                        return switchEscapedSpacesToSpacesInWord(text);
                    else {
                        return switchEscapedSpacesToSpacesInWord(findEscapedSpaceWordCloseToEnd(text));
                    }
                }
                else {
                    if(text.lastIndexOf(SPACE) >= cursor) //cant use lastIndexOf
                        return text.substring(text.substring(0, cursor).lastIndexOf(SPACE)).trim();
                    else
                        return text.substring(text.lastIndexOf(SPACE)).trim();
                }
            }
            else
                return text.trim();
        }
        else {
            String rest;
            if(text.length() > cursor+1)
                rest = text.substring(0, cursor+1);
            else
                rest = text;

            if(doWordContainOnlyEscapedSpace(rest)) {
                if(cursor > 1 &&
                        text.charAt(cursor) == SPACE_CHAR && text.charAt(cursor-1) == SPACE_CHAR)
                    return "";
                else
                    return switchEscapedSpacesToSpacesInWord(rest);
            }
            else {
                if(cursor > 1 &&
                        text.charAt(cursor) == SPACE_CHAR && text.charAt(cursor-1) == SPACE_CHAR)
                    return "";
                //only if it contains a ' ' and its not at the end of the string
                if(rest.trim().contains(SPACE))
                    return rest.substring(rest.trim().lastIndexOf(" ")).trim();
                else
                    return rest.trim();
            }
        }
    }

    /**
     * Search backwards for a non-escaped space and only return work containing non-escaped space
     *
     * @param text text
     * @return text with only non-escaped space
     */
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

    public static List<String> findAllWords(String text) {
        if(!doWordContainEscapedSpace(text)) {
            return Arrays.asList(text.trim().split(SPACE));
        }
        else {
            List<String> textList = new ArrayList<String>();
            Matcher matcher = spacePattern.matcher(text);
            while(matcher.find()) {
                if(matcher.start() > 0) {
                    //do we have an escaped space?
                    if(text.charAt(matcher.start()-1) == SLASH) {
                        //if word is \\  bla we remove the first
                        if(matcher.end()+1 < text.length() &&
                                text.charAt(matcher.end()) == SPACE_CHAR) {
                            text = text.substring(matcher.end()+1);
                            matcher = spacePattern.matcher(text);
                        }
                    }
                    //just a normal space
                    else {
                        textList.add(text.substring(0,matcher.start()));
                        text = text.substring(matcher.end());
                        matcher = spacePattern.matcher(text);
                    }
                }
                //word starts with a space
                else {
                    text = text.substring(1);
                    matcher = spacePattern.matcher(text);
                }
            }
            if(text.length() > 0)
                textList.add(text);
            return textList;
        }
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
        return spaceEscapedPattern.matcher(word).replaceAll(SPACE);
    }

    /**
     * Similar to String.trim(), but do not remove spaces that are escaped
     *
     * @param buffer input
     * @return trimmed buffer
     */
    public static String trim(String buffer) {
        //remove spaces in front
        int count = 0;
        for(int i=0; i < buffer.length(); i++) {
            if(buffer.charAt(i) == SPACE_CHAR)
                count++;
            else
                break;
        }
        if(count > 0)
            buffer = buffer.substring(count);

        //remove spaces in the end
        count = buffer.length();
        for(int i=buffer.length()-1; i > 0; i--) {
            if(buffer.charAt(i) == SPACE_CHAR && buffer.charAt(i-1) != SLASH)
                count--;
            else
                break;
        }
        if(count != buffer.length())
            buffer = buffer.substring(0, count);

        return buffer;
    }

    /**
     * If string contain space, return the text before the first space.
     * Spaces in the beginning and end is removed with Parser.trim(..)
     *
     * @param buffer input
     * @return first word
     */
    public static String findFirstWord(String buffer) {
        if(buffer.indexOf(SPACE_CHAR) < 0)
            return buffer;
        else {
            buffer = Parser.trim(buffer);
            int index = buffer.indexOf(SPACE_CHAR);
            if(index > 0)
                return buffer.substring(0, index);
            else
                return buffer;
        }
    }

}

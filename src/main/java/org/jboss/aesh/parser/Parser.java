/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.parser;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.terminal.TerminalString;

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
    public static final String SPACE = " ";
    public static final char SPACE_CHAR = ' ';
    public static final char BACK_SLASH = '\\';
    public static final char SINGLE_QUOTE = '\'';
    public static final char DOUBLE_QUOTE = '\"';
    public static final char DOLLAR = '$';
    private static final Pattern spaceEscapedPattern = Pattern.compile("\\\\ ");
    private static final Pattern spacePattern = Pattern.compile("(?<!\\\\)\\s");
    private static final Pattern ansiPattern = Pattern.compile("\\u001B\\[[\\?]?[0-9;]*[a-zA-Z]?");

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
        if(numColumns < 1)
            numColumns = 1;
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

    /**
     * Format completions so that they look similar to GNU Readline
     *
     * @param displayList to format
     * @param termHeight max height
     * @param termWidth max width
     * @return formatted string to be outputted
     */
    public static String formatDisplayListTerminalString(List<TerminalString> displayList, int termHeight, int termWidth) {
        if(displayList == null || displayList.size() < 1)
            return "";
        //make sure that termWidth is > 0
        if(termWidth < 1)
            termWidth = 80; //setting it to default

        int maxLength = 0;
        for(TerminalString completion : displayList)
            if(completion.getCharacters().length() > maxLength)
                maxLength = completion.getCharacters().length();

        maxLength = maxLength +2; //adding two spaces for better readability
        int numColumns = termWidth / maxLength;
        if(numColumns > displayList.size()) // we dont need more columns than items
            numColumns = displayList.size();
        if(numColumns < 1)
            numColumns = 1;
        int numRows = displayList.size() / numColumns;

        // add a row if we cant display all the items
        if(numRows * numColumns < displayList.size())
            numRows++;

        StringBuilder completionOutput = new StringBuilder();
        if(numRows > 1) {
            // build the completion listing
            for(int i=0; i < numRows; i++) {
                for(int c=0; c < numColumns; c++) {
                    int fetch = i + (c * numRows);
                    if(fetch < displayList.size())
                        completionOutput.append(padRight(maxLength+displayList.get(i + (c * numRows)).getANSILength(),
                                displayList.get(i + (c * numRows)).toString())) ;
                    else
                        break;
                }
                completionOutput.append(Config.getLineSeparator());
            }
        }
        else {
            for(TerminalString ts : displayList) {
                completionOutput.append(ts.toString()).append("  ");
            }
            completionOutput.append(Config.getLineSeparator());
        }

        return completionOutput.toString();
    }

    /**
     * Format output to columns with flexible sizes and no redundant space between them
     *
     * @param displayList to format
     * @param termWidth   max width
     * @return formatted string to be outputted
     */
    public static String formatDisplayCompactListTerminalString(List<TerminalString> displayList, int termWidth) {
        if (displayList == null || displayList.size() < 1)
            return "";
        //make sure that termWidth is > 0
        if (termWidth < 1)
            termWidth = 80; //setting it to default

        int numRows = 1;

        // increase numRows and check in loop if it's possible to format strings to columns
        while (!canDisplayColumns(displayList, numRows, termWidth) && numRows < displayList.size()) {
            numRows++;
        }

        int numColumns = displayList.size() / numRows;
        if (displayList.size() % numRows > 0) {
            numColumns++;
        }

        int[] columnsSizes = calculateColumnSizes(displayList, numColumns, numRows, termWidth);

        StringBuilder stringOutput = new StringBuilder();

        for (int i = 0; i < numRows; i++) {
            for (int c = 0; c < numColumns; c++) {
                int fetch = i + (c * numRows);
                int nextFetch = i + ((c + 1) * numRows);

                if (fetch < displayList.size()) {
                    // don't need to format last column of row = nextFetch doesn't exit
                    if (nextFetch < displayList.size()) {
                        stringOutput.append(padRight(columnsSizes[c] + displayList.get(i + (c * numRows)).getANSILength(),
                            displayList.get(i + (c * numRows)).toString()));
                    } else {
                        stringOutput.append(displayList.get(i + (c * numRows)).toString());
                    }
                } else {
                    break;
                }
            }
            stringOutput.append(Config.getLineSeparator());
        }

        return stringOutput.toString();
    }

    /**
     * Decides if it's possible to format provided Strings into calculated number of columns while the output
     * will not exceed terminal width
     *
     * @param displayList
     * @param numRows
     * @param terminalWidth
     * @return true if it's possible to format strings to columns and false otherwise.
     */
    private static boolean canDisplayColumns(List<TerminalString> displayList, int numRows, int terminalWidth) {

        int numColumns = displayList.size() / numRows;
        if (displayList.size() % numRows > 0) {
            numColumns++;
        }

        int[] columnSizes = calculateColumnSizes(displayList, numColumns, numRows, terminalWidth);

        int totalSize = 0;
        for (int columnSize : columnSizes) {
            totalSize += columnSize;
        }
        if (totalSize > terminalWidth) {
            return false;
        }

        return true;
    }

    private static int[] calculateColumnSizes(List<TerminalString> displayList, int numColumns, int numRows, int termWidth) {
        int[] columnSizes = new int[numColumns];

        for (int i = 0; i < displayList.size(); i++) {
            int columnIndex = (i / numRows) % numColumns;
            int stringSize = displayList.get(i).getCharacters().length() + 2;

            if (columnSizes[columnIndex] < stringSize && stringSize <= termWidth) {
                columnSizes[columnIndex] = stringSize;
            }
        }

        return columnSizes;
    }

    public static String padRight(int n, String s) {
        return String.format("%1$-" + n + "s", s);
    }

    public static String padLeft(int n, String s) {
        return String.format("%1$" + n + "s", s);
    }

    public static List<String> splitBySizeKeepWords(String words, int size) {
        List<String> out = new ArrayList<>();
        if(words.length() <= size) {
            out.add(words);
            return out;
        }
        else {
            while(words.length() > size) {
               int i = words.lastIndexOf(' ', size);
                if(i > 0) {
                    out.add(words.substring(0,i));
                    words = words.substring(i+1);
                }
                else {
                    out.add(words);
                    break;
                }
            }
            if(words.length() > 0)
                out.add(words);
            return out;
        }
    }

    /**
     * remove leading dashes from word
     */
    public static String trimOptionName(String word) {
        if(word.startsWith("--"))
            return word.substring(2);
        else if(word.startsWith("-"))
            return word.substring(1);
        else
            return word;
    }

    public static boolean findIfWordEndWithSpace(String word) {
       return !word.isEmpty() && word.endsWith(" ") && !word.endsWith("\\ ");
    }

    /**
     * If there is any common start string in the completion list, return it
     *
     * @param coList completion list
     * @return common start string
     */
    public static String findStartsWithOperation(List<CompleteOperation> coList) {
        List<String> tmpList = new ArrayList<>();
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
     * Return the biggest common startsWith string
     *
     * @param completionList list to compare
     * @return biggest common startsWith string
     */
    public static String findStartsWithTerminalString(List<TerminalString> completionList) {
        StringBuilder builder = new StringBuilder();
        for(TerminalString completion : completionList)
            while(builder.length() < completion.getCharacters().length() &&
                  startsWithTerminalString(completion.getCharacters().substring(0, builder.length()+1), completionList))
                builder.append(completion.getCharacters().charAt(builder.length()));

        return builder.toString();
    }

    private static boolean startsWithTerminalString(String criteria, List<TerminalString> completionList) {
        for(TerminalString completion : completionList)
            if(!completion.getCharacters().startsWith(criteria))
                return false;

        return true;
    }



    public static String findWordClosestToCursor(String text, int cursor) {
        boolean startOutsideText = false;
        if(cursor >= text.length()) {
            cursor = text.length()-1;
            startOutsideText = true;
        }
        if(cursor < 0 || text.trim().length() == 0)
            return "";

        boolean foundBackslash = false;

        if(text.contains(SPACE)) {
            int start, end;
            if(text.charAt(cursor) == SPACE_CHAR) {
                if(startOutsideText)
                    return "";
                if(cursor > 0) {
                    if(text.charAt(cursor-1) == SPACE_CHAR)
                        return "";
                    else
                        cursor--;
                }
            }

            boolean space = false;
            for(start = cursor; start > 0; start--) {
                if(space) {
                   if(text.charAt(start) == BACK_SLASH) {
                       space = false;
                       foundBackslash = true;
                   }
                    else {
                       start+=2;
                       break;
                   }
                }
                if(Character.isSpaceChar(text.charAt(start)))
                    space = true;
            }

            boolean back = false;
            for(end = cursor; end < text.length(); end++) {
                if(text.charAt(end) == BACK_SLASH) {
                    back = true;
                    foundBackslash = true;
                }
                else if(back) {
                    if(Character.isSpaceChar(text.charAt(end))) {
                        back = false;
                    }
                }
                else if(Character.isSpaceChar(text.charAt(end))) {
                    break;
                }
            }
            if(foundBackslash)
                return switchEscapedSpacesToSpacesInWord(text.substring(start,end));
            else
                return text.substring(start,end);
        }
        else {
            return text.trim();
        }
    }

    /**
     * Return the word "connected" to cursor, the word ends at cursor position.
     * Note that cursor position starts at 0
     *
     * @param text to parse
     * @param cursor position
     * @return word connected to cursor
     */
    public static String findCurrentWordFromCursor(String text, int cursor) {
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
           if(index > 0 && text.charAt(index-1) == BACK_SLASH) {
               text = text.substring(0,index-1);
           }
            else
               return originalText.substring(index+1);
        }
        return originalText;
    }

    /**
     * Check if a string contain open quotes. Escaped quotes does not count.
     *
     * @param text text
     * @return true if it contains open quotes, else false
     */
    public static boolean doesStringContainOpenQuote(String text) {
        boolean doubleQuote = false;
        boolean singleQuote = false;
        for(int i=0; i < text.length(); i++) {
            if(text.charAt(i) == SINGLE_QUOTE) {
                if(!doubleQuote &&
                        (i == 0 || (i > 0 && !(text.charAt(i-1) == BACK_SLASH))))
                    singleQuote = !singleQuote;
            }
            else if(text.charAt(i) == DOUBLE_QUOTE) {
                if(!singleQuote &&
                        (i == 0 || (i > 0 && !(text.charAt(i-1) == BACK_SLASH))))
                    doubleQuote = !doubleQuote;
            }
        }
        return doubleQuote || singleQuote;
    }

    /**
     * Split up the text into words, escaped spaces and quotes are handled
     *
     * @param text test
     * @return aeshline with all the words
     */
    public static AeshLine findAllWords(String text) {
        List<String> textList = new ArrayList<>();
        boolean haveEscape = false;
        boolean haveSingleQuote = false;
        boolean haveDoubleQuote = false;
        StringBuilder builder = new StringBuilder();

        for(char c : text.toCharArray()) {
            if(c == SPACE_CHAR) {
                if(haveEscape) {
                    builder.append(c);
                    haveEscape = false;
                }
                else if(haveSingleQuote || haveDoubleQuote) {
                    builder.append(c);
                }
                else if(builder.length() > 0) {
                    textList.add(builder.toString());
                    builder = new StringBuilder();
                }
            }
            else if(c == BACK_SLASH) {
                if(haveEscape) {
                    builder.append(c);
                    haveEscape = false;
                }
                else
                    haveEscape = true;
            }
            else if(c == SINGLE_QUOTE) {
                if(haveEscape) {
                    builder.append(c);
                    haveEscape = false;
                }
                else if(haveSingleQuote) {
                    if(builder.length() > 0) {
                        textList.add(builder.toString());
                        builder = new StringBuilder();
                    }
                    haveSingleQuote = false;
                }
                else
                    haveSingleQuote = true;
            }
            else if(c == DOUBLE_QUOTE) {
                if(haveEscape) {
                    builder.append(c);
                    haveEscape = false;
                }
                else if(haveDoubleQuote) {
                    if(builder.length() > 0) {
                        textList.add(builder.toString());
                        builder = new StringBuilder();
                    }
                    haveDoubleQuote = false;
                }
                else
                    haveDoubleQuote = true;
            }
            else if(haveEscape) {
                builder.append(BACK_SLASH);
                builder.append(c);
                haveEscape = false;
            }
            else
                builder.append(c);
        }
        //if the escape was the last char, add it to the builder
        if(haveEscape)
            builder.append(BACK_SLASH);

        if(builder.length() > 0)
            textList.add(builder.toString());

        ParserStatus status = ParserStatus.OK;
        if(haveSingleQuote && haveDoubleQuote)
            status = ParserStatus.DOUBLE_UNCLOSED_QUOTE;
        else  if(haveSingleQuote || haveDoubleQuote)
            status = ParserStatus.UNCLOSED_QUOTE;

        return new AeshLine(textList, status, "");
    }

    public static boolean doWordContainOnlyEscapedSpace(String word) {
        return (findAllOccurrences(word, spaceEscapedMatcher) == findAllOccurrences(word, SPACE));
    }

    public static boolean doWordContainEscapedSpace(String word) {
        return spaceEscapedPattern.matcher(word).find();
    }

    /**
     * find number of spaces in the given word.
     * escaped spaces are not counted
     * @param word to check
     * @return number of spaces
     */
    public static int findNumberOfSpacesInWord(String word) {
        int count = 0;
        for(int i=0; i < word.length();i++)
            if(word.charAt(i) == SPACE_CHAR && (i == 0 || word.charAt(i-1) != BACK_SLASH))
                count++;

        return count;
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

    public static void switchEscapedSpacesToSpacesInTerminalStringList(List<TerminalString> list) {
        for(TerminalString ts : list)
            ts.setCharacters(switchEscapedSpacesToSpacesInWord(ts.getCharacters()));
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
            if(buffer.charAt(i) == SPACE_CHAR && buffer.charAt(i-1) != BACK_SLASH)
                count--;
            else
                break;
        }
        if(count != buffer.length())
            buffer = buffer.substring(0, count);

        return buffer;
    }

    /**
     * Only trim space in front of the word
     *
     * @param buffer input
     * @return trimmed buffer
     */
    public static String trimInFront(String buffer) {
        //remove spaces in front
        int count = 0;
        for(int i=0; i < buffer.length(); i++) {
            if(buffer.charAt(i) == SPACE_CHAR)
                count++;
            else
                break;
        }
        if(count > 0)
            return buffer.substring(count);
        else
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

    public static boolean containsNonEscapedDollar(String buffer) {
        int startIndex = 0;
        while((startIndex = buffer.indexOf(DOLLAR, startIndex)) > -1) {
            if(startIndex == 0)
                return true;
            else if(buffer.charAt(startIndex-1) != BACK_SLASH)
                return true;
            else
                startIndex++;
        }
        return false;
    }

    public static String stripAwayAnsiCodes(String text) {
        return ansiPattern.matcher(text).replaceAll("");
    }
}

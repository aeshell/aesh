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

package org.aesh.parser;

import org.aesh.command.operator.OperatorType;

import java.util.ArrayList;
import java.util.List;

/**
 * A input line that is parsed into chunks of words.
 *
 * The input are split into words usually based on whitespaces.
 * The exceptions are made for escaped whitespaces and whitespaces that are
 * enclosed within quotes.
 * Single and double quotes is accepted, but only as pairs.
 *
 * If the cursor position is given, ParsedLine will be able to return the word
 * "connected" to the cursor.
 * If no cursor position is given the cursor value is -1 and cursorWord is an empty string.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParsedLine {

    private final String originalInput;
    private final String errorMessage;
    private final List<ParsedWord> words;
    private final ParserStatus status;
    private final int cursor;
    private final int cursorWord;
    private final int wordCursor;
    private final OperatorType operator;


    public ParsedLine(String originalInput, List<ParsedWord> words,
                      int cursor, int cursorWord, int wordCursor,
                      ParserStatus status, String errorMessage, OperatorType operator) {
        this.originalInput = originalInput;
        this.cursor = cursor;
        this.cursorWord = cursorWord;
        this.wordCursor = wordCursor;
        this.status = status;
        this.errorMessage = errorMessage;
        this.operator = operator;

        if (words == null) {
            this.words = new ArrayList<>(0);
            return;
        }

        this.words = words;
    }

    /**
     * @return cursor
     */
    public int cursor() {
        return cursor;
    }

    /**
     * @return the word index connected to the cursor
     */
    public int selectedIndex() {
        return cursorWord;
    }

    /**
     * @return the word connected to the cursor.
     * If not cursor was given it will return an empty ParsedWord object.
     */
    public ParsedWord selectedWord() {
        if(cursorWord > -1 && cursorWord < words.size())
            return words.get(cursorWord);
        else
            return new ParsedWord("", 0);
    }

    /**
     * @return the word connected to the cursor.
     * If the cursor is not at the end of the word,
     * it will only return part of the word up to the cursor position.
     */
    public ParsedWord selectedWordToCursor() {
        if(cursorWord > -1 && cursorWord < words.size())
            return new ParsedWord(
                    words.get(cursorWord).word().substring(0, wordCursor),
                    words.get(cursorWord).lineIndex());
        else
            return new ParsedWord("", 0);
    }

    /**
     * @return index inside the word where the cursor is positioned.
     */
    public int wordCursor() {
        return wordCursor;
    }

    /**
     * @return original input
     */
    public String line() {
        return originalInput;
    }

    /**
     * @return any errors that was found during parsing
     */
    public String errorMessage() {
        return errorMessage;
    }

    /**
     * @return the list of words
     */
    public List<ParsedWord> words() {
        return words;
    }

    /**
     * @return status of the parser. Useful if there have been any errors.
     */
    public ParserStatus status() {
        return status;
    }

    public ParsedWord lastWord() {
        return words().get(words.size()-1);
    }

    public ParsedWord firstWord() {
        if(words.size() > 0 )
            return words.get(0);
        else
            return new ParsedWord("", 0);
    }

    public int size() {
        return words().size();
    }

    public boolean hasWords() {
        return words().size() > 0;
    }

    /**
     * @return a highly specialized iterator to make it easier to parse the input
     */
    public ParsedLineIterator iterator() {
        return new ParsedLineIterator(this);
    }

    public OperatorType operator() {
        return operator;
    }

    public boolean cursorAtEnd() {
        return cursor == originalInput.length();
    }

    public boolean spaceAtEnd() {
        if(originalInput.length() > 1) {
            return originalInput.charAt(originalInput.length()-1) == ' ' &&
                   originalInput.charAt(originalInput.length()-2) != '\\' ;
        }
        else
            return (originalInput.length() > 0 &&
                    originalInput.charAt(originalInput.length()-1) == ' ');
    }

    public boolean isCursorAtEndOfSelectedWord() {
        return cursor() == (selectedWord().lineIndex()+selectedWord().word().length());
    }

    @Override
    public String toString() {
        return "ParsedLine{" +
                "originalInput='" + originalInput + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", words=" + words +
                ", status=" + status +
                ", cursor=" + cursor +
                ", cursorWord=" + cursorWord +
                ", wordCursor=" + wordCursor +
                ", operator=" + operator +
                '}';
    }
}

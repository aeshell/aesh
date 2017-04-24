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

/**
 * A specialized iterator that makes it easier to parse the input.
 *
 * Polling either words or chars from the stack will update the respective
 * other values correctly as well.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParsedLineIterator {

    private final ParsedLine parsedLine;
    private int word = 0;
    private int character = 0;

    public ParsedLineIterator(ParsedLine parsedLine) {
        this.parsedLine = parsedLine;
    }

    /**
     * @return true if there is a next word
     */
    public boolean hasNextWord() {
        return parsedLine.words().size() > word;
    }

    /**
     * @return true if there is a next char
     */
    public boolean hasNextChar() {
        return parsedLine.line().length() > character;
    }

    /**
     * Polls the next ParsedWord from the stack.
     *
     * @return next ParsedWord
     */
    public ParsedWord pollParsedWord() {
        if(hasNextWord()) {
            //set correct next char
            if(parsedLine.words().size() > (word+1))
                character = parsedLine.words().get(word+1).lineIndex();
            else
                character = -1;
            return parsedLine.words().get(word++);
        }
        else
            return new ParsedWord(null, -1);
    }

    /**
     * Peeks at the next ParsedWord from the stack
     *
     * @return next ParsedWord
     */
    public ParsedWord peekParsedWord() {
        if(hasNextWord())
            return parsedLine.words().get(word);
        else
            return new ParsedWord(null, -1);
    }

    /**
     * Polls the next word as String from the stack.
     *
     * @return next word
     */
    public String pollWord() {
        return pollParsedWord().word();
    }

    /**
     * Peeks the next word as String from the stack.
     *
     * @return next word
     */
    public String peekWord() {
        return peekParsedWord().word();
    }

    /**
     * Polls the next char from the stack
     *
     * @return next char
     */
    public char pollChar() {
        if(hasNextChar()) {
            if(hasNextWord() &&
                    character+1 >= parsedLine.words().get(word).lineIndex()+
                            parsedLine.words().get(word).word().length())
                word++;
            return parsedLine.line().charAt(character++);
        }
        return '\u0000';
    }

    /**
     * Peeks at the next char from the stack
     *
     * @return next char
     */
    public char peekChar() {
        if(hasNextChar())
            return parsedLine.line().charAt(character);
        return '\u0000';
    }

    /**
     * @return true if there are no more words/chars on the stack
     */
    public boolean finished() {
        return parsedLine.words().size() == word || parsedLine.line().length() == character;
    }

    /**
     * @return any parsing errors made when creating the ParsedLine
     */
    public String parserError() {
        return parsedLine.errorMessage();
    }

    /**
     * Return a substring of the base input from where the current position is.
     *
     * @return substring from current position till the end.
     */
    public String stringFromCurrentPosition() {
        return parsedLine.line().substring(character);
    }

    /**
     * Update the current position with specified length.
     * The input will append to the current position of the iterator.
     *
     * @param length update length
     */
    public void updateIteratorPosition(int length) {
        if(length > 0) {
            //make sure we dont go OB
            if((length + character) > parsedLine.line().length())
                length = parsedLine.line().length() - character;

            //move word counter to the correct word
            while(hasNextWord() &&
                    (length + character) >= parsedLine.words().get(word).lineIndex() +
                            parsedLine.words().get(word).word().length())
                word++;

            character = length + character;
        }
        else
            throw new IllegalArgumentException("The length given must be > 0 and not exceed the boundary of the line (including the current position)");
    }

    /**
     * @return true if next word is connected to the cursor
     */
    public boolean isNextWordCursorWord() {
        return word == parsedLine.selectedIndex();
    }

    public ParsedLine baseLine() {
        return parsedLine;
    }
}

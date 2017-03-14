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

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class LineParser {

    private static final char NULL_CHAR = '\u0000';
    private static final char SPACE_CHAR = ' ';
    private static final char BACK_SLASH = '\\';
    private static final char SINGLE_QUOTE = '\'';
    private static final char DOUBLE_QUOTE = '\"';

    /**
     * Split up the text into words, escaped spaces and quotes are handled
     *
     * @param text test
     * @return aeshline with all the words
     */
    public static ParsedLine parseLine(String text) {
        return parseLine(text, -1);
    }

    public static ParsedLine parseLine(String text, int cursor) {
        List<ParsedWord> textList = new ArrayList<>();
        boolean haveEscape = false;
        boolean haveSingleQuote = false;
        boolean haveDoubleQuote = false;
        boolean ternaryQuote = false;
        StringBuilder builder = new StringBuilder();
        char prev = NULL_CHAR;
        int index = 0;
        int cursorWord = -1;
        int wordCursor = -1;

        for (char c : text.toCharArray()) {
            //if the previous char was a space, there is no word "connected" to cursor
            if(cursor == index && (prev != SPACE_CHAR || haveEscape)) {
                cursorWord = textList.size();
                wordCursor = builder.length();
            }
            if (c == SPACE_CHAR) {
                if (haveEscape) {
                    builder.append(c);
                    haveEscape = false;
                }
                else if (haveSingleQuote || haveDoubleQuote) {
                    builder.append(c);
                }
                else if (builder.length() > 0) {
                    textList.add(new ParsedWord(builder.toString(), index-builder.length()));
                    builder = new StringBuilder();
                }
            }
            else if (c == BACK_SLASH) {
                if (haveEscape || ternaryQuote) {
                    builder.append(c);
                    haveEscape = false;
                }
                else
                    haveEscape = true;
            }
            else if (c == SINGLE_QUOTE) {
                if (haveEscape || ternaryQuote) {
                    builder.append(c);
                    haveEscape = false;
                }
                else if (haveSingleQuote) {
                    if (builder.length() > 0) {
                        textList.add(new ParsedWord(builder.toString(), index-builder.length()));
                        builder = new StringBuilder();
                    }
                    haveSingleQuote = false;
                }
                else if(haveDoubleQuote) {
                    builder.append(c);
                }
                else
                    haveSingleQuote = true;
            }
            else if (c == DOUBLE_QUOTE) {
                if (haveEscape || (ternaryQuote && prev != DOUBLE_QUOTE)) {
                    builder.append(c);
                    haveEscape = false;
                }
                else if (haveDoubleQuote) {
                    if (!ternaryQuote && prev == DOUBLE_QUOTE)
                        ternaryQuote = true;
                    else if (ternaryQuote && prev == DOUBLE_QUOTE) {
                        if (builder.length() > 0) {
                            builder.deleteCharAt(builder.length() - 1);
                            textList.add(new ParsedWord(builder.toString(), index-builder.length()));
                            builder = new StringBuilder();
                        }
                        haveDoubleQuote = false;
                        ternaryQuote = false;
                    }
                    else {
                        if (builder.length() > 0) {
                            textList.add(new ParsedWord(builder.toString(), index-builder.length()));
                            builder = new StringBuilder();
                        }
                        haveDoubleQuote = false;
                    }
                }
                else if(haveSingleQuote)
                    builder.append(c);
                else
                    haveDoubleQuote = true;
            }
            else if (haveEscape) {
                builder.append(BACK_SLASH);
                builder.append(c);
                haveEscape = false;
            }
            else
                builder.append(c);
            prev = c;
            index++;
        }
        // if the escape was the last char, add it to the builder
        if (haveEscape)
            builder.append(BACK_SLASH);

        if (builder.length() > 0)
            textList.add(new ParsedWord(builder.toString(), index-builder.length()));

        if (cursor == text.length()) {
            cursorWord = textList.size() - 1;
            if(textList.size() > 0)
                wordCursor = textList.get(textList.size() - 1).word().length();
        }

        ParserStatus status = ParserStatus.OK;
        if (haveSingleQuote && haveDoubleQuote)
            status = ParserStatus.DOUBLE_UNCLOSED_QUOTE;
        else if (haveSingleQuote || haveDoubleQuote)
            status = ParserStatus.UNCLOSED_QUOTE;

        return new ParsedLine(text, textList, cursor, cursorWord, wordCursor, status, "");
    }
}

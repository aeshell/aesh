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
import java.util.Set;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class LineParser {

    private static final char NULL_CHAR = '\u0000';
    private static final char SPACE_CHAR = ' ';
    private static final char BACK_SLASH = '\\';
    private static final char SINGLE_QUOTE = '\'';
    private static final char DOUBLE_QUOTE = '\"';
    private static final char CURLY_START = '{';
    private static final char CURLY_END = '}';

    private List<ParsedWord> textList = new ArrayList<>();
    private boolean haveEscape = false;
    private boolean haveSingleQuote = false;
    private boolean haveDoubleQuote = false;
    private boolean ternaryQuote = false;
    private boolean haveCurlyBracket = false;
    private boolean haveSquareBracket = false;
    private StringBuilder builder = new StringBuilder();
    private char prev = NULL_CHAR;
    private int index = 0;
    private int cursorWord = -1;
    private int wordCursor = -1;

    /**
     * Split up the text into words, escaped spaces and quotes are handled
     *
     * @param text test
     * @return aeshline with all the words
     */
    public ParsedLine parseLine(String text) {
        return parseLine(text, -1);
    }

    public ParsedLine parseLine(String text, int cursor) {
        return parseLine(text, cursor, false);
    }

    public ParsedLine parseLine(String text, int cursor, boolean parseCurlyAndSquareBrackets) {
        //first reset all values
        reset();
        if(cursor > text.length())
            cursor = text.length();
        for (char c : text.toCharArray()) {
            //if the previous char was a space, there is no word "connected" to cursor
            if(cursor == index && (prev != SPACE_CHAR || haveEscape)) {
                cursorWord = textList.size();
                wordCursor = builder.length();
            }
            if (c == SPACE_CHAR) {
                handleSpace(c);
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
                handleSingleQuote(c);
            }
            else if (c == DOUBLE_QUOTE) {
                handleDoubleQuote(c);
            }
            else if(parseCurlyAndSquareBrackets &&  c == CURLY_START) {
                handleCurlyStart(c);
            }
            else if(parseCurlyAndSquareBrackets &&  c == CURLY_END && haveCurlyBracket) {
                handleCurlyEnd(c);
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
        return endOfLineProcessing(text, cursor);
   }

   public List<ParsedLine> parseLine(String text, int cursor, boolean parseCurlyAndSquareBrackets, Set<OperatorType> operators) {
        List<ParsedLine> lines = new ArrayList<>();
        if(operators == null || operators.size() == 0) {
            lines.add(parseLine(text, cursor, parseCurlyAndSquareBrackets));
        }
        else {
            //first reset all values
            reset();
            OperatorType currentOperator = null;
            int startIndex = 0;
            char c;
            //for (char c : text.toCharArray()) {
            for(index=0; index < text.length();) {
                c = text.charAt(index);
                //if the previous char was a space, there is no word "connected" to cursor
                if(cursor == index && (prev != SPACE_CHAR || haveEscape)) {
                    cursorWord = textList.size();
                    wordCursor = builder.length();
                }
                if (c == SPACE_CHAR) {
                    handleSpace(c);
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
                    handleSingleQuote(c);
                }
                else if (c == DOUBLE_QUOTE) {
                    handleDoubleQuote(c);
                }
                else if(parseCurlyAndSquareBrackets &&  c == CURLY_START) {
                    handleCurlyStart(c);
                }
                else if(parseCurlyAndSquareBrackets &&  c == CURLY_END && haveCurlyBracket) {
                    handleCurlyEnd(c);
                }
                else if (haveEscape) {
                    builder.append(BACK_SLASH);
                    builder.append(c);
                    haveEscape = false;
                }
                else if(!haveEscape && !isQuoted() &&
                        (currentOperator = matchesOperators(operators, text, index)) != OperatorType.NONE) {
                    if (builder.length() > 0) {
                        textList.add(new ParsedWord(builder.toString(), index-builder.length()));
                        builder = new StringBuilder();
                    }
                    if (cursor == text.length()) {
                        cursorWord = textList.size() - 1;
                        if(textList.size() > 0)
                            wordCursor = textList.get(textList.size() - 1).word().length();
                    }

                    lines.add(
                            new ParsedLine(text.substring(startIndex, index), textList, cursor, cursorWord, wordCursor, ParserStatus.OK, "", currentOperator));

                    cursorWord = -1;
                    wordCursor = -1;
                    startIndex = index + currentOperator.value().length();
                    textList = new ArrayList<>();
                }
                else
                    builder.append(c);

                //if current operator is set, we need to handle index/prev specially
                if(currentOperator != OperatorType.NONE) {
                   index = index + currentOperator.value().length();
                   prev = text.charAt(index-1);
                   //we need to reset operator
                   currentOperator = OperatorType.NONE;
                }
                else {
                    prev = c;
                    index++;
                }
            }

            if(builder.length() > 0 || !textList.isEmpty())
                lines.add(endOfLineProcessing(text.substring(startIndex, index), cursor));
        }

        return lines;
    }

    private char nextChar(String text, int index) {
        if(text.length() > index+1)
            return text.charAt(index+1);
        else
            return '\u0000';
    }

    private boolean isQuoted() {
        return (haveDoubleQuote || haveSingleQuote || haveCurlyBracket || haveSquareBracket);
    }

    private OperatorType matchesOperators(Set<OperatorType> operators, String text, int index) {
        return OperatorType.matches(operators, text, index);
    }

    private ParsedLine endOfLineProcessing(String text, int cursor) {
        // if the escape was the last char, add it to the builder
        if (haveEscape)
            builder.append(BACK_SLASH);

        if (builder.length() > 0)
            textList.add(new ParsedWord(builder.toString(), index-builder.length()));

        if (cursor == text.length() && (prev != SPACE_CHAR || haveEscape)) {
            cursorWord = textList.size() - 1;
            if (textList.size() > 0)
                wordCursor = textList.get(textList.size() - 1).word().length();
        }

        ParserStatus status = ParserStatus.OK;
        if (haveSingleQuote && haveDoubleQuote)
            status = ParserStatus.DOUBLE_UNCLOSED_QUOTE;
        else if (haveSingleQuote || haveDoubleQuote || haveCurlyBracket)
            status = ParserStatus.UNCLOSED_QUOTE;

        return new ParsedLine(text, textList, cursor, cursorWord, wordCursor, status, "", OperatorType.NONE);
    }

    private void handleCurlyEnd(char c) {
        if(haveEscape)
            haveEscape = false;
        else {
            haveCurlyBracket = false;
        }
        builder.append(c);
    }

    private void handleCurlyStart(char c) {
        if(haveEscape) {
            haveEscape = false;
        }
        else if(!haveSingleQuote && !haveDoubleQuote){
            haveCurlyBracket = true;
        }
        builder.append(c);
    }

    private void handleDoubleQuote(char c) {
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

    private void handleSingleQuote(char c) {
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

    private void handleSpace(char c) {
        if (haveEscape) {
            builder.append(c);
            haveEscape = false;
        }
        else if (haveSingleQuote || haveDoubleQuote || haveCurlyBracket) {
            builder.append(c);
        }
        else if (builder.length() > 0) {
            textList.add(new ParsedWord(builder.toString(), index-builder.length()));
            builder = new StringBuilder();
        }
    }

    private void reset() {
        textList = new ArrayList<>();
        haveEscape = false;
        haveSingleQuote = false;
        haveDoubleQuote = false;
        ternaryQuote = false;
        haveCurlyBracket = false;
        haveSquareBracket = false;
        builder = new StringBuilder();
        prev = NULL_CHAR;
        index = 0;
        cursorWord = -1;
        wordCursor = -1;
    }
}

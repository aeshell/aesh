/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.aesh.command.operator.OperatorType;

/**
 * @author Aesh team
 */
public class LineParser {

    private static final char NULL_CHAR = '\u0000';
    private static final char SPACE_CHAR = ' ';
    private static final char BACK_SLASH = '\\';
    private static final char SINGLE_QUOTE = '\'';
    private static final char DOUBLE_QUOTE = '\"';
    private static final char CURLY_START = '{';
    private static final char CURLY_END = '}';
    private static final char PARENTHESIS_START = '(';
    private static final char PARENTHESIS_END = ')';

    private List<ParsedWord> textList = new ArrayList<>();
    private boolean haveEscape = false;
    private boolean haveSingleQuote = false;
    private boolean haveDoubleQuote = false;
    private boolean ternaryQuote = false;
    private boolean haveCurlyBracket = false;
    private boolean haveSquareBracket = false;
    private StringBuilder builder = new StringBuilder();
    /**
     * A quote pair just closed over an empty builder (outside the ternary
     * path). The token exists despite having no text, so emission points
     * must not consult {@code builder.length()} alone (#651).
     */
    private boolean quotedEmpty = false;
    /**
     * Line index captured when quotes close over content, carried to the
     * later flush. Closing quotes advance the index without growing the
     * builder, so a later flush would otherwise misplace the word; the
     * carried value reproduces the historical close-time position. Unset
     * (-1) when no quotes closed since the token began.
     */
    private int pendingIndex = -1;
    private char prev = NULL_CHAR;
    private int index = 0;
    private int cursorWord = -1;
    private int wordCursor = -1;

    private String text;
    private int cursor = -1;
    private boolean parseBrackets;
    private EnumSet<OperatorType> operators;
    private OperatorType currentOperator;
    private int startIndex;

    public LineParser input(String text) {
        this.text = text;
        return this;
    }

    public LineParser cursor(int cursor) {
        this.cursor = cursor;
        return this;
    }

    public LineParser parseBrackets(boolean doParse) {
        this.parseBrackets = doParse;
        return this;
    }

    public LineParser operators(EnumSet<OperatorType> operators) {
        this.operators = operators;
        return this;
    }

    public ParsedLine parse() {
        if (text != null)
            return parseLine(text, cursor, parseBrackets);
        else
            return null;
    }

    public List<ParsedLine> parseWithOperators() {
        if (text != null && operators != null)
            return parseLine(text, cursor, parseBrackets, operators);
        else
            return null;
    }

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
        if (cursor > text.length())
            cursor = text.length();
        return doParseLine(text, cursor, parseCurlyAndSquareBrackets);
    }

    private ParsedLine doParseLine(String text, int cursor, boolean parseCurlyAndSquareBrackets) {
        char c;
        for (index = 0; index < text.length();) {
            c = text.charAt(index);
            //if the previous char was a space, there is no word "connected" to cursor
            if (cursor == index && (prev != SPACE_CHAR || haveEscape)) {
                // A token completed by a closing quote is flushed only by
                // the following space/end/operator; if the cursor sits on
                // that separator it is already past the token (#651).
                int completed = (pendingIndex >= 0 && c == SPACE_CHAR) ? 1 : 0;
                cursorWord = textList.size() + completed;
                if (haveEscape) //if we have escape the builder is shorter than cursor
                    wordCursor = builder.length() + 1;
                else if (completed == 1)
                    wordCursor = 0;
                else
                    wordCursor = builder.length();
            }
            if (c == SPACE_CHAR) {
                c = handleSpace(c);
            } else if (c == BACK_SLASH) {
                if (haveEscape || ternaryQuote || haveDoubleQuote || haveSingleQuote) {
                    builder.append(c);
                    haveEscape = false;
                } else
                    haveEscape = true;
            } else if (c == SINGLE_QUOTE) {
                handleSingleQuote(c);
            } else if (c == DOUBLE_QUOTE) {
                handleDoubleQuote(c);
            } else if (parseCurlyAndSquareBrackets && (c == CURLY_START || c == PARENTHESIS_START)) {
                handleCurlyStart(c);
            } else if (parseCurlyAndSquareBrackets && (c == CURLY_END || c == PARENTHESIS_END) && haveCurlyBracket) {
                handleCurlyEnd(c);
            } else if (haveEscape) {
                handleEscape(c);
            } else
                builder.append(c);
            prev = c;
            index++;
        }
        return endOfLineProcessing(text, cursor, 0, text.length());
    }

    public List<ParsedLine> parseLine(String text, int cursor, boolean parseCurlyAndSquareBrackets,
            Set<OperatorType> operators) {
        if (operators == null || operators.size() == 0) {
            List<ParsedLine> lines = new ArrayList<>();
            lines.add(parseLine(text, cursor, parseCurlyAndSquareBrackets));
            return lines;
        } else {
            //first reset all values
            reset();
            currentOperator = null;
            startIndex = 0;
            return doParseLine(text, cursor, parseCurlyAndSquareBrackets, operators);
        }
    }

    private List<ParsedLine> doParseLine(String text, int cursor, boolean parseCurlyAndSquareBrackets,
            Set<OperatorType> operators) {
        List<ParsedLine> lines = new ArrayList<>();
        char c;
        for (index = 0; index < text.length();) {
            c = text.charAt(index);
            //if the previous char was a space, there is no word "connected" to cursor
            if (cursor == index && (prev != SPACE_CHAR || haveEscape)) {
                // As above: a quote-completed token pending flush counts as
                // complete when the cursor sits on the separator (#651).
                int completed = (pendingIndex >= 0 && c == SPACE_CHAR) ? 1 : 0;
                cursorWord = textList.size() + completed;
                wordCursor = completed == 1 ? 0 : builder.length();
            }
            if (c == SPACE_CHAR) {
                c = handleSpace(c);
            } else if (c == BACK_SLASH) {
                if (haveEscape || ternaryQuote || haveDoubleQuote || haveSingleQuote) {
                    builder.append(c);
                    haveEscape = false;
                } else
                    haveEscape = true;
            } else if (c == SINGLE_QUOTE) {
                handleSingleQuote(c);
            } else if (c == DOUBLE_QUOTE) {
                handleDoubleQuote(c);
            } else if (parseCurlyAndSquareBrackets && (c == CURLY_START || c == PARENTHESIS_START)) {
                handleCurlyStart(c);
            } else if (parseCurlyAndSquareBrackets && (c == CURLY_END || c == PARENTHESIS_END) && haveCurlyBracket) {
                handleCurlyEnd(c);
            } else if (haveEscape) {
                //Escaping an operator?
                if (!isQuoted()
                        && (currentOperator = matchesOperators(operators, text, index)) != OperatorType.NONE) {
                    // Do not add the \ that was a way to escape an operator.
                } else {
                    builder.append(BACK_SLASH);
                }
                builder.append(c);
                haveEscape = false;
            } else if (!haveEscape && !isQuoted() &&
                    (currentOperator = matchesOperators(operators, text, index)) != OperatorType.NONE) {
                handleFoundOperator(lines, text, cursor);

                //if we end on an operator and cursor == text.length, add another empty line
                if (index + currentOperator.value().length() == text.length() && cursor == text.length()) {
                    textList.add(new ParsedWord("", index));
                    lines.add(new ParsedLine(text, textList, 0,
                            0, 0, ParserStatus.OK, "", OperatorType.NONE));

                    //we know we're at the end so we can return
                    return lines;
                }
            } else
                builder.append(c);

            //if current operator is set, we need to handle index/prev specially
            if (currentOperator != null && currentOperator != OperatorType.NONE) {
                index = index + currentOperator.value().length();
                prev = text.charAt(index - 1);
                //we need to reset operator
                currentOperator = OperatorType.NONE;
            } else {
                prev = c;
                index++;
            }
        }

        if (builder.length() > 0 || !textList.isEmpty() || startIndex < index)
            lines.add(endOfLineProcessing(text.substring(startIndex, index), cursor, startIndex, text.length()));

        return lines;
    }

    private char nextChar(String text, int index) {
        if (text.length() > index + 1)
            return text.charAt(index + 1);
        else
            return '\u0000';
    }

    private boolean isQuoted() {
        return (haveDoubleQuote || haveSingleQuote || haveCurlyBracket || haveSquareBracket);
    }

    private OperatorType matchesOperators(Set<OperatorType> operators, String text, int index) {
        return OperatorType.matches(operators, text, index);
    }

    private ParsedLine endOfLineProcessing(String text, int cursor,
            int startIndex, int totalTextLength) {
        // if the escape was the last char, add it to the builder
        if (haveEscape)
            builder.append(BACK_SLASH);

        if (builder.length() > 0) {
            int lineIndex = pendingIndex >= 0 ? pendingIndex : index - builder.length();
            if (haveDoubleQuote || haveSingleQuote)
                textList.add(new ParsedWord(builder.toString(), lineIndex, ParsedWord.Status.OPEN_QUOTE));
            else if (haveSquareBracket || haveCurlyBracket)
                textList.add(new ParsedWord(builder.toString(), lineIndex, ParsedWord.Status.OPEN_BRACKET));
            else
                textList.add(new ParsedWord(builder.toString(), lineIndex));
        } else if (quotedEmpty) {
            // A quote pair closed over an empty builder: the token exists
            // despite having no text (e.g. probe '' keeps its argument).
            textList.add(new ParsedWord("", index));
        }
        quotedEmpty = false;
        pendingIndex = -1;

        if (cursor == totalTextLength &&
                (prev != SPACE_CHAR || (haveEscape || isQuoted()))) {
            cursorWord = textList.size() - 1;
            if (textList.size() > 0)
                wordCursor = textList.get(textList.size() - 1).word().length();
        }

        ParserStatus status = ParserStatus.OK;
        if (haveSingleQuote && haveDoubleQuote)
            status = ParserStatus.DOUBLE_UNCLOSED_QUOTE;
        else if (haveSingleQuote || haveDoubleQuote || haveCurlyBracket)
            status = ParserStatus.UNCLOSED_QUOTE;

        return new ParsedLine(text, textList,
                startIndex <= cursor && cursor <= index ? cursor - startIndex : -1,
                cursorWord, wordCursor, status, "", OperatorType.NONE);
    }

    private void handleCurlyEnd(char c) {
        if (haveEscape)
            haveEscape = false;
        else {
            haveCurlyBracket = false;
        }
        builder.append(c);
    }

    private void handleCurlyStart(char c) {
        if (haveEscape) {
            haveEscape = false;
        } else if (!haveSingleQuote && !haveDoubleQuote) {
            haveCurlyBracket = true;
        }
        builder.append(c);
    }

    private void handleDoubleQuote(char c) {
        //already quoted and prev is escape, just add the quote
        if (ternaryQuote || haveDoubleQuote || haveSingleQuote) {
            if (prev == BACK_SLASH) {
                builder.append(c);
                return;
            }
        }

        if (haveEscape || (ternaryQuote && prev != DOUBLE_QUOTE)) {
            builder.append(c);
            haveEscape = false;
        } else if (haveDoubleQuote) {
            handleHaveDoubleQuote();
        } else if (haveSingleQuote || haveCurlyBracket)
            builder.append(c);
        else
            haveDoubleQuote = true;
    }

    /**
     * Emits the accumulated token, using the close-time index carried past
     * any closing quotes when present.
     */
    private void emitToken() {
        int lineIndex = pendingIndex >= 0 ? pendingIndex : index - builder.length();
        textList.add(new ParsedWord(builder.toString(), lineIndex));
        builder = new StringBuilder();
        quotedEmpty = false;
        pendingIndex = -1;
    }

    private void handleHaveDoubleQuote() {
        if (!ternaryQuote && prev == DOUBLE_QUOTE) {
            if (builder.length() > 0) {
                // "" attached to existing content (e.g., --option=""):
                // closing the quotes must not split the word, the content
                // is emitted by the space/end/operator handling, positioned
                // as if emitted here (#651).
                pendingIndex = index - builder.length();
                haveDoubleQuote = false;
            } else {
                ternaryQuote = true;
            }
        } else if (ternaryQuote && prev == DOUBLE_QUOTE) {
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
                textList.add(new ParsedWord(builder.toString(), index - builder.length()));
                builder = new StringBuilder();
            }
            quotedEmpty = false;
            pendingIndex = -1;
            haveDoubleQuote = false;
            ternaryQuote = false;
        } else {
            // Closing quotes do not imply a word boundary: ab"cd"ef is one
            // word. An empty close still denotes a token (see quotedEmpty).
            if (builder.length() == 0)
                quotedEmpty = true;
            else
                pendingIndex = index - builder.length();
            haveDoubleQuote = false;
        }
    }

    private void handleSingleQuote(char c) {
        //already quoted and prev is escape, just add the quote
        if (ternaryQuote || haveDoubleQuote || haveSingleQuote) {
            if (prev == BACK_SLASH) {
                builder.append(c);
                return;
            }
        }
        if (haveEscape || ternaryQuote) {
            builder.append(c);
            haveEscape = false;
        } else if (haveSingleQuote) {
            // Closing quotes do not imply a word boundary; an empty close
            // still denotes a token (see quotedEmpty).
            if (builder.length() == 0)
                quotedEmpty = true;
            else
                pendingIndex = index - builder.length();
            haveSingleQuote = false;
        } else if (haveDoubleQuote) {
            builder.append(c);
        } else if (haveCurlyBracket)
            builder.append(c);
        else
            haveSingleQuote = true;
    }

    private char handleSpace(char c) {
        if (haveEscape) {
            builder.append(c);
            haveEscape = false;
            //since we escape it, we need to set it to a different value other than space
            c = NULL_CHAR;
        } else if (haveSingleQuote || haveDoubleQuote || haveCurlyBracket) {
            builder.append(c);
        } else if (builder.length() > 0 || quotedEmpty) {
            emitToken();
        }

        return c;
    }

    private void handleFoundOperator(List<ParsedLine> lines, String text, int cursor) {
        ParserStatus parserStatus = ParserStatus.OK;
        String errorMessage = "";
        if (builder.length() > 0 || quotedEmpty) {
            emitToken();
        }
        //if textList.size == 0, we have an empty line before the operator
        else if (textList.size() == 0) {
            if (!currentOperator.equals(OperatorType.NONE))
                parserStatus = ParserStatus.EMPTY_BEFORE_OPERATOR;
            errorMessage = "aesh: syntax error near unexpected token \'" + currentOperator.value() + '\'';
        }
        //we know we have an operator so we need to subtract one char
        if (cursor == text.length() - 1) {
            cursorWord = textList.size() - 1;
            if (textList.size() > 0)
                wordCursor = textList.get(textList.size() - 1).word().length();
        }

        lines.add(
                new ParsedLine(text.substring(startIndex, index), textList,
                        startIndex <= cursor && cursor <= index ? cursor - startIndex : -1,
                        cursorWord, wordCursor, parserStatus, errorMessage, currentOperator));

        cursorWord = -1;
        wordCursor = -1;
        startIndex = index + currentOperator.value().length();
        textList = new ArrayList<>();
    }

    private void handleEscape(char c) {
        builder.append(BACK_SLASH);
        builder.append(c);
        haveEscape = false;
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
        quotedEmpty = false;
        pendingIndex = -1;
        prev = NULL_CHAR;
        index = 0;
        cursorWord = -1;
        wordCursor = -1;
    }
}

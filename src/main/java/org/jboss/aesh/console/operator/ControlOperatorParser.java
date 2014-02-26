/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.operator;

import org.jboss.aesh.console.ConsoleOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser class for everything that contain operator and pipelines
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ControlOperatorParser {

    private static final Pattern controlOperatorPattern = Pattern.compile("(2>&1)|(2>>)|(2>)|(>>)|(>)|(<)|(\\|&)|(\\|\\|)|(\\|)|(;)|(&&)|(&)");
    private static final Pattern redirectionNoPipelinePattern = Pattern.compile("(2>&1)|(2>>)|(2>)|(>>)|(>)|(<)");
    private static final Pattern pipelineAndEndPattern = Pattern.compile("(\\|&)|(\\|)|(;)");
    private static final char ESCAPE = '\\';
    private static final char EQUALS = '=';

    public static boolean doStringContainRedirectionNoPipeline(String buffer) {
        return redirectionNoPipelinePattern.matcher(buffer).find();
    }

    /**
     * Used when parsing a complete
     *
     * @param buffer text
     * @return true if it contains pipeline
     */
    public static boolean doStringContainPipelineOrEnd(String buffer) {
        return pipelineAndEndPattern.matcher(buffer).find();
    }

    public static int getPositionOfFirstRedirection(String buffer) {
        Matcher matcher = redirectionNoPipelinePattern.matcher(buffer);
        if(matcher.find())
            return matcher.end();
        else
            return 0;
    }

    /**
     * Used when finding the correct word to base complete on
     *
     * @param buffer text
     * @param cursor position
     * @return last pipeline pos before cursor
     */
    public static int findLastPipelineAndEndPositionBeforeCursor(String buffer, int cursor) {
        return findLastRedirectionOrPipelinePositionBeforeCursor(pipelineAndEndPattern, buffer, cursor);
    }

    /**
     * Used when finding the correct word to base operator complete on
     *
     * @param buffer text
     * @param cursor position
     * @return last operator pos before cursor
     */
    public static int findLastRedirectionPositionBeforeCursor(String buffer, int cursor) {
        return findLastRedirectionOrPipelinePositionBeforeCursor(redirectionNoPipelinePattern, buffer, cursor);
    }

    private static int findLastRedirectionOrPipelinePositionBeforeCursor(Pattern pattern, String buffer, int cursor) {
        Matcher matcher = pattern.matcher(buffer);
        if(cursor > buffer.length())
            cursor = buffer.length();
        int end = 0;
        while(matcher.find()) {
            if(matcher.start() > cursor)
                return end;
            else
                end = matcher.end();
        }
        return end;
    }

    /**
     * Parse buffer and find all RedirectionOperations
     *
     * @param buffer text
     * @return all RedirectionOperations
     */
    public static List<ConsoleOperation> findAllControlOperators(String buffer) {
        Matcher matcher = controlOperatorPattern.matcher(buffer);
        List<ConsoleOperation> reOpList = new ArrayList<>();

        while(matcher.find()) {
            if(matcher.group(1) != null) {
                reOpList.add( new ConsoleOperation(ControlOperator.OVERWRITE_OUT_AND_ERR,
                        buffer.substring(0, matcher.start(1))));
                buffer = buffer.substring(matcher.end(1));
                matcher = controlOperatorPattern.matcher(buffer);
            }
            else if(matcher.group(2) != null) {
                reOpList.add( new ConsoleOperation(ControlOperator.APPEND_ERR,
                        buffer.substring(0, matcher.start(2))));
                buffer = buffer.substring(matcher.end(2));
                matcher = controlOperatorPattern.matcher(buffer);
            }
            else if(matcher.group(3) != null) {
                reOpList.add( new ConsoleOperation(ControlOperator.OVERWRITE_ERR,
                        buffer.substring(0, matcher.start(3))));
                buffer = buffer.substring(matcher.end(3));
                matcher = controlOperatorPattern.matcher(buffer);
            }
            else if(matcher.group(4) != null) {
                reOpList.add( new ConsoleOperation(ControlOperator.APPEND_OUT,
                        buffer.substring(0, matcher.start(4))));
                buffer = buffer.substring(matcher.end(4));
                matcher = controlOperatorPattern.matcher(buffer);
            }
            else if(matcher.group(5) != null) {
                if(matcher.start(5) > 0 &&
                        buffer.charAt(matcher.start(5)-1) != ESCAPE &&
                        buffer.charAt(matcher.start(5)-1) != EQUALS &&
                        ((matcher.start(5)+1 < buffer.length() &&
                                buffer.charAt(matcher.start(5)+1) != EQUALS)
                                || matcher.start(5)+1 == buffer.length())) {
                    reOpList.add( new ConsoleOperation(ControlOperator.OVERWRITE_OUT,
                            buffer.substring(0, matcher.start(5))));
                    buffer = buffer.substring(matcher.end(5));
                    matcher = controlOperatorPattern.matcher(buffer);
                }
            }
            else if(matcher.group(6) != null) {
                if(matcher.start(6) > 0 &&
                        buffer.charAt(matcher.start(6)-1) != ESCAPE &&
                        buffer.charAt(matcher.start(6)-1) != EQUALS &&
                        (( matcher.start(6)+1 < buffer.length() &&
                                buffer.charAt(matcher.start(6)+1) != EQUALS) ||
                                matcher.start(6)+1 == buffer.length())) {

                    reOpList.add( new ConsoleOperation(ControlOperator.OVERWRITE_IN,
                            buffer.substring(0, matcher.start(6))));
                    buffer = buffer.substring(matcher.end(6));
                    matcher = controlOperatorPattern.matcher(buffer);
                }
            }
            else if(matcher.group(7) != null) {
                reOpList.add( new ConsoleOperation(ControlOperator.PIPE_OUT_AND_ERR,
                        buffer.substring(0, matcher.start(7))));
                buffer = buffer.substring(matcher.end(7));
                matcher = controlOperatorPattern.matcher(buffer);
            }
            else if(matcher.group(8) != null) {
                reOpList.add( new ConsoleOperation(ControlOperator.OR,
                        buffer.substring(0, matcher.start(8))));
                buffer = buffer.substring(matcher.end(8));
                matcher = controlOperatorPattern.matcher(buffer);
            }
            else if(matcher.group(9) != null) {
                if(matcher.start(9) > 0 &&
                        buffer.charAt(matcher.start(9)-1) != ESCAPE) {
                    reOpList.add( new ConsoleOperation(ControlOperator.PIPE,
                            buffer.substring(0, matcher.start(9))));
                    buffer = buffer.substring(matcher.end(9));
                    matcher = controlOperatorPattern.matcher(buffer);
                }
            }
            else if(matcher.group(10) != null) {
                reOpList.add( new ConsoleOperation(ControlOperator.END,
                        buffer.substring(0, matcher.start(10))));
                buffer = buffer.substring(matcher.end(10));
                matcher = controlOperatorPattern.matcher(buffer);
            }
            else if(matcher.group(11) != null) {
                reOpList.add( new ConsoleOperation(ControlOperator.AND,
                        buffer.substring(0, matcher.start(11))));
                buffer = buffer.substring(matcher.end(11));
                matcher = controlOperatorPattern.matcher(buffer);
            }
            else if(matcher.group(12) != null) {
                if(matcher.start(12) > 0 &&
                        buffer.charAt(matcher.start(12)-1) != ESCAPE) {
                    reOpList.add( new ConsoleOperation(ControlOperator.AMP,
                            buffer.substring(0, matcher.start(12))));
                    buffer = buffer.substring(matcher.end(12));
                    matcher = controlOperatorPattern.matcher(buffer);
                }
            }
        }
        if(reOpList.size() == 0)
            reOpList.add(new ConsoleOperation( ControlOperator.NONE, buffer));
        if(buffer.trim().length() > 0)
            reOpList.add(new ConsoleOperation(ControlOperator.NONE, buffer));

        return reOpList;
    }
}

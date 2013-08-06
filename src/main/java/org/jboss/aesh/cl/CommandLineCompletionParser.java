/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.OptionInt;
import org.jboss.aesh.cl.internal.ParameterInt;
import org.jboss.aesh.util.Parser;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineCompletionParser {

    private CommandLineParser parser;

    public CommandLineCompletionParser(CommandLineParser parser) {
        this.parser = parser;
    }


    /**
     * 1. find the last "word"
     *   if it starts with '-', we need to check if its a value or name
     * @param line buffer
     * @return ParsedCompleteObject
     */
    public ParsedCompleteObject findCompleteObject(String line) throws CommandLineParserException {

        //first we check if it could be a param
        if(Parser.findIfWordEndWithSpace(line)) {
            //check if we try to complete just after the command name
            ParameterInt param = parser.getParameter();
            if(line.trim().equals(param.getName()))
                return new ParsedCompleteObject(false, null, 0);

            //else we try to complete an option,an option value or arguments
            String lastWord = Parser.findEscapedSpaceWordCloseToEnd(line.trim());
            if(lastWord.startsWith("-")) {
                while(lastWord.startsWith("-"))
                    lastWord = lastWord.substring(1);
                if(lastWord.length() == 0)
                    return new ParsedCompleteObject(false, null, 0);
                else if(parser.getParameter().findOption(lastWord) != null ||
                        parser.getParameter().findLongOption(lastWord) != null)
                    return findCompleteObjectValue(line, true);
                else
                    return new ParsedCompleteObject(false, null, 0);
            }
            else
                return new ParsedCompleteObject(true);
        }
        else
            return optionFinder(line);
    }

    private ParsedCompleteObject optionFinder(String line) throws CommandLineParserException {
        String lastWord = Parser.findEscapedSpaceWordCloseToEnd(line);
        //last word might be an option
        if(lastWord.startsWith("-") ) {
            String secLastWord =
                    Parser.findEscapedSpaceWordCloseToEnd(
                            line.substring(0,line.length()-lastWord.length()));
            //second to last word also start with -
            if(secLastWord.startsWith("-")) {
                //do this for now
                return findCompleteObjectValue(line, false);
            }
            //the last word is an option (most likely)
            else {
                if(lastWord.equals("-")) {
                    return new ParsedCompleteObject(true, "", 1);
                }
                else if(lastWord.equals("--")) {
                    return new ParsedCompleteObject(true, "", 2);
                }
                else {
                    return new ParsedCompleteObject(true,
                            Parser.trimOptionName(lastWord), lastWord.length());
                }
            }
        }
        else
            return findCompleteObjectValue(line, false);
    }

    /**
     * Only called when we know that the last word is an option value
     * If endsWithSpace is true we set the value to an empty string to indicate a value
     */
    private ParsedCompleteObject findCompleteObjectValue(String line, boolean endsWithSpace) throws CommandLineParserException {
        CommandLine cl = parser.parse(line, true);
        //the last word is an argument
        if(cl.getArgument() != null && !cl.getArgument().getValues().isEmpty()) {
            return new ParsedCompleteObject("",
                    cl.getArgument().getValues().get(cl.getArgument().getValues().size()-1),
                    cl.getArgument().getType(), false);
        }
        //get the last option
        else {
            OptionInt po = cl.getOptions().get(cl.getOptions().size()-1);
            return new ParsedCompleteObject( po.getName().isEmpty() ? po.getShortName() : po.getName(),
                    endsWithSpace ? "" : po.getValue(), po.getType(), true);
        }
    }

}

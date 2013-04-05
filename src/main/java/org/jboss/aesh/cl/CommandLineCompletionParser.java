/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

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
    public ParsedCompleteObject findCompleteObject(String line) {

        //first we check if it could be a param
        if(Parser.findIfWordEndWithSpace(line)) {
            //check if we try to complete just after the command name
            for(ParameterInt param : parser.getParameters()) {
                if(line.trim().equals(param.getName()))
                    return new ParsedCompleteObject(false, null, 0);
            }
            //else we try to complete an option
            return new ParsedCompleteObject(true);
        }

        String lastWord = Parser.findEscapedSpaceWordCloseToEnd(line);
        //last word might be an option
        if(lastWord.startsWith("-") ) {
            String secLastWord =
                    Parser.findEscapedSpaceWordCloseToEnd(
                            line.substring(0,line.length()-lastWord.length()));
            //second to last word also start with -
            if(secLastWord.startsWith("-")) {
                //do this for now
                return findCompleteObjectValue(line);
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
        //trying to complete a value (a bit crude, but will do for now)
        else
            return findCompleteObjectValue(line);
    }

    /**
     * Only called when we know that the last word is an option value
     */
    private ParsedCompleteObject findCompleteObjectValue(String line) {
        CommandLine cl = parser.parse(line, true);
        if(cl.getArguments().isEmpty()) {
            ParsedOption po = cl.getOptions().get(cl.getOptions().size()-1);
            return new ParsedCompleteObject( po.getLongName().isEmpty() ? po.getName() : po.getLongName(),
                    po.getValue(), po.getType(), true);
        }
        else {
            return new ParsedCompleteObject("",
                    cl.getArguments().get(cl.getArguments().size()-1),
                    parser.getParameters().get(0).getArgumentType(), false);
        }
    }

}

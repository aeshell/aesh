/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.completer.CompleterData;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.OptionInt;
import org.jboss.aesh.cl.internal.ParameterInt;
import org.jboss.aesh.console.Command;
import org.jboss.aesh.util.LoggerUtil;
import org.jboss.aesh.util.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineCompletionParser {

    private CommandLineParser parser;

    private static Logger logger = LoggerUtil.getLogger(CommandLineCompletionParser.class.getName());

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
                int offset = lastWord.length();
                while(lastWord.startsWith("-"))
                    lastWord = lastWord.substring(1);
                if(lastWord.length() == 0)
                    return new ParsedCompleteObject(false, null, offset);
                else if(parser.getParameter().findOption(lastWord) != null ||
                        parser.getParameter().findLongOption(lastWord) != null)
                    return findCompleteObjectValue(line, true);
                else
                    return new ParsedCompleteObject(false, null, offset);
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
                    cl.getArgument().getValues().get(cl.getArgument().getValues().size() - 1),
                    cl.getArgument().getType(), false);
        }
        //get the last option
        else {
            OptionInt po = cl.getOptions().get(cl.getOptions().size()-1);
            return new ParsedCompleteObject( po.getName().isEmpty() ? po.getShortName() : po.getName(),
                    endsWithSpace ? "" : po.getValue(), po.getType(), true);
        }
    }

    public CompleterData injectValuesAndComplete(ParsedCompleteObject completeObject, Command command,
                                                String buffer) {
        CompleterData completions = new CompleterData();
        if(completeObject.doDisplayOptions()) {
            logger.info("displayOptions");
            logger.info("CompleteObject: "+completeObject);
            //we have partial/full name
            logger.info("Name: "+completeObject.getName());
            if(completeObject.getName() != null && completeObject.getName().length() > 0) {
                if(parser.getParameter().findPossibleLongNamesWitdDash(completeObject.getName()).size() > 0) {
                    //only one param
                    if(parser.getParameter().findPossibleLongNamesWitdDash(completeObject.getName()).size() == 1) {
                        completions.addCompleterValue(buffer.substring(0, buffer.length()-completeObject.getOffset())+
                                parser.getParameter().findPossibleLongNamesWitdDash(completeObject.getName()).get(0));
                        //completions.setOffset( completeObject.getOffset());
                        //completions.setOffset( completions.getCompleterValues().get(0).length());
                    }
                    //multiple params
                    else {
                        completions.addAllCompleterValues(parser.getParameter().findPossibleLongNamesWitdDash(completeObject.getName()));
                    }

                }
            }
            else {
                if(parser.getParameter().getOptionLongNamesWithDash().size() > 1)
                    completions.addAllCompleterValues(parser.getParameter().getOptionLongNamesWithDash());
                else {
                    completions.addAllCompleterValues(parser.getParameter().getOptionLongNamesWithDash());
                    //completeOperation.setOffset(completeOperation.getCursor() - completeObject.getOffset());
                }

            }
        }
        //complete option value
        else if(completeObject.isOption()) {
            logger.info("buffer: "+buffer);
            logger.info("completeObject.getName(): "+completeObject.getName());
            //split the line on the option name. populate the object, then call the options completer
            String rest = buffer.substring(0, buffer.lastIndexOf(completeObject.getName()));
            //while(rest.endsWith("-"))
            //    rest = rest.substring(0, rest.length()-1);
            try {
                parser.populateObject(command, rest);
            } catch (CommandLineParserException e) {
                e.printStackTrace();
            }

            OptionInt currentOption = parser.getParameter().findLongOption(completeObject.getName());
            if(currentOption != null && currentOption.getCompleter() != null) {
                completions = currentOption.getCompleter().complete(completeObject.getValue());
            }
            if(completions.getCompleterValues().size() == 1 && completions.getOffset() > 0) {
                //completions.setOffset( completions.getOffset() + rest.length());
                completions.setOffset( completions.getOffset() + buffer.length() - rest.length());
            }

        }
        else if(completeObject.isArgument()) {
            String lastWord = Parser.findEscapedSpaceWordCloseToEnd(buffer);
            String rest = buffer.substring(0, buffer.length()-lastWord.length());
            try {
                parser.populateObject(command, rest);
            } catch (CommandLineParserException e) {
                e.printStackTrace();
            }
            if(parser.getParameter().getArgument() != null &&
                    parser.getParameter().getArgument().getCompleter() != null) {
                completions = parser.getParameter().getArgument().getCompleter().complete(completeObject.getValue());
            }
        }

        return completions;
    }

}

/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.internal.OptionInt;
import org.jboss.aesh.cl.internal.ParameterInt;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.util.Parser;

import java.util.List;

/**
 * A simple command line parser.
 * It parses a given string based on the Parameter given and
 * returns a {@link CommandLine}
 *
 * It can also print a formatted usage/help information.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineParser {

    private ParameterInt param;
    private static final String EQUALS = "=";

    public CommandLineParser(ParameterInt parameterInt) {
        this.param = parameterInt;
    }

    public CommandLineParser(String name, String usage) {
        this.param = new ParameterInt(name, usage);
    }

    /**
     * Add an option
     * Name or longName can be null
     * Both argument and type can be null
     *
     * @param name name (short) one char
     * @param longName multi character name
     * @param description a description of the option
     * @param hasValue if this option has value
     * @param argument what kind of argument this option can have
     * @param required is it required?
     * @param type what kind of type it is (not used)
     */
    public void addOption(char name, String longName, String description, boolean hasValue,
                          String argument, boolean required, boolean hasMultipleValues, Object type) {
        this.param.addOption(name, longName, description, hasValue, argument,
                required, hasMultipleValues, type);
    }

    /**
     * Add an option
     * Name or longName can be null
     *
     * @param name name (short) one char
     * @param longName multi character name
     * @param description a description of the option
     * @param hasValue if this option has value
     */
    public void addOption(char name, String longName, String description, boolean hasValue) {
        this.param.addOption(name, longName, description, hasValue, null, false, false, null);
    }

    protected ParameterInt getParameter() {
        return param;
    }

    /**
     * Returns a usage String based on the defined parameter and options.
     * Useful when printing "help" info etc.
     *
     */
    public String printHelp() {
        int maxLength = 0;
        int width = 80;
        for(OptionInt o : getParameter().getOptions())
            if(o.getFormattedLength() > maxLength)
                maxLength = o.getFormattedLength();

        StringBuilder sb = new StringBuilder();
        for(OptionInt o : getParameter().getOptions())
            sb.append(o.getFormattedOption(2, maxLength+4, width)).append(Config.getLineSeparator());
        return "Usage: "+getParameter().getName()+" "+getParameter().getUsage()+Config.getLineSeparator()+sb.toString();

    }

    /**
     * Parse a command line with the defined parameter as base of the rules.
     * If any options are found, but not defined in the parameter object an
     * IllegalArgumentException will be thrown.
     * Also, if a required option is not found or options specified with value,
     * but is not given any value an IllegalArgumentException will be thrown.
     *
     * The options found will be returned as a {@link CommandLine} object where
     * they can be queried after.
     *
     * @param line input
     * @return CommandLine
     * @throws IllegalArgumentException
     */
    public CommandLine parse(String line) throws IllegalArgumentException {
        param.clean();
        List<String> lines = Parser.findAllWords(line);
        CommandLine commandLine = new CommandLine();
        OptionInt active = null;
        //skip first entry since that's the name of the command
        for(int i=1; i < lines.size(); i++) {
            String parseLine = lines.get(i);
            //longName
            if(parseLine.startsWith("--")) {
                //make sure that we dont have any "active" options lying around
                if(active != null)
                    throw new IllegalArgumentException("Option: "+active.getName()+" must be given a value");

                active = findLongOption(parseLine.substring(2));
                if(active != null && active.isProperty()) {
                    if(parseLine.length() <= (2+active.getLongName().length()) ||
                        !parseLine.contains(EQUALS))
                        throw new IllegalArgumentException(
                                "Option "+active.getLongName()+", must be part of a property");

                    String name =
                            parseLine.substring(2+active.getLongName().length(),
                                    parseLine.indexOf(EQUALS));
                    String value = parseLine.substring( parseLine.indexOf(EQUALS)+1);

                    commandLine.addOption(new
                            ParsedOption(active.getName(), active.getLongName(),
                            new OptionProperty(name, value)));
                    active = null;
                }
                else if(active != null && (!active.hasValue() || active.getValue() != null)) {
                    commandLine.addOption(new ParsedOption(active.getName(), active.getLongName(), active.getValue()));
                    active = null;
                }
                else if(active == null)
                    throw new IllegalArgumentException("Option: "+parseLine+" is not a valid option for this command");
            }
            //name
            else if(parseLine.startsWith("-")) {
                //make sure that we dont have any "active" options lying around
                if(active != null)
                    throw new IllegalArgumentException("Option: "+active.getName()+" must be given a value");
                if(parseLine.length() != 2 && !parseLine.contains("="))
                    throw new IllegalArgumentException("Option: - must be followed by a valid operator");

                active = findOption(parseLine.substring(1));

                if(active != null && active.isProperty()) {
                    if(parseLine.length() <= 2 ||
                            !parseLine.contains(EQUALS))
                    throw new IllegalArgumentException(
                            "Option "+active.getName()+", must be part of a property");
                    String name =
                            parseLine.substring(2, // 2+char.length
                                    parseLine.indexOf(EQUALS));
                    String value = parseLine.substring( parseLine.indexOf(EQUALS)+1);

                    commandLine.addOption(new
                            ParsedOption(active.getName(), active.getLongName(),
                            new OptionProperty(name, value)));
                    active = null;
                }

                else if(active != null && (!active.hasValue() || active.getValue() != null)) {
                    commandLine.addOption(new ParsedOption(String.valueOf(active.getName()), active.getLongName(), active.getValue()));
                    active = null;
                }
                else if(active == null)
                    throw new IllegalArgumentException("Option: "+parseLine+" is not a valid option for this command");
            }
            else if(active != null) {
                if(active.hasMultipleValues()) {
                    if(parseLine.contains(String.valueOf(active.getValueSeparator()))) {
                        for(String value : parseLine.split(String.valueOf(active.getValueSeparator()))) {
                            active.addValue(value.trim());
                        }
                    }
                }
                else
                    active.addValue(parseLine);

                commandLine.addOption(new ParsedOption(active.getName(), active.getLongName(), active.getValues()));
                active = null;
            }
            //if no param is "active", we add it as an argument
            else {
                commandLine.addArgument(parseLine);
            }
        }

        //this will throw and IllegalArgumentException if needed
        checkForMissingRequiredOptions(commandLine);

        return commandLine;
    }

    private void checkForMissingRequiredOptions(CommandLine commandLine) throws IllegalArgumentException {
        for(OptionInt o : param.getOptions())
            if(o.isRequired()) {
                boolean found = false;
                for(ParsedOption po : commandLine.getOptions()) {
                    if(po.getName().equals(o.getName()) ||
                            po.getName().equals(o.getLongName()))
                        found = true;
                }
                if(!found)
                    throw new IllegalArgumentException("Option: "+o.getName()+" is required for this command.");
            }
    }

    private OptionInt findOption(String line) {
        OptionInt option = param.findOption(line);
        //simplest case
        if(option != null)
            return option;

        option = param.startWithOption(line);
        //if its a property, we'll parse it later
        if(option != null && option.isProperty())
            return option;
        if(option != null) {
           String rest = line.substring(option.getName().length());
            if(rest != null && rest.length() > 1 && rest.startsWith("=")) {
                option.addValue(rest.substring(1));
                return option;
            }
        }

        return null;
    }

    private OptionInt findLongOption(String line) {
        OptionInt option = param.findLongOption(line);
        //simplest case
        if(option != null)
            return option;

        option = param.startWithLongOption(line);
        //if its a property, we'll parse it later
        if(option != null && option.isProperty())
            return option;
        if(option != null) {
            String rest = line.substring(option.getLongName().length());
            if(rest != null && rest.length() > 1 && rest.startsWith("=")) {
                option.addValue(rest.substring(1));
                return option;
            }
        }

        return null;
    }
}

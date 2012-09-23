/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.cl;

import org.jboss.jreadline.cl.internal.OptionInt;
import org.jboss.jreadline.cl.internal.ParameterInt;
import org.jboss.jreadline.util.Parser;

import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineParser {

    private ParameterInt param;

    public CommandLineParser(ParameterInt parameterInt) {
        this.param = parameterInt;
    }

    public CommandLineParser(String usage) {
        this.param = new ParameterInt(usage);
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
                          String argument, boolean required, Object type) {
        this.param.addOption(name, longName, description, hasValue, argument,
                required, type);
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
        this.param.addOption(name, longName, description, hasValue, null, false, null);
    }

    protected ParameterInt getParameter() {
        return param;
    }

    public CommandLine parse(String line) throws IllegalArgumentException {
        List<String> lines = Parser.findAllWords(line);
        CommandLine commandLine = new CommandLine();
        OptionInt active = null;
        //skip first entry since that's the name of the command
        for(int i=1; i < lines.size(); i++) {
            String parseLine = lines.get(i);
            if(parseLine.startsWith("--")) {
                active = findLongOption(parseLine.substring(2));
                if(active != null && active.isProperty()) {
                    if(parseLine.length() <= (2+active.getLongName().length()) ||
                        !parseLine.contains(String.valueOf(active.getValueSeparator())))
                        throw new IllegalArgumentException(
                                "Option "+active.getLongName()+", must be part of a property");

                    String name =
                            parseLine.substring(2+active.getLongName().length(),
                                    parseLine.indexOf(active.getValueSeparator()));
                    String value = parseLine.substring( parseLine.indexOf(active.getValueSeparator()+1));

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

            else if(parseLine.startsWith("-")) {
                active = findOption(parseLine.substring(1));

                if(active != null && active.isProperty()) {
                    if(parseLine.length() <= 2 ||
                            !parseLine.contains(String.valueOf(active.getValueSeparator())))
                    throw new IllegalArgumentException(
                            "Option "+active.getLongName()+", must be part of a property");
                    String name =
                            parseLine.substring(3, // 2+char.length
                                    parseLine.indexOf(active.getValueSeparator()));
                    String value = parseLine.substring( parseLine.indexOf(active.getValueSeparator()+1));

                    commandLine.addOption(new
                            ParsedOption(active.getName(), active.getLongName(),
                            new OptionProperty(name, value)));
                    active = null;
                }

                if(active != null && !active.hasValue()) {
                    commandLine.addOption(new ParsedOption(String.valueOf(active.getName()), active.getLongName(), active.getValue()));
                    active = null;
                }
                else if(active == null)
                    throw new IllegalArgumentException("Option: "+parseLine+" is not a valid option for this command");
            }
            else if(active != null) {
                active.setValue(parseLine);
                commandLine.addOption(new ParsedOption(active.getName(), active.getLongName(), active.getValue()));
                active = null;
            }
            else {
                commandLine.addArgument(parseLine);
            }
        }

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
        else
            return null;
    }

    private OptionInt findLongOption(String line) {
        OptionInt option = param.findLongOption(line);
        //simplest case
        if(option != null)
            return option;

        option = param.startWithLongOption(line);
        if(option != null) {
            String rest = line.substring(option.getLongName().length());
            if(rest != null && rest.length() > 1 && rest.startsWith("=")) {
                option.setValue(rest.substring(1));
                return option;
            }
        }

        return null;
    }
}

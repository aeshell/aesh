/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.exception.ArgumentParserException;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.exception.OptionParserException;
import org.jboss.aesh.cl.exception.RequiredOptionException;
import org.jboss.aesh.cl.internal.OptionInt;
import org.jboss.aesh.cl.internal.ParameterInt;
import org.jboss.aesh.util.Parser;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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

    private ParameterInt parameter;
    private static final String EQUALS = "=";

    public CommandLineParser(ParameterInt parameter) {
        this.parameter = parameter;
    }

    public CommandLineParser(String name, String usage) {
        parameter = new ParameterInt(name, usage);
    }

    public ParameterInt getParameter() {
        return parameter;
    }

    /**
     * Returns a usage String based on the defined parameter and options.
     * Useful when printing "help" info etc.
     *
     */
    public String printHelp() {
        return parameter.printHelp();
    }

    /**
     * Parse a command line with the defined parameter as base of the rules.
     * If any options are found, but not defined in the parameter object an
     * CommandLineParserException will be thrown.
     * Also, if a required option is not found or options specified with value,
     * but is not given any value an OptionParserException will be thrown.
     *
     * The options found will be returned as a {@link CommandLine} object where
     * they can be queried after.
     *
     * @param line input
     * @return CommandLine
     * @throws CommandLineParserException
     */
    public CommandLine parse(String line) throws CommandLineParserException {
        return parse(line, false);
    }

    /**
     * Parse a command line with the defined parameter as base of the rules.
     * If any options are found, but not defined in the parameter object an
     * CommandLineParserException will be thrown.
     * Also, if a required option is not found or options specified with value,
     * but is not given any value an CommandLineParserException will be thrown.
     *
     * The options found will be returned as a {@link CommandLine} object where
     * they can be queried after.
     *
     * @param line input
     * @param ignoreMissingRequirements if we should ignore
     * @return CommandLine
     * @throws CommandLineParserException
     */
    public CommandLine parse(String line, boolean ignoreMissingRequirements) throws CommandLineParserException {
        List<String> lines = Parser.findAllWords(line);
        if(lines.size() > 0) {
            if(parameter.getName().equals(lines.get(0)))
                return doParse(parameter, lines, ignoreMissingRequirements, false);
        }
        throw new CommandLineParserException("Parameter:"+ parameter +", not found in: "+line);
    }

    public CommandLine parse(String line, boolean ignoreMissingRequirements,
                             boolean ignoreExceptions) throws CommandLineParserException {
        List<String> lines = Parser.findAllWords(line);
        if(lines.size() > 0) {
            if(parameter.getName().equals(lines.get(0)))
                return doParse(parameter, lines, ignoreMissingRequirements, ignoreExceptions);
        }
        throw new CommandLineParserException("Parameter:"+ parameter +", not found in: "+line);
    }

    private CommandLine doParse(ParameterInt param, List<String> lines,
                                boolean ignoreMissing, boolean ignoreException) throws CommandLineParserException {
        param.clear();
        CommandLine commandLine = new CommandLine();
        if(param.hasArgument())
            commandLine.setArgument(param.getArgument());
        OptionInt active = null;
        boolean addedArgument = false;
        try {
            //skip first entry since that's the name of the command
            for(int i=1; i < lines.size(); i++) {
                String parseLine = lines.get(i);
                //name
                if(parseLine.startsWith("--")) {
                    //make sure that we dont have any "active" options lying around
                    if(active != null)
                        throw new OptionParserException("Option: "+active.getDisplayName()+" must be given a value");

                    active = findLongOption(param, parseLine.substring(2));
                    if(active != null)
                        active.setLongNameUsed(true);
                    if(active != null && active.isProperty()) {
                        if(parseLine.length() <= (2+active.getName().length()) ||
                                !parseLine.contains(EQUALS))
                            throw new OptionParserException(
                                    "Option "+active.getDisplayName()+", must be part of a property");

                        String name =
                                parseLine.substring(2+active.getName().length(),
                                        parseLine.indexOf(EQUALS));
                        String value = parseLine.substring( parseLine.indexOf(EQUALS)+1);
                        if(value.length() < 1)
                            throw new OptionParserException("Option "+active.getDisplayName()+", must have a value");

                        active.addProperty(name, value);
                        commandLine.addOption(active);
                        active = null;
                        if(addedArgument)
                            throw new ArgumentParserException("An argument was given to an option that do not support it.");
                    }
                    else if(active != null && (!active.hasValue() || active.getValue() != null)) {
                        active.addValue("true");
                        commandLine.addOption(active);
                        active = null;
                        if(addedArgument)
                            throw new ArgumentParserException("An argument was given to an option that do not support it.");
                    }
                    else if(active == null)
                        throw new OptionParserException("Option: "+parseLine+" is not a valid option for this command");
                }
                //name
                else if(parseLine.startsWith("-")) {
                    //make sure that we dont have any "active" options lying around
                    if(active != null)
                        throw new OptionParserException("Option: "+active.getDisplayName()+" must be given a value");
                    if(parseLine.length() != 2 && !parseLine.contains("="))
                        throw new OptionParserException("Option: - must be followed by a valid operator");

                    active = findOption(param, parseLine.substring(1));
                    if(active != null)
                        active.setLongNameUsed(false);

                    if(active != null && active.isProperty()) {
                        if(parseLine.length() <= 2 ||
                                !parseLine.contains(EQUALS))
                            throw new OptionParserException(
                                    "Option "+active.getDisplayName()+", must be part of a property");
                        String name =
                                parseLine.substring(2, // 2+char.length
                                        parseLine.indexOf(EQUALS));
                        String value = parseLine.substring( parseLine.indexOf(EQUALS)+1);
                        if(value.length() < 1)
                            throw new OptionParserException("Option "+active.getDisplayName()+", must have a value");

                        active.addProperty(name, value);
                        commandLine.addOption(active);
                        active = null;
                        if(addedArgument)
                            throw new OptionParserException("An argument was given to an option that do not support it.");
                    }

                    else if(active != null && (!active.hasValue() || active.getValue() != null)) {
                        active.addValue("true");
                        commandLine.addOption(active);
                        active = null;
                        if(addedArgument)
                            throw new OptionParserException("An argument was given to an option that do not support it.");
                    }
                    else if(active == null)
                        throw new OptionParserException("Option: "+parseLine+" is not a valid option for this command");
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

                    commandLine.addOption(active);
                    active = null;
                    if(addedArgument)
                        throw new OptionParserException("An argument was given to an option that do not support it.");
                }
                //if no parameter is "active", we add it as an argument
                else {
                    if(param.getArgument() == null) {
                        throw new OptionParserException("An argument was given to a command that do not support it.");
                    }
                    else {
                        commandLine.addArgumentValue(parseLine);
                        addedArgument = true;
                    }
                }
            }
        }
        catch(CommandLineParserException clipe) {
            if(ignoreException)
                return commandLine;
            else
                throw clipe;
        }

        if(active != null && ignoreMissing) {
            commandLine.addOption(active);
        }

        //this will throw and CommandLineParserException if needed
        if(!ignoreMissing)
            checkForMissingRequiredOptions(param, commandLine);

        return commandLine;
    }

    private void checkForMissingRequiredOptions(ParameterInt param, CommandLine commandLine) throws CommandLineParserException {
        for(OptionInt o : param.getOptions())
            if(o.isRequired()) {
                boolean found = false;
                for(OptionInt po : commandLine.getOptions()) {
                    if(po.getShortName().equals(o.getShortName()) ||
                            po.getShortName().equals(o.getName()))
                        found = true;
                }
                if(!found)
                    throw new RequiredOptionException("Option: "+o.getDisplayName()+" is required for this command.");
            }
    }

    private OptionInt findOption(ParameterInt param, String line) {
        OptionInt option = param.findOption(line);
        //simplest case
        if(option != null)
            return option;

        option = param.startWithOption(line);
        //if its a property, we'll parse it later
        if(option != null && option.isProperty())
            return option;
        if(option != null) {
           String rest = line.substring(option.getShortName().length());
            if(rest != null && rest.length() > 1 && rest.startsWith("=")) {
                option.addValue(rest.substring(1));
                return option;
            }
        }

        return null;
    }

    private OptionInt findLongOption(ParameterInt param, String line) {
        OptionInt option = param.findLongOption(line);
        //simplest case
        if(option != null)
            return option;

        option = param.startWithLongOption(line);
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

    public void populateObject(Object instance, String line) throws CommandLineParserException {
        CommandLine cl = parse(line);
        for(OptionInt option: parameter.getOptions()) {
            if(cl.hasOption(option.getName()))
                cl.getOption(option.getName()).injectValueIntoField(instance);
            else
                resetField(instance, option.getFieldName());
        }
        if(cl.getArgument() != null && cl.getArgument().getValues().size() > 0) {
            cl.getArgument().injectValueIntoField(instance);
        }
        else if(cl.getArgument() != null)
            resetField(instance, cl.getArgument().getFieldName());
    }

    /**
     * Will parse the input line and populate the fields in the instance object specified by
     * the given annotations.
     * The instance object must be annotated with the CommandDefinition annotation @see CommandDefinition
     *
     * @param instance
     * @param line
     * @throws CommandLineParserException
     */
    public static void parseAndPopulate(Object instance, String line) throws CommandLineParserException {
        ParserGenerator.generateCommandLineParser(instance.getClass()).populateObject(instance, line);
    }

    private void resetField(Object instance, String fieldName) {
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            if(Modifier.isPrivate(field.getModifiers()))
                field.setAccessible(true);
            if(field.getType().isPrimitive()) {
                if(boolean.class.isAssignableFrom(field.getType()))
                    field.set(instance, false);
                else if(int.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0);
                else if(short.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0);
                else if(char.class.isAssignableFrom(field.getType()))
                    field.set(instance, '\u0000');
                else if(byte.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0);
                else if(long.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0L);
                else if(float.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0.0f);
                else if(double.class.isAssignableFrom(field.getType()))
                    field.set(instance, 0.0d);
            }
            else
                field.set(instance, null);
        }
        catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "CommandLineParser{" +
                "parameter=" + parameter +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommandLineParser)) return false;

        CommandLineParser that = (CommandLineParser) o;

        if (!parameter.equals(that.parameter)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return parameter.hashCode();
    }
}

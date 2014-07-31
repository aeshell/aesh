/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.internal;

import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.result.NullResultHandler;
import org.jboss.aesh.cl.result.ResultHandler;
import org.jboss.aesh.cl.validator.CommandValidator;
import org.jboss.aesh.cl.validator.NullCommandValidator;
import org.jboss.aesh.util.ReflectionUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Build a {@link org.jboss.aesh.cl.internal.ProcessedCommand} object using the Builder pattern.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ProcessedCommandBuilder {

    private String name;
    private String description;
    private CommandValidator<?> validator;
    private ResultHandler resultHandler;
    private ProcessedOption argument;
    private final List<ProcessedOption> options;
    private boolean groupCommand;


    public ProcessedCommandBuilder() {
        options = new ArrayList<>();
    }

    public ProcessedCommandBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ProcessedCommandBuilder description(String usage) {
        this.description = usage;
        return this;
    }

    public ProcessedCommandBuilder argument(ProcessedOption argument) {
        this.argument = argument;
        return this;
    }

    public ProcessedCommandBuilder validator(CommandValidator<?> validator) {
        this.validator = validator;
        return this;
    }

    public ProcessedCommandBuilder validator(Class<? extends CommandValidator> validator) {
        this.validator = initValidator(validator);
        return this;
    }

    private CommandValidator initValidator(Class<? extends CommandValidator> validator) {
        if(validator != null && !validator.equals(NullCommandValidator.class))
            return ReflectionUtil.newInstance(validator);
        else
            return new NullCommandValidator();
    }

    public ProcessedCommandBuilder resultHandler(Class<? extends ResultHandler> resultHandler) {
        this.resultHandler = initResultHandler(resultHandler);
        return this;
    }

    private ResultHandler initResultHandler(Class<? extends ResultHandler> resultHandler) {
        if(resultHandler != null && !resultHandler.equals(NullResultHandler.class))
            return ReflectionUtil.newInstance(resultHandler);
        else
            return new NullResultHandler();
    }

    public ProcessedCommandBuilder resultHandler(ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
        return this;
    }

    public ProcessedCommandBuilder addOption(ProcessedOption option) {
        this.options.add(option);
        return this;
    }

    public ProcessedCommandBuilder addOptions(List<ProcessedOption> options) {
        this.options.addAll(options);
        return this;
    }

    public ProcessedCommandBuilder groupCommand(boolean groupCommand) {
        this.groupCommand = groupCommand;
        return this;
    }

    public ProcessedCommand generateCommand() throws CommandLineParserException {
        if(name == null || name.length() < 1)
            throw new CommandLineParserException("The parameter name must be defined");

        if(validator == null)
            validator = new NullCommandValidator();

        if(resultHandler == null)
            resultHandler = new NullResultHandler();

        return  new ProcessedCommand(name, description, validator, resultHandler, argument, options, groupCommand);
    }
}

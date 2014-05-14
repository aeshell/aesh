/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.builder;

import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.result.ResultHandler;
import org.jboss.aesh.cl.validator.CommandValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Build a {@link org.jboss.aesh.cl.internal.ProcessedCommand} object using the Builder pattern.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandBuilder {

    private String name;
    private String description;
    private CommandValidator<?> validator;
    private ResultHandler resultHandler;
    private ProcessedOption argument;
    private List<ProcessedOption> options;


    public CommandBuilder() {
        options = new ArrayList<>();
    }

    public CommandBuilder name(String name) {
        this.name = name;
        return this;
    }

    public CommandBuilder description(String usage) {
        this.description = usage;
        return this;
    }

    public CommandBuilder argument(ProcessedOption argument) {
        this.argument = argument;
        return this;
    }

    public CommandBuilder validator(CommandValidator<?> validator) {
        this.validator = validator;
        return this;
    }

    public CommandBuilder resultHandler(ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
        return this;
    }

    public CommandBuilder addOption(ProcessedOption option) {
        this.options.add(option);
        return this;
    }

    public CommandBuilder addOptions(List<ProcessedOption> options) {
        this.options.addAll(options);
        return this;
    }

    public ProcessedCommand generateCommand() throws CommandLineParserException {
        if(name == null || name.length() < 1)
            throw new CommandLineParserException("The parameter name must be defined");
        return  new ProcessedCommand(name, description, validator, resultHandler, argument, options);
    }
}

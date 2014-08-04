/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.command.container;

import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.validator.CommandValidatorException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.InvocationProviders;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.parser.AeshLine;

import java.io.IOException;

/**
 * A CommandContainer hold reference to the Command and
 * the CommandLineParser generated from the Command.
 *
 * CommandRegistry will not put any CommandContainer objects in the registry
 * if it have any build errors.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CommandContainer extends AutoCloseable {

    /**
     * @return command
     */
    Command getCommand();

    /**
     * @return parser generated from Command
     */
    CommandLineParser getParser();

    /**
     * @return true if the CommandLineParser or Command generation generated any errors
     */
    boolean haveBuildError();

    /**
     * @return error message
     */
    String getBuildErrorMessage();

    CommandContainerResult executeCommand(AeshLine line, InvocationProviders invocationProviders,
                                          AeshContext aeshContext,
                                          CommandInvocation commandInvocation) throws CommandLineParserException, OptionValidatorException, CommandValidatorException, IOException, InterruptedException;
}

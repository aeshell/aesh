/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.parser;

import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.InvocationProviders;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CommandPopulator<T> {

    /**
     * Populate a Command instance with the values parsed from a command line
     * If any parser errors are detected it will throw an exception
     * @param instance command
     * @param line command line
     * @param aeshContext the context
     * @param validate based on rules given to the parser
     * @throws CommandLineParserException
     */
    void populateObject(T instance, CommandLine line, InvocationProviders invocationProviders,
                        AeshContext aeshContext, boolean validate) throws CommandLineParserException, OptionValidatorException;

}

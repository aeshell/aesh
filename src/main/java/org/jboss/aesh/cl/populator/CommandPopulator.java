/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.cl.populator;

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

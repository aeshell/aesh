/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.impl.operator;

import java.io.IOException;

import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.console.AeshContext;

public class ErrorRedirectionOperator implements ConfigurationOperator {

    private final AeshContext context;
    private final boolean append;
    private String argument;

    public ErrorRedirectionOperator(AeshContext context, boolean append) {
        this.context = context;
        this.append = append;
    }

    @Override
    public CommandInvocationConfiguration getConfiguration() throws IOException {
        CommandInvocationConfiguration configuration = new CommandInvocationConfiguration(context);
        configuration.setErrorRedirection(errorDelegate());
        return configuration;
    }

    @Override
    public void setArgument(String argument) {
        this.argument = argument;
    }

    public OutputDelegate errorDelegate() throws IOException {
        OutputRedirectionOperator fileOutput = new OutputRedirectionOperator(context, append);
        fileOutput.setArgument(argument);
        return fileOutput.getConfiguration().getOutputRedirection();
    }
}

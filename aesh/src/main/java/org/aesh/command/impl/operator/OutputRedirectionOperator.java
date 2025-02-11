/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.console.AeshContext;

/**
 *
 * @author jdenise@redhat.com
 */
public class OutputRedirectionOperator implements ConfigurationOperator {

    private class OutputDelegateImpl extends FileOutputDelegate {

        private OutputDelegateImpl(String file) throws IOException {
            super(context, file);
        }

        @Override
        protected BufferedWriter buildWriter(File f) throws IOException {
            return Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8);
        }
    }

    private CommandInvocationConfiguration config;
    private String argument;
    private final AeshContext context;

    public OutputRedirectionOperator(AeshContext context) {
        this.context = context;
    }

    @Override
    public CommandInvocationConfiguration getConfiguration() throws IOException {
        if (config == null) {
            config = new CommandInvocationConfiguration(context, new OutputDelegateImpl(argument));
        }
        return config;
    }

    @Override
    public void setArgument(String argument) {
        this.argument = argument;
    }
}

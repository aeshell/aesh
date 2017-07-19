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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.console.AeshContext;

/**
 *
 * @author jdenise@redhat.com
 */
public class PipeOperator extends EndOperator implements
        ConfigurationOperator, DataProvider {

    private class OutputDelegateImpl extends OutputDelegate {

        @Override
        protected BufferedWriter buildWriter() throws IOException {
            return new BufferedWriter(new OutputStreamWriter(stream));
        }
    }

    private ByteArrayOutputStream stream = new ByteArrayOutputStream();
    private final AeshContext context;
    private CommandInvocationConfiguration config;

    public PipeOperator(AeshContext context) {
        this.context = context;
    }

    @Override
    public CommandInvocationConfiguration getConfiguration() throws IOException {
        if (config == null) {
            config = new CommandInvocationConfiguration(context, new OutputDelegateImpl(), this);
        }
        return config;
    }

    @Override
    public void setArgument(String value) {
        // NOOP
    }

    @Override
    public BufferedInputStream getData() {
        return new BufferedInputStream(new ByteArrayInputStream(stream.toByteArray()));
    }
}

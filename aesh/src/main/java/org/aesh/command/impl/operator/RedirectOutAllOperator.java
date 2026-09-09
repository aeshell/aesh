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

import java.io.BufferedWriter;

import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.console.AeshContext;

public class RedirectOutAllOperator implements ConfigurationOperator {

    private final AeshContext context;

    public RedirectOutAllOperator(AeshContext context) {
        this.context = context;
    }

    @Override
    public CommandInvocationConfiguration getConfiguration() {
        return new CommandInvocationConfiguration(context);
    }

    @Override
    public void setArgument(String argument) {
    }

    public void mergeInto(CommandInvocationConfiguration configuration) {
        if (configuration == null)
            return;
        OutputDelegate target = configuration.getOutputRedirection();
        if (target != null)
            configuration.setErrorRedirection(new ForwardErrorDelegate(target));
    }

    private static final class ForwardErrorDelegate extends OutputDelegate {
        private final OutputDelegate target;

        private ForwardErrorDelegate(OutputDelegate target) {
            this.target = target;
        }

        @Override
        protected BufferedWriter buildWriter() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void write(String message) {
            target.write(message);
        }

        @Override
        public void close() {
        }
    }
}

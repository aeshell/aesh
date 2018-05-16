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
package org.aesh.command.invocation;

import java.io.BufferedInputStream;
import org.aesh.command.impl.operator.DataProvider;
import org.aesh.command.impl.operator.InputDelegate;
import org.aesh.command.impl.operator.OutputDelegate;
import org.aesh.readline.AeshContext;

/**
 *
 * @author jdenise@redhat.com
 */
public class CommandInvocationConfiguration {

    private OutputDelegate outputDelegate;
    private AeshContext context;
    private DataProvider dataProvider;
    private InputDelegate inputDelegate;

    public CommandInvocationConfiguration(AeshContext context) {
        this(context, null, null, null);
    }

    public CommandInvocationConfiguration(AeshContext context, OutputDelegate outputDelegate) {
        this(context, outputDelegate, null);
    }

    public CommandInvocationConfiguration(AeshContext context, DataProvider dataProvider) {
        this(context, null, null, dataProvider);
    }

    public CommandInvocationConfiguration(AeshContext context, OutputDelegate outputDelegate, InputDelegate inputDelegate, DataProvider dataProvider) {
        this.context = context;
        this.outputDelegate = outputDelegate;
        this.inputDelegate = inputDelegate;
        this.dataProvider = dataProvider;
    }

    public CommandInvocationConfiguration(AeshContext context, OutputDelegate outputDelegate, DataProvider dataProvider) {
        this(context, outputDelegate, null, dataProvider);
    }

    public CommandInvocationConfiguration(AeshContext context, InputDelegate inputDelegate) {
        this(context, null, inputDelegate, null);
    }

    public CommandInvocationConfiguration(AeshContext context, InputDelegate inputDelegate, DataProvider dataProvider) {
        this(context, null, inputDelegate, dataProvider);
    }

    public InputDelegate getInputRedirection() {
        return inputDelegate;
    }

    public OutputDelegate getOutputRedirection() {
        return outputDelegate;
    }

    public AeshContext getAeshContext() {
        return context;
    }

    public BufferedInputStream getPipedData() {
        return dataProvider == null ? null : dataProvider.getData();
    }

    public boolean hasOutputRedirection() {
        return getOutputRedirection() != null;
    }

    public boolean hasInputRedirection() {
        return getInputRedirection() != null;
    }

    public boolean hasPipedData() {
        return getPipedData() != null;
    }
}

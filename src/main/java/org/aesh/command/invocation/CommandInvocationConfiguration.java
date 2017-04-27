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
import java.io.ByteArrayInputStream;
import org.aesh.command.impl.operator.DataProvider;
import org.aesh.command.impl.operator.OutputDelegate;
import org.aesh.console.AeshContext;

/**
 *
 * @author jdenise@redhat.com
 */
public class CommandInvocationConfiguration {

    private static final BufferedInputStream EMPTY_INPUT
            = new BufferedInputStream(new ByteArrayInputStream(new byte[0]));
    private static final DataProvider EMPTY_DATA_PROVIDER = () -> {
        return EMPTY_INPUT;
    };
    private final OutputDelegate delegate;
    private final AeshContext context;
    private final DataProvider dataProvider;
    public CommandInvocationConfiguration(AeshContext context) {
        this(context, null, null);
    }

    public CommandInvocationConfiguration(AeshContext context, OutputDelegate delegate) {
        this(context, delegate, null);
    }

    public CommandInvocationConfiguration(AeshContext context, OutputDelegate delegate, DataProvider dataProvider) {
        this.context = context;
        this.delegate = delegate;
        this.dataProvider = dataProvider == null ? EMPTY_DATA_PROVIDER : dataProvider;
    }

    public OutputDelegate getOutputRedirection() {
        return delegate;
    }

    public AeshContext getAeshContext() {
        return context;
    }

    public BufferedInputStream getPipedData() {
        return dataProvider.getData();
    }
}

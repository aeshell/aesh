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
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.io.filter;

import org.aesh.readline.AeshContext;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.io.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

/**
 * @author <a href="mailto:00hf11@gmail.com">Helio Frota</a>
 */
public class NoDotNamesFilterTest {

    private Resource resource;

    @Before
    public void setUp() throws IOException {
        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);
        Settings settings = SettingsBuilder.builder()
                .inputStream(pis)
                .outputStream(new PrintStream(new ByteArrayOutputStream()))
                .logging(true)
                .build();

        AeshContext aeshContext = settings.aeshContext();
        resource = aeshContext.getCurrentWorkingDirectory().newInstance(".");
    }

    @Test
    public void testNoDotNamesFilter() {
        NoDotNamesFilter noDotNamesFilter = new NoDotNamesFilter();
        Assert.assertFalse(noDotNamesFilter.accept(resource));
    }
}

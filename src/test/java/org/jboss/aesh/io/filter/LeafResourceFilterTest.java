/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.io.filter;

import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.io.Resource;
import org.jboss.aesh.terminal.TestTerminal;
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
public class LeafResourceFilterTest {

    private Resource resource;

    @Before
    public void setUp() throws IOException {
        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);
        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .inputStream(pis)
                .outputStream(new PrintStream(new ByteArrayOutputStream()))
                .logging(true)
                .create();

        AeshContext aeshContext = settings.getAeshContext();
        resource = aeshContext.getCurrentWorkingDirectory().newInstance(".");
    }

    @Test
    public void testLeafResourceFilter() {
        LeafResourceFilter leafResourceFilter = new LeafResourceFilter();
        Assert.assertFalse(leafResourceFilter.accept(resource));
    }
}

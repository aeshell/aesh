/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.man;

import org.jboss.aesh.console.man.parser.ManFileParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;


/**
 * @author <a href="mailto:00hf11@gmail.com">Helio Frota</a>
 */
public class TerminalPageTest {

    private ManFileParser parser;

    @Before
    public void setUp() throws IOException {
        parser = new ManFileParser();
        parser.setInput(new FileInputStream("src/test/resources/asciitest1.txt"));
        parser.loadPage(80);
    }

    @Test
    public void testTerminalPage() throws IOException {
        TerminalPage tp = new TerminalPage(parser, 80);

        Assert.assertTrue(tp.hasData());
        Assert.assertTrue(tp.size() > 0);
        Assert.assertNotNull(tp.getLines());
        Assert.assertEquals("ASCIIDOC(1)", tp.getFileName());
        Assert.assertNotNull(tp.findWord("is"));
        Assert.assertTrue(tp.getLine(8).contains("DocBook"));

    }
}

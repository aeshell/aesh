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
package org.aesh.command.man;

import org.aesh.command.man.parser.ManFileParser;
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

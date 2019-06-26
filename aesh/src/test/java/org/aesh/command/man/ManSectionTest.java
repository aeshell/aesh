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

import org.aesh.terminal.utils.ANSI;
import org.aesh.terminal.utils.Config;
import org.aesh.command.man.parser.ManSection;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ManSectionTest {

    @Test
    public void testManSection() {
        List<String> input = new ArrayList<String>();
        input.add("OPTIONS");
        input.add("-------");
        input.add("*-a, --attribute*='ATTRIBUTE'::");
        input.add("  Define or delete document attribute. ");
        input.add(Config.getLineSeparator());
        input.add("*-b, --backend*='BACKEND'::");
        input.add("  Define or delete document attribute.");

        ManSection section = new ManSection().parseSection(input, 80);
        assertEquals("OPTIONS",section.getName());

        assertEquals(ANSI.BOLD+"OPTIONS"+ ANSI.DEFAULT_TEXT+
                Config.getLineSeparator()+
                "  "+ ANSI.BOLD+
                "-a, --attribute"+
                ANSI.DEFAULT_TEXT+
                "="+ ANSI.UNDERLINE+
                "ATTRIBUTE"+
                ANSI.DEFAULT_TEXT+ Config.getLineSeparator()+
                "    Define or delete document attribute. "+ Config.getLineSeparator()+
                " "+ Config.getLineSeparator()+
                "  "+ ANSI.BOLD+
                "-b, --backend"+
                ANSI.DEFAULT_TEXT+
                "="+ ANSI.UNDERLINE+
                "BACKEND"+
                ANSI.DEFAULT_TEXT+ Config.getLineSeparator()+
                "    Define or delete document attribute. "+ Config.getLineSeparator()+
                " "+ Config.getLineSeparator(),
                section.printToTerminal());
    }
}

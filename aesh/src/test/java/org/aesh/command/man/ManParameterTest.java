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

import org.aesh.command.man.parser.ManParameter;
import org.aesh.terminal.utils.ANSI;
import org.aesh.terminal.utils.Config;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ManParameterTest {

    @Test
    public void testParameter() {

        List<String> input = new ArrayList<String>();
        input.add("*-a, --attribute*='ATTRIBUTE'::");
        Assert.assertEquals("  "+ ANSI.BOLD+
                "-a, --attribute"+
                ANSI.DEFAULT_TEXT+
                "="+ ANSI.UNDERLINE+
                "ATTRIBUTE"+
                ANSI.DEFAULT_TEXT+ Config.getLineSeparator(),
                new ManParameter().parseParams(input, 80).printToTerminal());

        input.clear();
        input.add("*-a, --attribute*='ATTRIBUTE'::");
        input.add("   Backend output file format");
        assertEquals("  "+ ANSI.BOLD+
                "-a, --attribute"+
                ANSI.DEFAULT_TEXT+
                "="+ ANSI.UNDERLINE+
                "ATTRIBUTE"+
                ANSI.DEFAULT_TEXT+ Config.getLineSeparator()+
                "    Backend output file format"+" "+ Config.getLineSeparator()+
                " "+ Config.getLineSeparator(),
                new ManParameter().parseParams(input, 80).printToTerminal());



    }
}

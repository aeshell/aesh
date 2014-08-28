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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.console.man;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.man.parser.ManParameter;
import org.jboss.aesh.util.ANSI;
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
        assertEquals("  "+ ANSI.getBold()+
                "-a, --attribute"+
                ANSI.defaultText()+
                "="+ ANSI.getUnderline()+
                "ATTRIBUTE"+
                ANSI.defaultText()+ Config.getLineSeparator(),
                new ManParameter().parseParams(input, 80).printToTerminal());

        input.clear();
        input.add("*-a, --attribute*='ATTRIBUTE'::");
        input.add("   Backend output file format");
        assertEquals("  "+ ANSI.getBold()+
                "-a, --attribute"+
                ANSI.defaultText()+
                "="+ ANSI.getUnderline()+
                "ATTRIBUTE"+
                ANSI.defaultText()+ Config.getLineSeparator()+
                "    Backend output file format"+" "+ Config.getLineSeparator()+
                " "+ Config.getLineSeparator(),
                new ManParameter().parseParams(input, 80).printToTerminal());



    }
}

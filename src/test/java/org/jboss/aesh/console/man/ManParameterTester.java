/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
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
public class ManParameterTester {

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

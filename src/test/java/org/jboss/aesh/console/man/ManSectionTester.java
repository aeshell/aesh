/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.man;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.man.parser.ManSection;
import org.jboss.aesh.util.ANSI;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ManSectionTester {

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

        assertEquals(ANSI.getBold()+"OPTIONS"+ ANSI.defaultText()+
                Config.getLineSeparator()+
                "  "+ ANSI.getBold()+
                "-a, --attribute"+
                ANSI.defaultText()+
                "="+ ANSI.getUnderline()+
                "ATTRIBUTE"+
                ANSI.defaultText()+ Config.getLineSeparator()+
                "    Define or delete document attribute. "+ Config.getLineSeparator()+
                " "+ Config.getLineSeparator()+
                "  "+ ANSI.getBold()+
                "-b, --backend"+
                ANSI.defaultText()+
                "="+ ANSI.getUnderline()+
                "BACKEND"+
                ANSI.defaultText()+ Config.getLineSeparator()+
                "    Define or delete document attribute. "+ Config.getLineSeparator()+
                " "+ Config.getLineSeparator(),
                section.printToTerminal());


    }
}

/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.alias;

import org.jboss.aesh.console.Config;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AliasManagerTest {

    private AliasManager manager;

    @Before
    public void setTup() {
        try {
            manager = new AliasManager(new File("foo"), false, "aesh");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testParseAlias() {

        assertNull(manager.parseAlias("alias foo2='bar -s -h'"));
        assertNull(manager.parseAlias("alias foo=bar"));
        assertNull(manager.parseAlias("alias foo3=bar --help"));

        String out = manager.parseAlias("alias foo");
        assertEquals("alias foo='bar'"+ Config.getLineSeparator(), out);
        out = manager.parseAlias("alias foo2");
        assertEquals("alias foo2='bar -s -h'"+ Config.getLineSeparator(), out);
        out = manager.parseAlias("alias foo3");
        assertEquals("alias foo3='bar --help'"+ Config.getLineSeparator(), out);
        out = manager.parseAlias("alias");
        StringBuilder sb = new StringBuilder();
        sb.append("alias foo='bar'"+ Config.getLineSeparator())
                .append("alias foo2='bar -s -h'"+ Config.getLineSeparator())
                .append("alias foo3='bar --help'"+ Config.getLineSeparator());
        assertEquals(sb.toString(), out);
    }

    @Test
    public void testUnalias() {

        manager.parseAlias("alias foo2='bar -s -h'");
        manager.parseAlias("alias foo=bar");
        manager.parseAlias("alias foo3=bar --help");

        manager.removeAlias("unalias foo3");
        assertEquals("aesh: unalias: foo3: not found"+Config.getLineSeparator(), manager.removeAlias("unalias foo3"));
    }

    @Test
    public void testPrintAllAliases() {
        String alias = "alias foo='bar'";
        manager.parseAlias(alias);
        Assert.assertEquals(alias + Config.getLineSeparator(), manager.printAllAliases());
    }

}

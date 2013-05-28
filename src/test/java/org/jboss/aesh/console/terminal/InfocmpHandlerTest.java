/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.terminal;

import org.jboss.aesh.terminal.InfocmpHandler;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Ignore //only run locally as these codes are hardcoded and will/can change from machine to machine
public class InfocmpHandlerTest {

    @Test
    public void testInfocmpHandlerInts() {

        InfocmpHandler handler = InfocmpHandler.getInstance();
        int[] keyDown = handler.getAsInts("kcud1");

        Assert.assertArrayEquals(keyDown, new int[]{27,79,66});
        Assert.assertArrayEquals(handler.getAsInts("kend"), new int[]{27,79,70});
    }

    @Test
    public void testInfocmpHandlerString() {

        InfocmpHandler handler = InfocmpHandler.getInstance();
        assertEquals(handler.get("rc"), "\u001B8"); //save cursor
        assertEquals(handler.get("sc"), "\u001B7"); //save cursor

    }

}

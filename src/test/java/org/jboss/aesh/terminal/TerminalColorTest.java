/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TerminalColorTest {

    @Test
    public void testTerminalColor() {
        TerminalColor color = new TerminalColor( Color.DEFAULT, Color.BLACK);

        assertEquals("3"+Color.DEFAULT.getValue()+";4"+Color.BLACK.getValue(), color.toString());
    }
}

/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import org.junit.Test;

import static org.junit.Assert.assertTrue;


/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class KeyTest {

    @Test
    public void testContain() {
        assertTrue(Key.ESC.containKey(new int[]{27,10}));
    }
}

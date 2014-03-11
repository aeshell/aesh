/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.edit.mapper.KeyMapper;
import org.jboss.aesh.terminal.Key;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class KeyMapperTest {

    @Test
    public void testQuoteMapKeys() {
        if(Config.isOSPOSIXCompatible()) {
            KeyOperation o1 = KeyMapper.mapQuoteKeys("\"\\M-a","meta");
            assertEquals(new KeyOperation(Key.META_a, Operation.NO_ACTION), o1);
            o1 = KeyMapper.mapQuoteKeys("\"\\M-[D","backward-char");
            assertEquals(new KeyOperation(Key.LEFT, Operation.MOVE_PREV_CHAR), o1);
            o1 = KeyMapper.mapQuoteKeys("\"\\M-[C","forward-char");
            assertEquals(new KeyOperation(Key.RIGHT, Operation.MOVE_NEXT_CHAR), o1);
            o1 = KeyMapper.mapQuoteKeys("\"\\M-[A","previous-history");
            assertEquals(new KeyOperation(Key.UP, Operation.HISTORY_PREV), o1);
            o1 = KeyMapper.mapQuoteKeys("\"\\M-[B","next-history");
            assertEquals(new KeyOperation(Key.DOWN, Operation.HISTORY_NEXT), o1);

            KeyOperation o2 = KeyMapper.mapQuoteKeys("\"\\M-\\C-D", "backward-char");
            assertEquals(new KeyOperation(Key.META_CTRL_D, Operation.MOVE_PREV_CHAR), o2);
            o2 = KeyMapper.mapQuoteKeys("\"\\C-\\M-d", "backward-char");
            assertEquals(new KeyOperation(Key.META_CTRL_D, Operation.MOVE_PREV_CHAR), o2);
            o2 = KeyMapper.mapQuoteKeys("\"\\C-a", "beginning-of-line");
            assertEquals(new KeyOperation(Key.CTRL_A, Operation.MOVE_BEGINNING), o2);
        }
    }

    @Test
    public void testMapKeys() {
        if(Config.isOSPOSIXCompatible()) {
            KeyOperation o1 = KeyMapper.mapKeys("M-a","meta");
            assertEquals(new KeyOperation(Key.META_a, Operation.NO_ACTION), o1);
            o1 = KeyMapper.mapKeys("M-[D","backward-char");
            assertEquals(new KeyOperation(Key.LEFT, Operation.MOVE_PREV_CHAR), o1);
            o1 = KeyMapper.mapKeys("M-[C","forward-char");
            assertEquals(new KeyOperation(Key.RIGHT, Operation.MOVE_NEXT_CHAR), o1);
            o1 = KeyMapper.mapKeys("M-[A","previous-history");
            assertEquals(new KeyOperation(Key.UP, Operation.HISTORY_PREV), o1);
            o1 = KeyMapper.mapKeys("M-[B","next-history");
            assertEquals(new KeyOperation(Key.DOWN, Operation.HISTORY_NEXT), o1);

            KeyOperation o2 = KeyMapper.mapKeys("M-C-d", "backward-char");
            assertEquals(new KeyOperation(Key.META_CTRL_D, Operation.MOVE_PREV_CHAR), o2);
            o2 = KeyMapper.mapKeys("C-M-D", "backward-char");
            assertEquals(new KeyOperation(Key.META_CTRL_D, Operation.MOVE_PREV_CHAR), o2);
            o2 = KeyMapper.mapKeys("C-a", "beginning-of-line");
            assertEquals(new KeyOperation(Key.CTRL_A, Operation.MOVE_BEGINNING), o2);
        }
    }
}

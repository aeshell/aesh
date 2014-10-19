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

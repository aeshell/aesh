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
package org.jboss.aesh.terminal;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.readline.Action;
import org.jboss.aesh.readline.EventQueue;
import org.jboss.aesh.readline.actions.ActionMapper;
import org.jboss.aesh.readline.editing.EditMode;
import org.jboss.aesh.readline.editing.EditModeBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class KeyTest {

    @Test
    public void testContain() {
        assertTrue(Key.ESC.containKey(new int[]{27, 10}));
    }

    @Test
    public void testOtherOperations() {

        EditMode editMode = new EditModeBuilder(EditMode.Mode.EMACS).create();

        Key up = Key.UP;

        Action action = editMode.parse(up);

        assertEquals(action.name(), ActionMapper.mapToAction("previous-history").name());

        if (Config.isOSPOSIXCompatible()) {
            int[] doubleUpKey = new int[6];
            for (int i = 0; i < 6; i++) {
                if (i > 2)
                    doubleUpKey[i] = up.getKeyValues()[i - 3];
                else
                    doubleUpKey[i] = up.getKeyValues()[i];
            }

            EventQueue eventQueue = new EventQueue(editMode);

            eventQueue.append(doubleUpKey);

            action = editMode.parse(eventQueue.next());
            assertEquals(action.name(), ActionMapper.mapToAction("previous-history").name());
        }

    }

    @Test
    public void testFindStartKey() {
        int[] input = new int[]{2, 27, 65};
        Key inc = Key.findStartKey(input);
        assertEquals(Key.CTRL_B, inc);
        System.arraycopy(input, inc.getKeyValues().length, input, 0, input.length - inc.getKeyValues().length);
        inc = Key.findStartKey(input);
        assertEquals(Key.ESC, inc);
        System.arraycopy(input, inc.getKeyValues().length, input, 0, input.length - inc.getKeyValues().length);
        inc = Key.findStartKey(input);
        assertEquals(Key.A, inc);

        if (Config.isOSPOSIXCompatible()) {
            input = new int[]{32, 27, 91, 65, 10};
            inc = Key.findStartKey(input);
            assertEquals(Key.SPACE, inc);
            System.arraycopy(input, inc.getKeyValues().length, input, 0, input.length - inc.getKeyValues().length);
            inc = Key.findStartKey(input);
            assertEquals(Key.UP, inc);
            System.arraycopy(input, inc.getKeyValues().length, input, 0, input.length - inc.getKeyValues().length);
            inc = Key.findStartKey(input);
            assertEquals(Key.ENTER, inc);
            System.arraycopy(input, inc.getKeyValues().length, input, 0, input.length - inc.getKeyValues().length);
        }
    }

    @Test
    public void testFindStartKeyPosition() {
        int[] input = new int[]{2, 27, 65};
        Key inc = Key.findStartKey(input, 0);
        assertEquals(Key.CTRL_B, inc);
        inc = Key.findStartKey(input, 1);
        assertEquals(Key.ESC, inc);
        System.arraycopy(input, inc.getKeyValues().length, input, 0, input.length - inc.getKeyValues().length);
        inc = Key.findStartKey(input, 2);
        assertEquals(Key.A, inc);

        if (Config.isOSPOSIXCompatible()) {
            input = new int[]{32, 27, 91, 65, 10};
            inc = Key.findStartKey(input, 0);
            assertEquals(Key.SPACE, inc);
            inc = Key.findStartKey(input, 1);
            assertEquals(Key.UP, inc);
            inc = Key.findStartKey(input, 4);
            assertEquals(Key.ENTER, inc);

            input = new int[]{10};
            inc = Key.findStartKey(input, 0);
            assertEquals(Key.ENTER, inc);

        }
    }

    @Test
    public void testIsPrintable() {
        assertTrue(Key.a.isPrintable());
        assertTrue(Key.P.isPrintable());
        assertTrue(Key.RIGHT_CURLY_BRACKET.isPrintable());

        assertFalse(Key.BACKSPACE.isPrintable());

        assertTrue(Key.isPrintable(new int[]{197}));
        assertTrue(Key.isPrintable(new int[]{229}));
        if (!Config.isOSPOSIXCompatible())
            assertFalse(Key.isPrintable(new int[]{Key.WINDOWS_ESC.getFirstValue()}));
    }

    @Test
    public void testHomeEndKeysNotPrintable() {
        assertFalse(Key.HOME.isPrintable());
        assertFalse(Key.END.isPrintable());
    }
}

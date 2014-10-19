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
import org.jboss.aesh.edit.KeyOperation;
import org.jboss.aesh.edit.KeyOperationFactory;
import org.jboss.aesh.edit.KeyOperationManager;
import org.jboss.aesh.edit.actions.Operation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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

        KeyOperationManager manager = new KeyOperationManager();
        manager.addOperations(KeyOperationFactory.generateEmacsMode());

        Key up = Key.UP;

        KeyOperation ko = manager.findOperation(up.getKeyValues());
        assertEquals(up, ko.getKey());
        assertEquals(ko.getOperation(), Operation.HISTORY_PREV);

        if (Config.isOSPOSIXCompatible()) {
            int[] doubleUpKey = new int[6];
            for (int i = 0; i < 6; i++) {
                if (i > 2)
                    doubleUpKey[i] = up.getKeyValues()[i - 3];
                else
                    doubleUpKey[i] = up.getKeyValues()[i];
            }

            ko = manager.findOperation(doubleUpKey);
            assertEquals(up, ko.getKey());
            assertEquals(ko.getOperation(), Operation.HISTORY_PREV);

            doubleUpKey[2] = 3212;
            ko = manager.findOperation(doubleUpKey);
            assertEquals(Key.ESC, ko.getKey());
            assertEquals(ko.getOperation(), Operation.NO_ACTION);

            doubleUpKey = new int[7];
            for (int i = 0; i < 6; i++) {
                if (i > 2)
                    doubleUpKey[i] = up.getKeyValues()[i - 3];
                else
                    doubleUpKey[i] = up.getKeyValues()[i];
            }
            doubleUpKey[6] = 42;

            ko = manager.findOperation(doubleUpKey);
            assertEquals(Key.ESC, ko.getKey());
            assertEquals(ko.getOperation(), Operation.NO_ACTION);

            doubleUpKey = new int[4];
            for (int i = 0; i < 4; i++)
                doubleUpKey[i] = 1000 + i;

            ko = manager.findOperation(doubleUpKey);
            assertNull(ko);
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

/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit;

import junit.framework.TestCase;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;

/**
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class KeyOperationManagerTest extends TestCase {

    public KeyOperationManagerTest(String test) {
        super(test);
    }

    public void testOperations() {
        KeyOperationManager kom = new KeyOperationManager();
        kom.addOperation(new KeyOperation(Key.DOWN, Operation.HISTORY_NEXT));
        kom.addOperation(new KeyOperation(Key.UP, Operation.HISTORY_PREV));
        kom.addOperation(new KeyOperation(Key.RIGHT, Operation.MOVE_NEXT_CHAR));
        assertEquals(3, kom.getOperations().size());

        kom.addOperation(new KeyOperation(Key.RIGHT, Operation.MOVE_BEGINNING));
        assertEquals(3, kom.getOperations().size());

        kom.clear();
        kom.addOperation(new KeyOperation(Key.PGUP, Operation.MOVE_BEGINNING));
        assertEquals(1, kom.getOperations().size());
        kom.addOperation(new KeyOperation(Key.PGDOWN, Operation.MOVE_NEXT_CHAR));
        assertEquals(2, kom.getOperations().size());

    }

    public void testFindOperation() {
        KeyOperationManager kom = new KeyOperationManager();
        kom.addOperation(new KeyOperation(Key.j, Operation.HISTORY_NEXT));
        kom.addOperation(new KeyOperation(Key.k, Operation.HISTORY_PREV));
        kom.addOperation(new KeyOperation(Key.l, Operation.MOVE_NEXT_CHAR));

       assertEquals(new KeyOperation(Key.l, Operation.MOVE_NEXT_CHAR),
               kom.findOperation(new int[]{108}));
    }
}

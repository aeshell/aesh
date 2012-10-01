/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit;

import junit.framework.TestCase;
import org.jboss.aesh.edit.actions.Operation;

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
        kom.addOperation(new KeyOperation(new int[]{27,1}, Operation.HISTORY_NEXT));
        kom.addOperation(new KeyOperation(new int[]{27,2}, Operation.HISTORY_PREV));
        kom.addOperation(new KeyOperation(new int[]{27,1,23}, Operation.MOVE_NEXT_CHAR));
        assertEquals(3, kom.getOperations().size());

        kom.addOperation(new KeyOperation(new int[]{27,1}, Operation.MOVE_BEGINNING));
        assertEquals(3, kom.getOperations().size());

        kom.clear();
        kom.addOperation(new KeyOperation(new int[]{27,1}, Operation.MOVE_BEGINNING));
        assertEquals(1, kom.getOperations().size());
        kom.addOperation(new KeyOperation(new int[]{27,1,23}, Operation.MOVE_NEXT_CHAR));
        assertEquals(2, kom.getOperations().size());

    }

    public void testFindOperation() {
        KeyOperationManager kom = new KeyOperationManager();
        kom.addOperation(new KeyOperation(new int[]{27,1}, Operation.HISTORY_NEXT));
        kom.addOperation(new KeyOperation(new int[]{27,2}, Operation.HISTORY_PREV));
        kom.addOperation(new KeyOperation(new int[]{27,1,23}, Operation.MOVE_NEXT_CHAR));

       assertEquals(new KeyOperation(new int[]{27,1,23}, Operation.MOVE_NEXT_CHAR),
               kom.findOperation(new int[]{27,1,23}));
    }
}

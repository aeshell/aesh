/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.jreadline.edit;

import junit.framework.TestCase;
import org.jboss.jreadline.edit.actions.Operation;

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

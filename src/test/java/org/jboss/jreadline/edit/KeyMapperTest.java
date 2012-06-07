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
import org.jboss.jreadline.edit.mapper.KeyMapper;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class KeyMapperTest extends TestCase {
    
    public KeyMapperTest(String test) {
        super(test);
    }
    
    public void testQuoteMapKeys() {
        KeyOperation o1 = KeyMapper.mapQuoteKeys("\"\\M-a","meta");
        assertEquals(new KeyOperation(new int[]{27,97}, Operation.NO_ACTION), o1);
        o1 = KeyMapper.mapQuoteKeys("\"\\M-[D","backward-char");
        assertEquals(new KeyOperation(new int[]{27,91,68}, Operation.MOVE_PREV_CHAR), o1);
        o1 = KeyMapper.mapQuoteKeys("\"\\M-[C","forward-char");
        assertEquals(new KeyOperation(new int[]{27,91,67}, Operation.MOVE_NEXT_CHAR), o1);
        o1 = KeyMapper.mapQuoteKeys("\"\\M-[A","previous-history");
        assertEquals(new KeyOperation(new int[]{27,91,65}, Operation.HISTORY_PREV), o1);
        o1 = KeyMapper.mapQuoteKeys("\"\\M-[B","next-history");
        assertEquals(new KeyOperation(new int[]{27,91,66}, Operation.HISTORY_NEXT), o1);

        KeyOperation o2 = KeyMapper.mapQuoteKeys("\"\\M-\\C-D", "backward-char");
        assertEquals(new KeyOperation(new int[]{27,4}, Operation.MOVE_PREV_CHAR), o2);
        o2 = KeyMapper.mapQuoteKeys("\"\\C-\\M-d", "backward-char");
        assertEquals(new KeyOperation(new int[]{27,4}, Operation.MOVE_PREV_CHAR), o2);
        o2 = KeyMapper.mapQuoteKeys("\"\\C-a", "beginning-of-line");
        assertEquals(new KeyOperation(new int[]{1}, Operation.MOVE_BEGINNING), o2);
    }

    public void testMapKeys() {
        KeyOperation o1 = KeyMapper.mapKeys("M-a","meta");
        assertEquals(new KeyOperation(new int[]{27,97}, Operation.NO_ACTION), o1);
        o1 = KeyMapper.mapKeys("M-[D","backward-char");
        assertEquals(new KeyOperation(new int[]{27,91,68}, Operation.MOVE_PREV_CHAR), o1);
        o1 = KeyMapper.mapKeys("M-[C","forward-char");
        assertEquals(new KeyOperation(new int[]{27,91,67}, Operation.MOVE_NEXT_CHAR), o1);
        o1 = KeyMapper.mapKeys("M-[A","previous-history");
        assertEquals(new KeyOperation(new int[]{27,91,65}, Operation.HISTORY_PREV), o1);
        o1 = KeyMapper.mapKeys("M-[B","next-history");
        assertEquals(new KeyOperation(new int[]{27,91,66}, Operation.HISTORY_NEXT), o1);

        KeyOperation o2 = KeyMapper.mapKeys("M-C-d", "backward-char");
        assertEquals(new KeyOperation(new int[]{27,4}, Operation.MOVE_PREV_CHAR), o2);
        o2 = KeyMapper.mapKeys("C-M-D", "backward-char");
        assertEquals(new KeyOperation(new int[]{27,4}, Operation.MOVE_PREV_CHAR), o2);
        o2 = KeyMapper.mapKeys("C-a", "beginning-of-line");
        assertEquals(new KeyOperation(new int[]{1}, Operation.MOVE_BEGINNING), o2);
    }
}

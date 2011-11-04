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
package org.jboss.jreadline.history;

import org.jboss.jreadline.JReadlineTestCase;
import org.jboss.jreadline.TestBuffer;

import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class HistoryTest extends JReadlineTestCase {

    public HistoryTest(String test) {
        super(test);
    }

    public void testHistory() throws IOException {
        TestBuffer buffer = new TestBuffer();

        buffer.append("1234")
                .append(TestBuffer.getNewLine()).append("567")
                .append(TestBuffer.getNewLine()).append(TestBuffer.EMACS_HISTORY_PREV)
                .append(TestBuffer.getNewLine());

        assertEquals("567", buffer);


        buffer = new TestBuffer();
        buffer.append("1234")
                .append(TestBuffer.getNewLine()).append("567")
                .append(TestBuffer.getNewLine()).append("89")
                .append(TestBuffer.getNewLine())
                .append(TestBuffer.EMACS_HISTORY_PREV)
                .append(TestBuffer.EMACS_HISTORY_PREV)
                .append(TestBuffer.EMACS_HISTORY_PREV).append(TestBuffer.getNewLine());

        assertEquals("1234", buffer);

    }

    public void testHistorySize() {
        History history = new InMemoryHistory(20);

        for(int i=0; i < 25; i++)
            history.push(new StringBuilder().append(i));


        assertEquals(20, history.size());
        assertEquals("24", history.getPreviousFetch().toString());
    }
}

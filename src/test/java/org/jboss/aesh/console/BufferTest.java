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
package org.jboss.aesh.console;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class BufferTest {

    @Test
    public void testMove() {
        String input = "foo-bar";
        Buffer buffer = new Buffer(true, null);
        buffer.write(input);

        char[] out = buffer.move(-1, 80);
        char[] expected = new char[] {(char) 27,'[','1','D'};
        assertEquals(new String(expected), new String(out));
        assertEquals(6, buffer.getCursor());

        out = buffer.move(1, 80);
        expected = new char[] {(char) 27,'[','1','C'};
        assertEquals(new String(expected), new String(out));
        assertEquals(7, buffer.getCursor());


        out = buffer.move(-5, 80);
        expected = new char[] {(char) 27,'[','5','D'};
        assertEquals(new String(expected), new String(out));
        assertEquals(2, buffer.getCursor());

        out = buffer.move(-3, 80);
        expected = new char[] {(char) 27,'[','2','D'};
        assertEquals(new String(expected), new String(out));
        assertEquals(0, buffer.getCursor());

        out = buffer.move(-3, 80);
        expected = new char[0];
        assertEquals(new String(expected), new String(out));
        assertEquals(0, buffer.getCursor());


        out = buffer.move(10, 80);
        expected = new char[] {(char) 27,'[','7','C'};
        assertEquals(new String(expected), new String(out));
        assertEquals(7, buffer.getCursor());

        String prompt = "foo@bar:";

        buffer.reset(new Prompt(prompt));
        buffer.write(input);
        //buffer.setCursor(5);

        out = buffer.move(-1, 80);
        expected = new char[] {(char) 27,'[','1','D'};
        assertEquals(new String(expected), new String(out));
        assertEquals(input.length()-1, buffer.getCursor());
        assertEquals(prompt.length()+input.length(), buffer.getCursorWithPrompt());

        out = buffer.move(1, 80);
        expected = new char[] {(char) 27,'[','1','C'};
        assertEquals(new String(expected), new String(out));
        assertEquals(7, buffer.getCursor());

        out = buffer.move(-5, 80);
        expected = new char[] {(char) 27,'[','5','D'};
        assertEquals(new String(expected), new String(out));
        assertEquals(2, buffer.getCursor());

        out = buffer.move(-6, 80);
        expected = new char[] {(char) 27,'[','2','D'};
        assertEquals(new String(expected), new String(out));
        assertEquals(0, buffer.getCursor());

        out = buffer.move(10, 80);
        expected = new char[] {(char) 27,'[','7','C'};
        assertEquals(new String(expected), new String(out));
        assertEquals(7, buffer.getCursor());

        buffer.reset(new Prompt(">"));
        buffer.write("foo");
        buffer.move(-4, 80, true);

        assertEquals(2, buffer.getCursorWithPrompt());

        buffer.move(5, 80, true);
        assertEquals(4, buffer.getCursorWithPrompt());

        buffer.move(-4, 80, true);
        assertEquals(2, buffer.getCursorWithPrompt());
    }

    @Test
    public void testPrintAnsi() {
        char[] expected = new char[] {(char) 27, '[', 'J'};
        assertEquals(new String(expected), new String(Buffer.printAnsi("J")));

        expected = new char[] {(char) 27, '[', 'J','p'};
        assertEquals(new String(expected), new String(Buffer.printAnsi("Jp")));

        expected = new char[] {(char) 27, '[', ' ',' ',' ',' '};
        assertEquals(new String(expected), new String(Buffer.printAnsi("\t")));

        expected = new char[] {(char) 27, '[', ' ',' ',' ',' ','B'};
        assertEquals(new String(expected), new String(Buffer.printAnsi("\tB")));

    }


}

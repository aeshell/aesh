/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class BufferTest extends TestCase {


    public BufferTest(String name) {
        super(name);
    }

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

package org.jboss.jreadline.console;

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
        Buffer buffer = new Buffer(null);
        buffer.write(input);

        char[] out = buffer.move(-1);
        char[] expected = new char[] {(char) 27,'[','1','D'};
        assertEquals(new String(expected), new String(out));
        assertEquals(6, buffer.getCursor());

        out = buffer.move(1);
        expected = new char[] {(char) 27,'[','1','C'};
        assertEquals(new String(expected), new String(out));
        assertEquals(7, buffer.getCursor());


        out = buffer.move(-5);
        expected = new char[] {(char) 27,'[','5','D'};
        assertEquals(new String(expected), new String(out));
        assertEquals(2, buffer.getCursor());

        out = buffer.move(-3);
        expected = new char[] {(char) 27,'[','2','D'};
        assertEquals(new String(expected), new String(out));
        assertEquals(0, buffer.getCursor());

        out = buffer.move(-3);
        expected = new char[0];
        assertEquals(new String(expected), new String(out));
        assertEquals(0, buffer.getCursor());


        out = buffer.move(10);
        expected = new char[] {(char) 27,'[','7','C'};
        assertEquals(new String(expected), new String(out));
        assertEquals(7, buffer.getCursor());

        String prompt = "foo@bar:";

        buffer.reset(prompt);
        buffer.write(input);
        //buffer.setCursor(5);

        out = buffer.move(-1);
        expected = new char[] {(char) 27,'[','1','D'};
        assertEquals(new String(expected), new String(out));
        assertEquals(input.length()-1, buffer.getCursor());
        assertEquals(prompt.length()+input.length()-1, buffer.getCursorWithPrompt());

        out = buffer.move(1);
        expected = new char[] {(char) 27,'[','1','C'};
        assertEquals(new String(expected), new String(out));
        assertEquals(7, buffer.getCursor());

        out = buffer.move(-5);
        expected = new char[] {(char) 27,'[','5','D'};
        assertEquals(new String(expected), new String(out));
        assertEquals(2, buffer.getCursor());

        out = buffer.move(-6);
        expected = new char[] {(char) 27,'[','2','D'};
        assertEquals(new String(expected), new String(out));
        assertEquals(0, buffer.getCursor());

        out = buffer.move(10);
        expected = new char[] {(char) 27,'[','7','C'};
        assertEquals(new String(expected), new String(out));
        assertEquals(7, buffer.getCursor());
    }

    public void testPrintAnsi() {
        Buffer buffer = new Buffer();

        char[] expected = new char[] {(char) 27, '[', 'J'};
        assertEquals(new String(expected), new String(buffer.printAnsi("J")));

        expected = new char[] {(char) 27, '[', 'J','p'};
        assertEquals(new String(expected), new String(buffer.printAnsi("Jp")));

        expected = new char[] {(char) 27, '[', ' ',' ',' ',' '};
        assertEquals(new String(expected), new String(buffer.printAnsi("\t")));

        expected = new char[] {(char) 27, '[', ' ',' ',' ',' ','B'};
        assertEquals(new String(expected), new String(buffer.printAnsi("\tB")));

    }

}

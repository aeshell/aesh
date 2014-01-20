package org.jboss.aesh.terminal;

import org.jboss.aesh.util.ANSI;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TerminalCharacterTest {

    private static byte BOLD_OFF = 22;

    @Test
    public void testTerminalCharacterAsString() {
        TerminalCharacter character = new TerminalCharacter('c', new TerminalTextStyle(CharacterType.BOLD));

        assertEquals(ANSI.getStart()+
                CharacterType.BOLD.getValue()+";"+
                3+ Color.DEFAULT.getValue()+";"+
                4+Color.DEFAULT.getValue()+
                "mc",
                character.toString());
    }

    @Test
    public void testPrevCharacterAsString() {
        TerminalCharacter c1 = new TerminalCharacter('c', new TerminalTextStyle(CharacterType.BOLD));

        assertEquals(ANSI.getStart() +
                CharacterType.BOLD.getValue() + ";" +
                3+Color.DEFAULT.getValue() + ";" +
                4+Color.DEFAULT.getValue() +
                "mc",
                c1.toString());

        TerminalCharacter c2 = new TerminalCharacter('f', new TerminalColor(Color.DEFAULT, Color.BLUE),
                new TerminalTextStyle(CharacterType.CROSSED_OUT));

         assertEquals(ANSI.getStart() +
                 BOLD_OFF + ";" +
                CharacterType.CROSSED_OUT.getValue() + ";" +
                4+Color.BLUE.getValue() +
                "mf",
                c2.toString(c1));

        TerminalCharacter c3 = new TerminalCharacter('f', new TerminalColor(Color.RED, Color.BLUE));
        TerminalCharacter c4 = new TerminalCharacter('f', new TerminalColor(Color.RED, Color.BLUE));

         assertEquals("f", c4.toString(c3));

        c4 = new TerminalCharacter('g', new TerminalColor(Color.RED, Color.BLUE), new TerminalTextStyle(CharacterType.BOLD));

         assertEquals(ANSI.getStart() + CharacterType.BOLD.getValue() + "mg", c4.toString(c3));

        c3 = new TerminalCharacter('f', new TerminalColor(Color.RED, Color.BLUE), new TerminalTextStyle(CharacterType.BOLD));
        c4 = new TerminalCharacter('g');

        assertEquals(ANSI.getStart() + "22;39;49mg", c4.toString(c3));

        c4 = new TerminalCharacter('g', new TerminalTextStyle(CharacterType.INVERT));

        assertEquals(ANSI.getStart() + "22;7;39;49mg", c4.toString(c3));
    }

    @Test
    public void testEqualsIgnoreCharacter() {
        TerminalCharacter c1 = new TerminalCharacter('a', new TerminalColor(Color.RED, Color.BLUE));
        TerminalCharacter c2 = new TerminalCharacter('b', new TerminalColor(Color.RED, Color.BLUE));

        assertTrue(c1.equalsIgnoreCharacter(c2));

    }


}

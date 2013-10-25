package org.jboss.aesh.terminal;

import org.jboss.aesh.util.ANSI;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

    }


}

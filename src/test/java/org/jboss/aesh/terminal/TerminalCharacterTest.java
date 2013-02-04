package org.jboss.aesh.terminal;

import junit.framework.TestCase;
import org.jboss.aesh.util.ANSI;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TerminalCharacterTest extends TestCase {

    public TerminalCharacterTest(String name) {
        super(name);
    }

    public void testTerminalCharacterAsString() {
        TerminalCharacter character = new TerminalCharacter('c', CharacterType.BOLD);

        assertEquals(ANSI.getStart()+
                CharacterType.BOLD.getValue()+";"+
                Color.DEFAULT_TEXT.getValue()+";"+
                Color.DEFAULT_BG.getValue()+
                "mc",
                character.getAsString());
    }
}

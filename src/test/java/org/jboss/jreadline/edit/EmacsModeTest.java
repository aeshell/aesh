package org.jboss.jreadline.edit;

import org.jboss.jreadline.JReadlineTestCase;
import org.jboss.jreadline.TestBuffer;
import org.jboss.jreadline.console.Config;
import org.jboss.jreadline.edit.actions.Operation;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class EmacsModeTest extends JReadlineTestCase {

    public EmacsModeTest(String test) {
        super(test);
    }

    public void testSimpleMovementAndEdit() throws Exception {
        TestBuffer b = new TestBuffer("1234");

        KeyOperation deleteNextChar = new KeyOperation(4, Operation.DELETE_NEXT_CHAR);
        KeyOperation deletePrevChar =  new KeyOperation(8, Operation.DELETE_PREV_CHAR);

        b.append(deletePrevChar.getFirstValue())
                .append(TestBuffer.getNewLine()); // enter

        assertEquals("123", b);

        KeyOperation movePrevChar = new KeyOperation(2, Operation.MOVE_PREV_CHAR);

        b = new TestBuffer("1234");
        b.append(movePrevChar.getFirstValue())
                .append(movePrevChar.getFirstValue())
                .append(deleteNextChar.getFirstValue())
                .append("5")
                .append(TestBuffer.getNewLine()); // enter
        assertEquals("1254", b);

        KeyOperation moveBeginning = new KeyOperation(1, Operation.MOVE_BEGINNING);

        b = new TestBuffer("1234");
        b.append(moveBeginning.getFirstValue())
                .append( deleteNextChar.getFirstValue())
                .append(TestBuffer.getNewLine()); // enter

        assertEquals("234", b);

        KeyOperation moveNextChar = new KeyOperation(6, Operation.MOVE_NEXT_CHAR);

        b = new TestBuffer("1234");
        b.append(moveBeginning.getFirstValue())
                .append(deleteNextChar.getFirstValue())
                .append(moveNextChar.getFirstValue())
                .append(moveNextChar.getFirstValue())
                .append("5")
                .append(TestBuffer.getNewLine());

        assertEquals("2354", b);
    }

    public void testWordMovementAndEdit() throws Exception {

        if(Config.isOSPOSIXCompatible()) {
            KeyOperation moveNextWord = new KeyOperation(new int[]{27,102}, Operation.MOVE_NEXT_WORD);
            KeyOperation movePrevWord = new KeyOperation(new int[]{27,98}, Operation.MOVE_PREV_WORD);
            KeyOperation deleteNextWord = new KeyOperation(new int[]{27,100}, Operation.DELETE_NEXT_WORD);
            KeyOperation moveBeginning = new KeyOperation(1, Operation.MOVE_BEGINNING);
            KeyOperation deleteBeginning = new KeyOperation(21, Operation.DELETE_BEGINNING);

            TestBuffer b = new TestBuffer("foo   bar...  Foo-Bar.");
            b.append(movePrevWord.getKeyValues())
                    .append(movePrevWord.getKeyValues())
                    .append(deleteNextWord.getKeyValues())
                    .append(TestBuffer.getNewLine());
            assertEquals("foo   bar...  Foo-.", b);

            b = new TestBuffer("foo   bar...  Foo-Bar.");
            b.append(moveBeginning.getKeyValues())
                    .append(moveNextWord.getKeyValues())
                    .append(moveNextWord.getKeyValues())
                    .append(deleteNextWord.getKeyValues())
                    .append(TestBuffer.getNewLine());
            assertEquals("foo   barFoo-Bar.", b);


            b = new TestBuffer("foo   bar...   Foo-Bar.");
            b.append(moveBeginning.getKeyValues())
                    .append(moveNextWord.getKeyValues())
                    .append(moveNextWord.getKeyValues())
                    .append(deleteBeginning.getKeyValues())
                    .append(TestBuffer.getNewLine());
            assertEquals("...   Foo-Bar.", b);

        }
    }

    public void testArrowMovement() throws Exception {

        KeyOperation deletePrevChar =  new KeyOperation(8, Operation.DELETE_PREV_CHAR);
        KeyOperation moveBeginning = new KeyOperation(1, Operation.MOVE_BEGINNING);
        KeyOperation movePrevChar;
        KeyOperation moveNextChar;
        if(Config.isOSPOSIXCompatible()) {
            moveNextChar = new KeyOperation(new int[]{27,91,67}, Operation.MOVE_NEXT_CHAR);
            movePrevChar = new KeyOperation(new int[]{27,91,68}, Operation.MOVE_PREV_CHAR);
        }
        else {
            moveNextChar = new KeyOperation(new int[]{224,77}, Operation.MOVE_NEXT_CHAR);
            movePrevChar = new KeyOperation(new int[]{224,75}, Operation.MOVE_PREV_CHAR);

        }

        TestBuffer b = new TestBuffer("foo   bar...  Foo-Bar.");
        b.append(movePrevChar.getKeyValues())
                .append(movePrevChar.getKeyValues())
                .append(movePrevChar.getKeyValues())
                .append(deletePrevChar.getKeyValues())
                .append(TestBuffer.getNewLine());
        assertEquals("foo   bar...  Foo-ar.", b);

        b = new TestBuffer("foo   bar...  Foo-Bar.");
        b.append(moveBeginning.getKeyValues())
                .append(moveNextChar.getKeyValues())
                .append(moveNextChar.getKeyValues())
                .append(deletePrevChar.getKeyValues())
                .append(TestBuffer.getNewLine());
        assertEquals("fo   bar...  Foo-Bar.", b);
    }

}

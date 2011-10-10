package org.jboss.jreadline.edit;

import org.jboss.jreadline.JReadlineTestCase;
import org.jboss.jreadline.TestBuffer;
import org.jboss.jreadline.console.Config;
import org.jboss.jreadline.edit.actions.Operation;

import java.util.List;

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
        KeyOperation deletePrevChar =  new KeyOperation(127, Operation.DELETE_PREV_CHAR);

        b.append(deletePrevChar.getFirstValue())
                .append(TestBuffer.ENTER); // enter

        assertEquals("123", b);

        KeyOperation movePrevChar = new KeyOperation(2, Operation.MOVE_PREV_CHAR);

        b = new TestBuffer("1234");
        b.append(movePrevChar.getFirstValue())
                .append(movePrevChar.getFirstValue())
                .append(deleteNextChar.getFirstValue())
                .append("5")
                .append(TestBuffer.ENTER); // enter
        assertEquals("1254", b);

        KeyOperation moveBeginning = new KeyOperation(1, Operation.MOVE_BEGINNING);

        b = new TestBuffer("1234");
        b.append(moveBeginning.getFirstValue())
                .append( deleteNextChar.getFirstValue())
                .append(TestBuffer.ENTER); // enter

        assertEquals("234", b);

        KeyOperation moveNextChar = new KeyOperation(6, Operation.MOVE_NEXT_CHAR);

        b = new TestBuffer("1234");
        b.append(moveBeginning.getFirstValue())
                .append(deleteNextChar.getFirstValue())
                .append(moveNextChar.getFirstValue())
                .append(moveNextChar.getFirstValue())
                .append("5")
                .append(TestBuffer.ENTER);

        assertEquals("2354", b);
    }

    public void testWordMovementAndEdit() throws Exception {

        if(Config.isOSPOSIXCompatible()) {
            KeyOperation moveNextWord = new KeyOperation(new int[]{27,102}, Operation.MOVE_NEXT_WORD);
            KeyOperation movePrevWord = new KeyOperation(new int[]{27,98}, Operation.MOVE_PREV_WORD);
            KeyOperation deleteNextWord = new KeyOperation(new int[]{27,100}, Operation.DELETE_NEXT_WORD);

            /*
            TestBuffer b = new TestBuffer("foo   bar...  Foo-Bar.");
            b.append(movePrevWord
                    .append("B")
                    .append("d").append("b") // db
                    .append(TestBuffer.ENTER);
            assertEqualsViMode("foo   barFoo-Bar.", b);

            b = new TestBuffer("foo   bar...  Foo-Bar.");
            b.append(TestBuffer.ESCAPE)
                    .append("0")
                    .append("W")
                    .append("W")
                    .append("d").append("W")
                    .append(TestBuffer.ENTER);
            assertEqualsViMode("foo   bar...  ", b);

            b = new TestBuffer("foo   bar...   Foo-Bar.");
            b.append(TestBuffer.ESCAPE)
                    .append("0")
                    .append("w")
                    .append("w")
                    .append("d").append("W")
                    .append(TestBuffer.ENTER);
            assertEqualsViMode("foo   barFoo-Bar.", b);

            b = new TestBuffer("foo   bar...   Foo-Bar.");
            b.append(TestBuffer.ESCAPE)
                    .append("B")
                    .append("d").append("B")
                    .append(TestBuffer.ENTER);
            assertEqualsViMode("foo   Foo-Bar.", b);

            b = new TestBuffer("foo   bar...   Foo-Bar.");
            b.append(TestBuffer.ESCAPE)
                    .append("0")
                    .append("w").append("w")
                    .append("i")
                    .append("-bar")
                    .append(TestBuffer.ESCAPE)
                    .append("B")
                    .append("d").append("w") //dw
                    .append("x") // x
                    .append("d").append("B") //dB
                    .append(TestBuffer.ENTER);
            assertEqualsViMode("bar...   Foo-Bar.", b);
            */
        }
    }

}

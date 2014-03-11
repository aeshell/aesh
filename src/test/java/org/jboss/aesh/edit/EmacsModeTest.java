/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit;

import org.jboss.aesh.AeshTestCase;
import org.jboss.aesh.TestBuffer;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;
import org.junit.Ignore;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Ignore //todo: rewrite to use console
public class EmacsModeTest extends AeshTestCase {

    public EmacsModeTest(String test) {
        super(test);
    }

    public void testSimpleMovementAndEdit() throws Exception {
        TestBuffer b = new TestBuffer("1234");

        KeyOperation deleteNextChar = new KeyOperation(Key.CTRL_D, Operation.DELETE_NEXT_CHAR);
        KeyOperation deletePrevChar =  new KeyOperation(Key.CTRL_H, Operation.DELETE_PREV_CHAR);

        b.append(deletePrevChar.getFirstValue())
                .append(TestBuffer.getNewLine()); // enter

        assertEquals("123", b);

        KeyOperation movePrevChar = new KeyOperation(Key.CTRL_B, Operation.MOVE_PREV_CHAR);

        b = new TestBuffer("1234");
        b.append(movePrevChar.getFirstValue())
                .append(movePrevChar.getFirstValue())
                .append(deleteNextChar.getFirstValue())
                .append("5")
                .append(TestBuffer.getNewLine()); // enter
        assertEquals("1254", b);

        KeyOperation moveBeginning = new KeyOperation(Key.CTRL_A, Operation.MOVE_BEGINNING);

        b = new TestBuffer("1234");
        b.append(moveBeginning.getFirstValue())
                .append( deleteNextChar.getFirstValue())
                .append(TestBuffer.getNewLine()); // enter

        assertEquals("234", b);

        KeyOperation moveNextChar = new KeyOperation(Key.CTRL_F, Operation.MOVE_NEXT_CHAR);

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
            KeyOperation moveNextWord = new KeyOperation(Key.META_f, Operation.MOVE_NEXT_WORD);
            KeyOperation movePrevWord = new KeyOperation(Key.META_b, Operation.MOVE_PREV_WORD);
            KeyOperation deleteNextWord = new KeyOperation(Key.META_d, Operation.DELETE_NEXT_WORD);
            KeyOperation moveBeginning = new KeyOperation(Key.CTRL_A, Operation.MOVE_BEGINNING);
            KeyOperation deleteBeginning = new KeyOperation(Key.CTRL_U, Operation.DELETE_BEGINNING);

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

        KeyOperation deletePrevChar =  new KeyOperation(Key.CTRL_H, Operation.DELETE_PREV_CHAR);
        KeyOperation moveBeginning = new KeyOperation(Key.CTRL_A, Operation.MOVE_BEGINNING);
        KeyOperation movePrevChar;
        KeyOperation moveNextChar;
        moveNextChar = new KeyOperation(Key.RIGHT, Operation.MOVE_NEXT_CHAR);
        movePrevChar = new KeyOperation(Key.LEFT, Operation.MOVE_PREV_CHAR);

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

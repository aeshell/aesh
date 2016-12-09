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
package org.jboss.aesh.terminal;

import org.aesh.terminal.formatting.CharacterType;
import org.aesh.terminal.formatting.Color;
import org.aesh.terminal.formatting.TerminalCharacter;
import org.aesh.terminal.formatting.TerminalColor;
import org.aesh.terminal.formatting.TerminalTextStyle;
import org.aesh.util.ANSI;
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

        assertEquals(ANSI.START+
                CharacterType.BOLD.getValue()+";"+
                3+ Color.DEFAULT.getValue()+";"+
                4+Color.DEFAULT.getValue()+
                "mc",
                character.toString());
    }

    @Test
    public void testPrevCharacterAsString() {
        TerminalCharacter c1 = new TerminalCharacter('c', new TerminalTextStyle(CharacterType.BOLD));

        assertEquals(ANSI.START +
                CharacterType.BOLD.getValue() + ";" +
                3+Color.DEFAULT.getValue() + ";" +
                4+Color.DEFAULT.getValue() +
                "mc",
                c1.toString());

        TerminalCharacter c2 = new TerminalCharacter('f', new TerminalColor(Color.DEFAULT, Color.BLUE),
                new TerminalTextStyle(CharacterType.CROSSED_OUT));

         assertEquals(ANSI.START +
                 BOLD_OFF + ";" +
                CharacterType.CROSSED_OUT.getValue() + ";" +
                4+Color.BLUE.getValue() +
                "mf",
                c2.toString(c1));

        TerminalCharacter c3 = new TerminalCharacter('f', new TerminalColor(Color.RED, Color.BLUE));
        TerminalCharacter c4 = new TerminalCharacter('f', new TerminalColor(Color.RED, Color.BLUE));

         assertEquals("f", c4.toString(c3));

        c4 = new TerminalCharacter('g', new TerminalColor(Color.RED, Color.BLUE), new TerminalTextStyle(CharacterType.BOLD));

         assertEquals(ANSI.START + CharacterType.BOLD.getValue() + "mg", c4.toString(c3));

        c3 = new TerminalCharacter('f', new TerminalColor(Color.RED, Color.BLUE), new TerminalTextStyle(CharacterType.BOLD));
        c4 = new TerminalCharacter('g');

        assertEquals(ANSI.START + "22;39;49mg", c4.toString(c3));

        c4 = new TerminalCharacter('g', new TerminalTextStyle(CharacterType.INVERT));

        assertEquals(ANSI.START + "22;7;39;49mg", c4.toString(c3));
    }

    @Test
    public void testEqualsIgnoreCharacter() {
        TerminalCharacter c1 = new TerminalCharacter('a', new TerminalColor(Color.RED, Color.BLUE));
        TerminalCharacter c2 = new TerminalCharacter('b', new TerminalColor(Color.RED, Color.BLUE));

        assertTrue(c1.equalsIgnoreCharacter(c2));

    }


}

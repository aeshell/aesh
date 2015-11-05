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
package org.jboss.aesh.readline.editing;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.readline.actions.BackwardChar;
import org.jboss.aesh.readline.actions.BeginningOfLine;
import org.jboss.aesh.readline.actions.DeletePrevChar;
import org.jboss.aesh.readline.actions.EndOfLine;
import org.jboss.aesh.readline.actions.ForwardChar;
import org.jboss.aesh.readline.actions.NextHistory;
import org.jboss.aesh.readline.actions.NoAction;
import org.jboss.aesh.readline.actions.PrevHistory;
import org.jboss.aesh.terminal.Key;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class KeyMapperTest {

    @Test
    public void testQuoteMapKeys() {
        if(Config.isOSPOSIXCompatible()) {
            EditModeBuilder builder = new EditModeBuilder();
            InputrcParser.parseLine("\"\\M-a\":   meta", builder);
            assertEquals(builder.create().parse(Key.META_a).name(), new NoAction().name());

            InputrcParser.parseLine("\"\\M-[D\": backward-char", builder);
            assertEquals(builder.create().parse(Key.LEFT).name(), new BackwardChar().name());
            InputrcParser.parseLine("\"\\M-[C\": forward-char", builder);
            assertEquals(builder.create().parse(Key.RIGHT).name(), new ForwardChar().name());
            InputrcParser.parseLine("\"\\M-[A\": previous-history", builder);
            assertEquals(builder.create().parse(Key.UP).name(), new PrevHistory().name());
            InputrcParser.parseLine("\"\\M-[B\": next-history", builder);
            assertEquals(builder.create().parse(Key.DOWN).name(), new NextHistory().name());

            InputrcParser.parseLine("\"\\M-\\C-D\": backward-char", builder);
            assertEquals(builder.create().parse(Key.META_CTRL_D).name(), new BackwardChar().name());
            InputrcParser.parseLine("\"\\C-\\M-d\": backward-char", builder);
            assertEquals(builder.create().parse(Key.META_CTRL_D).name(), new BackwardChar().name());
            InputrcParser.parseLine("\"\\C-a\": end-of-line", builder);
            assertEquals(builder.create().parse(Key.CTRL_A).name(), new EndOfLine().name());

        }
    }

    @Test
    public void testMapKeys() {
        if(Config.isOSPOSIXCompatible()) {


            EditModeBuilder builder = new EditModeBuilder();

            builder.addAction(InputrcParser.mapKeys("M-a"), "meta");
            assertEquals(builder.create().parse(Key.META_a).name(), new NoAction().name());

            builder = new EditModeBuilder();
            builder.addAction(InputrcParser.mapKeys("M-[D"), "backward-char");
            assertEquals(builder.create().parse(Key.LEFT).name(), new BackwardChar().name());

            builder = new EditModeBuilder();
            builder.addAction(InputrcParser.mapKeys("M-[C"), "forward-char");
            assertEquals(builder.create().parse(Key.RIGHT).name(), new ForwardChar().name());

            builder = new EditModeBuilder();
            builder.addAction(InputrcParser.mapKeys("M-[A"), "previous-history");
            assertEquals(builder.create().parse(Key.UP).name(), new PrevHistory().name());

            builder = new EditModeBuilder();
            builder.addAction(InputrcParser.mapKeys("M-[B"), "next-history");
            assertEquals(builder.create().parse(Key.DOWN).name(), new NextHistory().name());

            builder = new EditModeBuilder();
            builder.addAction(InputrcParser.mapKeys("M-C-d"), "backward-char");
            assertEquals(builder.create().parse(Key.META_CTRL_D).name(), new BackwardChar().name());

            builder = new EditModeBuilder();
            builder.addAction(InputrcParser.mapKeys("C-M-D"), "forward-char");
            assertEquals(builder.create().parse(Key.META_CTRL_D).name(), new ForwardChar().name());

            builder = new EditModeBuilder();
            builder.addAction(InputrcParser.mapKeys("C-a"), "beginning-of-line");
            assertEquals(builder.create().parse(Key.CTRL_A).name(), new BeginningOfLine().name());

            builder = new EditModeBuilder();
            builder.addAction(InputrcParser.mapKeys("C-?"), "backward-delete-char");
            assertEquals(builder.create().parse(Key.BACKSPACE).name(), new DeletePrevChar().name());
        }
    }
}

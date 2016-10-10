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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.jboss.aesh.console.AeshInputProcessorBuilder;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.ConsoleBuffer;
import org.jboss.aesh.console.InputProcessor;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.Shell;
import org.jboss.aesh.console.TestShell;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.terminal.Key;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class EmacsModeTest {

    @Test
    public void testSimpleMovementAndEdit() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .readInputrc(false)
                .ansi(true)
                .persistHistory(false)
                .enableAlias(false)
                .mode(EditMode.Mode.EMACS)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder()
                .shell(shell)
                .prompt(new Prompt("aesh"))
                .editMode(settings.editMode())
                .create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .settings(settings)
                .create();


        consoleBuffer.writeString("1234");

        //remove the last char
        inputProcessor.parseOperation(Key.CTRL_D);
        assertEquals("1234", consoleBuffer.getBuffer().getLineNoMask());

        //move on char to the left
        inputProcessor.parseOperation(Key.LEFT);
        inputProcessor.parseOperation(Key.CTRL_D);
        inputProcessor.parseOperation(Key.FIVE);

        assertEquals("1235", consoleBuffer.getBuffer().getLineNoMask());

        inputProcessor.parseOperation(Key.CTRL_A);
        inputProcessor.parseOperation(Key.CTRL_D);
        assertEquals("235", consoleBuffer.getBuffer().getLineNoMask());
        inputProcessor.parseOperation(Key.CTRL_F);
        inputProcessor.parseOperation(Key.CTRL_F);
        inputProcessor.parseOperation(Key.SIX);

        assertEquals("2365", consoleBuffer.getBuffer().getLineNoMask());

    }

    @Test
    public void testWordMovementAndEdit() throws Exception {

        if(Config.isOSPOSIXCompatible()) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .persistHistory(false)
                .readInputrc(false)
                .ansi(true)
                .enableAlias(false)
                .mode(EditMode.Mode.EMACS)
                .create();

        Shell shell = new TestShell(new PrintStream(byteArrayOutputStream), System.err);
        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder()
                .shell(shell)
                .prompt(new Prompt("aesh"))
                .editMode(settings.editMode())
                .create();

        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .settings(settings)
                .create();


            consoleBuffer.writeString("foo  bar...  Foo-Bar.");

            inputProcessor.parseOperation(Key.META_b);
            inputProcessor.parseOperation(Key.META_d);

            String output = inputProcessor.parseOperation(Key.ENTER);

            assertEquals("foo  bar...  Foo-.", output);

            consoleBuffer.writeString("foo  bar...  Foo-Bar.");
            inputProcessor.parseOperation(Key.CTRL_A);
            inputProcessor.parseOperation(Key.META_f);
            inputProcessor.parseOperation(Key.META_f);
            inputProcessor.parseOperation(Key.META_d);

            output = inputProcessor.parseOperation(Key.ENTER);

            assertEquals("foo  bar-Bar.", output);

            consoleBuffer.writeString("foo  bar...  Foo-Bar.");
            inputProcessor.parseOperation(Key.CTRL_A);
            inputProcessor.parseOperation(Key.META_f);
            inputProcessor.parseOperation(Key.META_f);
            inputProcessor.parseOperation(Key.CTRL_U);

            output = inputProcessor.parseOperation(Key.ENTER);

            assertEquals("...  Foo-Bar.", output);
        }
    }
}

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
package org.jboss.aesh.console.export;

import org.aesh.readline.editing.EditMode;
import org.aesh.terminal.Key;
import org.aesh.util.Config;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.readline.ReadlineConsole;
import org.jboss.aesh.tty.TestConnection;
import org.junit.Test;

import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ExportCommandTest {

    private final Key completeChar =  Key.CTRL_I;
    private final Key backSpace =  Key.BACKSPACE;

    @Test
    public void testExportCompletionAndCommand() throws IOException, InterruptedException {

        TestConnection connection = new TestConnection();

        CommandRegistry registry = new AeshCommandRegistryBuilder().create();

        Settings settings = new SettingsBuilder()
                .connection(connection)
                .commandRegistry(registry)
                .setPersistExport(false)
                .mode(EditMode.Mode.EMACS)
                .readInputrc(false)
                .logging(true)
                .create();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("exp");
        connection.read(completeChar.getFirstValue());
        //outputStream.flush();
        Thread.sleep(100);
        //assertEquals("export ", ((AeshConsoleImpl) console).getBuffer());


        connection.read("FOO=/tmp"+ Config.getLineSeparator());
        connection.read("export"+Config.getLineSeparator());
        //outputStream.flush();
        Thread.sleep(100);
        //assertTrue(byteArrayOutputStream.toString().contains("FOO=/tmp"));

        connection.read("export BAR=$F");
        connection.read(completeChar.getFirstValue());
        //outputStream.flush();
        Thread.sleep(100);
        //assertEquals("export BAR=$FOO ", ((AeshConsoleImpl) console).getBuffer());

        connection.read(backSpace.getFirstValue());
        connection.read(":/opt"+Config.getLineSeparator());
        //outputStream.flush();
        Thread.sleep(100);
        connection.read("export"+Config.getLineSeparator());
        //outputStream.flush();
        Thread.sleep(400);
        //assertTrue(byteArrayOutputStream.toString().contains("BAR=/tmp:/opt"));

        connection.read("$");
        connection.read(completeChar.getFirstValue());
        //outputStream.flush();
        Thread.sleep(400);
        //assertTrue(byteArrayOutputStream.toString().contains("$FOO"));
        //assertTrue(byteArrayOutputStream.toString().contains("$BAR"));

        connection.read("B");
        connection.read(completeChar.getFirstValue());
        //outputStream.flush();
        Thread.sleep(400);
        //assertEquals("$BAR ", ((AeshConsoleImpl) console).getBuffer());

        connection.read(Config.getLineSeparator());
        //outputStream.flush();
        Thread.sleep(400);

        //assertTrue(byteArrayOutputStream.toString().contains("/tmp:/opt"));

        console.stop();
    }

}

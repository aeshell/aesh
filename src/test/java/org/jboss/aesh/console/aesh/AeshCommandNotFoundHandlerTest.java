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
package org.jboss.aesh.console.aesh;

import org.jboss.aesh.console.Shell;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.CommandNotFoundHandler;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.readline.ReadlineConsole;
import org.jboss.aesh.tty.TestConnection;
import org.aesh.util.Config;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandNotFoundHandlerTest {

    @Test
    public void testCommandNotFoundHandler() throws InterruptedException, IOException {
        TestConnection connection = new TestConnection();

       CommandRegistry registry = new AeshCommandRegistryBuilder()
                .create();

         Settings settings = new SettingsBuilder()
                 .commandRegistry(registry)
                 .connection(connection)
                 .commandNotFoundHandler(new HandlerCommandNotFound())
                .logging(true)
                .create();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("foo -l 12 -h 20"+ Config.getLineSeparator());
        Thread.sleep(50);
        assertTrue( connection.getOutputBuffer().endsWith("DUUUUDE, where is your command?"+Config.getLineSeparator()));

        console.stop();
    }

    public static class HandlerCommandNotFound implements CommandNotFoundHandler {
        @Override
        public void handleCommandNotFound(String line, Shell shell) {
            shell.writeln("DUUUUDE, where is your command?");
        }
    }
}

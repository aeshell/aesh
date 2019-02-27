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
package org.aesh.command.man;

import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.ManProvider;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshManProviderTest {

    @Test
    public void testManProvider() throws IOException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .commandRegistry(registry)
                        .connection(connection)
                        .manProvider(new TestManProvider())
                        .logging(true)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        ManProvider manProvider = settings.manProvider();
        assertNotNull(manProvider);

        assertNotNull(manProvider.getManualDocument("foo"));

        console.stop();
    }

    class TestManProvider implements ManProvider {

        @Override
        public InputStream getManualDocument(String commandName) {
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    return 0;
                }
            };
        }
    }
}

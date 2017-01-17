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
package org.aesh.console.aesh;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.console.settings.Settings;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.readline.ReadlineConsole;

import org.aesh.readline.completion.CompleteOperation;
import org.aesh.terminal.Key;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandRegistryTest {

    private static final String WRITTEN = "hgjfiehk";
    private final Key completeChar =  Key.CTRL_I;

    @Test
    public void testExceptionThrownFromCommandRegistryShouldNotCrashAesh() throws Exception {

        TestConnection connection = new TestConnection();

        CommandRegistry registry = new CommandRegistry() {

            @Override
            public CommandContainer getCommand(String name, String line) throws CommandNotFoundException {
                throw new IllegalStateException("Should not crash Aesh");
            }

            @Override
            public CommandContainer getCommandByAlias(String alias) throws CommandNotFoundException {
                throw new IllegalStateException("Should not crash Aesh");
            }

            @Override
            public void completeCommandName(CompleteOperation completeOperation) {
            }

            @Override
            public Set<String> getAllCommandNames() {
                throw new IllegalStateException("Should not crash Aesh");
            }

            @Override public void removeCommand(String name) {

            }

            @Override
            public List<CommandLineParser<?>> getChildCommandParsers(String parent) throws CommandNotFoundException {
                return Collections.emptyList();
            }
        };

        Settings settings = SettingsBuilder.builder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read(completeChar.getFirstValue());
        connection.read(WRITTEN);
        Thread.sleep(50);
        connection.assertBuffer(WRITTEN);

        console.stop();
    }

    @Test
    public void testCommandRegistryReturningNullShouldNotCrashAesh() throws Exception {

        TestConnection connection = new TestConnection();

        CommandRegistry registry = new CommandRegistry() {

            @Override
            public CommandContainer getCommand(String name, String line) throws CommandNotFoundException {
                return null;
            }

            @Override
            public CommandContainer getCommandByAlias(String alias) throws CommandNotFoundException {
                return null;
            }

            @Override
            public void completeCommandName(CompleteOperation completeOperation) {
            }

            @Override
            public Set<String> getAllCommandNames() {
                return null;
            }

            @Override public void removeCommand(String name) {

            }

            @Override
            public List<CommandLineParser<?>> getChildCommandParsers(String parent) throws CommandNotFoundException {
                return Collections.emptyList();
            }
        };

        Settings settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .setPersistExport(false)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read(completeChar.getFirstValue());
        connection.read(WRITTEN);
        Thread.sleep(50);
        connection.assertBuffer(WRITTEN);

        console.stop();
    }

    @Test
    public void testCommandRegistryReturningNormalValues() throws Exception {

        TestConnection connection = new TestConnection();

       CommandRegistry registry = new CommandRegistry() {

            @Override
            public CommandContainer getCommand(String name, String line) throws CommandNotFoundException {
                return null;
            }

            @Override
            public CommandContainer getCommandByAlias(String alias) throws CommandNotFoundException {
                return null;
            }

            @Override
            public void completeCommandName(CompleteOperation completeOperation) {
            }

            @Override
            public Set<String> getAllCommandNames() {
                return new HashSet<>();
            }

            @Override public void removeCommand(String name) {

            }

            @Override
            public List<CommandLineParser<?>> getChildCommandParsers(String parent) throws CommandNotFoundException {
                return Collections.emptyList();
            }
        };

         Settings settings = SettingsBuilder.builder()
                 .commandRegistry(registry)
                 .connection(connection)
                 .logging(true)
                 .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read(completeChar.getFirstValue());
        connection.read(WRITTEN);
        Thread.sleep(50);
        connection.assertBuffer(WRITTEN);

        console.stop();
    }

}

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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jboss.aesh.cl.parser.CommandLineParser;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.AeshConsoleImpl;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.terminal.Key;
import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandRegistryTest {

    private static final String WRITTEN = "hgjfiehk";
    private final Key completeChar =  Key.CTRL_I;

    @Test
    public void testExceptionThrownFromCommandRegistryShouldNotCrashAesh() throws Exception {

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder().inputStream(pipedInputStream)
            .outputStream(new PrintStream(byteArrayOutputStream)).logging(true).create();

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

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder().settings(settings).commandRegistry(registry)
            .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();

        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();
        Thread.sleep(80);
        outputStream.write(WRITTEN.getBytes());
        outputStream.flush();
        Thread.sleep(80);

        assertEquals(WRITTEN, ((AeshConsoleImpl) aeshConsole).getBuffer().trim());

        aeshConsole.stop();
    }

    @Test
    public void testCommandRegistryReturningNullShouldNotCrashAesh() throws Exception {

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .setPersistExport(false)
                .logging(true)
                .create();

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

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();

        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();
        Thread.sleep(100);
        outputStream.write(WRITTEN.getBytes());
        outputStream.flush();
        Thread.sleep(100);

        assertEquals(WRITTEN, ((AeshConsoleImpl) aeshConsole).getBuffer().trim());

        aeshConsole.stop();
    }

    @Test
    public void testCommandRegistryReturningNormalValues() throws Exception {

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder().inputStream(pipedInputStream)
            .outputStream(new PrintStream(byteArrayOutputStream)).logging(true).create();

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

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder().settings(settings).commandRegistry(registry)
            .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();

        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();
        outputStream.write(WRITTEN.getBytes());
        outputStream.flush();
        Thread.sleep(100);

        assertEquals(WRITTEN, ((AeshConsoleImpl) aeshConsole).getBuffer().trim());

        aeshConsole.stop();
    }

}

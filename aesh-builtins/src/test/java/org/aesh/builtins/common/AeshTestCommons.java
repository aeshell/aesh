/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package org.aesh.builtins.common;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.aesh.command.Command;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.console.AeshContext;
import org.aesh.console.ReadlineConsole;
import org.aesh.terminal.utils.Config;
import org.aesh.tty.TestConnection;

/**
 * Base test class for built-in command tests.
 * Provides a TestConnection-based console with deterministic synchronization.
 */
public class AeshTestCommons {

    private ReadlineConsole console;
    private CommandRegistry registry;
    private TestConnection connection;
    private CountDownLatch commandLatch;

    private static final FileAttribute<?> FILE_ATTRIBUTE =
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx"));

    protected String getStream() {
        return connection.getOutputBuffer();
    }

    protected TestConnection connection() {
        return connection;
    }

    @SafeVarargs
    protected final void prepare(Class<? extends Command>... commands) throws IOException, CommandRegistryException {
        connection = new TestConnection();

        registry = AeshCommandRegistryBuilder.builder()
                .commands(commands)
                .create();

        commandLatch = new CountDownLatch(1);

        Settings settings = SettingsBuilder.builder()
                .connection(connection)
                .commandRegistry(registry)
                .commandExecutionListener((line, result, durationMs) -> commandLatch.countDown())
                .build();

        console = new ReadlineConsole(settings);
        console.start();
    }

    protected void finish() {
        console.stop();
    }

    protected void pushToOutput(String literalCommand) throws IOException {
        commandLatch = new CountDownLatch(1);
        connection.read(literalCommand + Config.getLineSeparator());
        try {
            commandLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void output(String literalCommand) {
        connection.write(literalCommand);
    }

    protected AeshContext getAeshContext() {
        return console.context();
    }

    protected Path createTempDirectory() throws IOException {
        if (Config.isOSPOSIXCompatible()) {
            return Files.createTempDirectory("temp", FILE_ATTRIBUTE);
        }
        return Files.createTempDirectory("temp");
    }

    protected void deleteRecursiveTempDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}

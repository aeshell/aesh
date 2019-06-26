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
package org.aesh.command.operator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.aesh.terminal.utils.Config;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author jdenise@redhat.com
 */
public class AeshCommandConditionalOperatorTest {

    private static int counter = 0;

    @Test
    public void testEnd() throws IOException, InterruptedException, CommandRegistryException {
        TestConnection connection = new TestConnection();

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(SuccessCommand.class)
                .command(FailureCommand.class)
                .create();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .commandRegistry(registry)
                        .enableOperatorParser(true)
                        .connection(connection)
                        .setPersistExport(false)
                        .logging(true)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("success || success || failure" + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("success0");
        reset(connection);

        connection.read("failure || success || failure" + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("failure0success1");
        reset(connection);

        connection.read("failure || failure || success" + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("failure0failure1success2");
        reset(connection);

        connection.read("failure || failure || failure" + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("failure0failure1failure2");
        reset(connection);

        connection.read("success && success && success" + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("success0success1success2");
        reset(connection);

        connection.read("success && failure && success" + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("success0failure1");
        reset(connection);

        connection.read("failure && success && success" + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("failure0");
        reset(connection);

        connection.read("success && success && success || failure || success" + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("success0success1success2");
        reset(connection);

        connection.read("success && success && failure || failure || success" + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("success0success1failure2failure3success4");
        reset(connection);

        connection.read("failure || failure && success || failure || success" + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("failure0failure1failure2success3");
        reset(connection);

        connection.read("failure || failure && success && success && failure && success || failure || success"
                + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("failure0failure1failure2success3");
        reset(connection);

        connection.read("success && failure && success && success && failure && success || failure || success"
                + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("success0failure1failure2success3");
        reset(connection);

        connection.read("success || failure && success && success && failure && success || failure || success"
                + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("success0success1success2failure3failure4success5");
        reset(connection);

        File tmpDir = File.createTempFile("aeshconditionnaltest", null).getParentFile();

        File outFile = new File(tmpDir, "conditional_out.txt");
        connection.read("success || failure && success && success && failure && success || failure || success > "
                + outFile.getAbsolutePath() + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("success0success1success2failure3failure4");
        List<String> output = Files.readAllLines(outFile.toPath());
        assertEquals("success5", output.get(0));
        Files.delete(outFile.toPath());
        reset(connection);

        File outFile2 = new File(tmpDir, "conditional_out2.txt");
        File inFile = new File(tmpDir, "conditional_in.txt");
        connection.read("success < " + inFile.getAbsolutePath() + " && success < "
                + inFile.getAbsolutePath() + " || success < " + inFile.getAbsolutePath()
                + " && success > " + outFile2.getAbsolutePath() + Config.getLineSeparator());
        Thread.sleep(100);
        connection.assertBufferEndsWith("success0success1");
        List<String> output2 = Files.readAllLines(outFile2.toPath());
        assertEquals("success2", output2.get(0));
        Files.delete(outFile2.toPath());
        reset(connection);


        console.stop();
    }

    private static void reset(TestConnection connection) {
        connection.clearOutputBuffer();
        counter = 0;
    }

    @CommandDefinition(name = "success", description = "")
    private static class SuccessCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.print("success" + counter);
            counter++;
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "failure", description = "")
    private static class FailureCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.print("failure" + counter);
            counter++;
            return CommandResult.FAILURE;
        }
    }

}

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

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.validator.CommandValidator;
import org.jboss.aesh.cl.validator.CommandValidatorException;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import org.jboss.aesh.console.command.CommandException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandOverrideRequiredTest {

    @Test
    public void testOverrideRequired() throws IOException, InterruptedException {
         PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .logging(true)
                .create();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(FooCommand.class)
                .create();

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();

        outputStream.write(("foo -h"+ Config.getLineSeparator()).getBytes());
        outputStream.flush();
        Thread.sleep(200);
        assertTrue(byteArrayOutputStream.toString().contains("OVERRIDDEN"));

        aeshConsole.stop();

    }

    @CommandDefinition(name = "foo", description = "", validator = FooCommandValidator.class)
    public class FooCommand implements Command {

        @Option(required = true)
        private boolean required;

        @Option(shortName = 'h', overrideRequired = true, hasValue = false)
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            if(help)
                commandInvocation.getShell().out().println("OVERRIDDEN");
            return CommandResult.SUCCESS;
        }
    }

    public class FooCommandValidator implements CommandValidator {
        @Override
        public void validate(Command command) throws CommandValidatorException {
            fail("Should never get here!");
        }
    }
}

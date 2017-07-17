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

import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.CommandValidator;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.console.settings.Settings;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.command.CommandDefinition;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.CommandResult;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.aesh.utils.Config;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandOverrideRequiredTest {

    @Test
    public void testOverrideRequired() throws IOException, InterruptedException, CommandLineParserException {
        TestConnection connection = new TestConnection();

       CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(FooCommand.class)
                .create();

        Settings settings = SettingsBuilder.builder()
                .commandRegistry(registry)
                .connection(connection)
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.start();

        connection.read("foo -h"+ Config.getLineSeparator());
        //outputStream.flush();
        Thread.sleep(100);
        connection.assertBufferEndsWith("OVERRIDDEN"+Config.getLineSeparator());

        console.stop();

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
                commandInvocation.println("OVERRIDDEN");
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

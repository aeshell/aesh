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
import org.jboss.aesh.cl.converter.Converter;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.converter.ConverterInvocation;
import org.jboss.aesh.console.command.converter.ConverterInvocationProvider;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshConverterInvocationProviderTest {

    @Test
    public void testConverterInvocationProvider() throws IOException, InterruptedException {

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .logging(true)
                .create();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(new ConCommand())
                .create();

        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .converterInvocationProvider(new FooConverterProvider())
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();

        outputStream.write(("convert --foo bar"+ Config.getLineSeparator()).getBytes());
        outputStream.flush();

        Thread.sleep(100);
        assertTrue(byteArrayOutputStream.toString().contains("FOOO"));
        aeshConsole.stop();
        outputStream.close();
        pipedInputStream.close();
        byteArrayOutputStream.close();
    }

@CommandDefinition(name = "convert", description = "")
public static class ConCommand implements Command {

    @Option(name = "foo", converter = FooConverter.class)
    private String foo;

    public ConCommand() {
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
        commandInvocation.getShell().out().println(foo);
        commandInvocation.getShell().out().flush();
        return CommandResult.SUCCESS;
    }
}

public static class FooConverterInvocation implements ConverterInvocation {

    private final String input;
    private final AeshContext aeshContext;

    public FooConverterInvocation(String input, AeshContext aeshContext) {
        this.input = input;
        this.aeshContext = aeshContext;
    }

    public String getFoo() {
        return "FOOO";
    }

    @Override
    public String getInput() {
        return input;
    }

    @Override
    public AeshContext getAeshContext() {
        return aeshContext;
    }
}

    public static class FooConverter implements Converter<String, FooConverterInvocation> {

    public FooConverter() {
    }

    @Override
    public String convert(FooConverterInvocation converterInvocation) throws OptionValidatorException {
        assertTrue(converterInvocation.getFoo().equals("FOOO"));
        return converterInvocation.getFoo();
    }
}

public static class FooConverterProvider implements ConverterInvocationProvider<FooConverterInvocation> {
    @Override
    public FooConverterInvocation enhanceConverterInvocation(ConverterInvocation converterInvocation) {
        return new FooConverterInvocation(converterInvocation.getInput(), converterInvocation.getAeshContext());
    }
}

}

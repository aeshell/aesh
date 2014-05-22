/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.aesh;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.converter.Converter;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
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

    private String input;

    public FooConverterInvocation(String input) {
        this.input = input;
    }

    public String getFoo() {
        return "FOOO";
    }

    @Override
    public String getInput() {
        return input;
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
        return new FooConverterInvocation(converterInvocation.getInput());
    }
}

}

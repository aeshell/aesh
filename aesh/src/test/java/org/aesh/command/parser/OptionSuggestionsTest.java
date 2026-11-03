/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.aesh.command.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionVisibility;
import org.junit.Test;

/**
 * An unknown long option is reported with the long options of the command that are close to it ("did you mean").
 */
public class OptionSuggestionsTest {

    @Test
    public void testMistypedOption() throws Exception {
        assertEquals("The option --exper is unknown. Did you mean: --expert?", error("--exper", "guard"));
        assertEquals("The option --timout is unknown. Did you mean: --timeout?", error("--timout", "5"));
    }

    @Test
    public void testOptionWithValue() throws Exception {
        assertEquals("The option --exper=guard is unknown. Did you mean: --expert?", error("--exper=guard"));
    }

    @Test
    public void testAliasAndSeveralSuggestions() throws Exception {
        assertEquals("The option --ou is unknown. Did you mean: --out or --output?", error("--ou"));
    }

    @Test
    public void testHiddenOptionIsNotSuggested() throws Exception {
        assertEquals("The option --secre is unknown.", error("--secre"));
    }

    @Test
    public void testNothingClose() throws Exception {
        assertEquals("The option --zzzzzz is unknown.", error("--zzzzzz"));
    }

    private static String error(String... args) throws Exception {
        CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                .commandRegistry(AeshCommandRegistryBuilder.<CommandInvocation> builder().command(EvalCommand.class)
                        .create())
                .build();
        try {
            runtime.executeCommand("eval", args);
        } catch (OptionParserException e) {
            return e.getMessage();
        }
        fail("an unknown option");
        return null;
    }

    @CommandDefinition(name = "eval", description = "")
    public static class EvalCommand implements Command<CommandInvocation> {
        @Option(name = "expert")
        String expert;

        @Option(name = "timeout")
        String timeout;

        @Option(name = "output", aliases = { "out" })
        String output;

        @Option(name = "secret", visibility = OptionVisibility.HIDDEN)
        String secret;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}

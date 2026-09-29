/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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
package org.aesh.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.aesh.command.activator.OptionActivator;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ParsedCommand;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionVisibility;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.DefaultAeshContext;
import org.aesh.terminal.formatting.TerminalString;
import org.aesh.util.completer.BashCompletionGenerator;
import org.aesh.util.doc.DocumentationGenerator;
import org.junit.Test;

/**
 * Cross-surface agreement for one shared fixture: help text, documentation
 * synopsis, dynamic completions and static bash candidates must agree on
 * eligible option forms, and agree on excluding hidden options from
 * invocation surfaces (#660).
 * <p>
 * Deliberate, documented differences encoded here, not fought:
 * help and skill docs are per-command references (own options only;
 * skill additionally marks HIDDEN for AI consumers), while completions
 * are invocation-aware (inherited options included, deactivated and
 * hidden excluded). Exclusivity only applies where parsed values exist
 * (dynamic), never in state-free static output.
 */
public class PresentationAgreementTest {

    public static class NeverActive implements OptionActivator {
        @Override
        public boolean isActivated(ParsedCommand parsedCommand) {
            return false;
        }
    }

    @CommandDefinition(name = "sub", description = "Sub op")
    public static class SubCommand implements Command<CommandInvocation> {
        @Option(name = "open", description = "open", aliases = { "op" })
        String open;

        @Option(name = "off", description = "off", activator = NeverActive.class)
        String off;

        @Option(name = "secret", description = "secret", visibility = OptionVisibility.HIDDEN)
        String secret;

        @Option(name = "cache", description = "cache", negatable = true, hasValue = false)
        boolean cache;

        @Argument(description = "target")
        String target;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "my-tool", description = "Hyphenated tool", groupCommands = { SubCommand.class })
    public static class ToolCommand implements Command<CommandInvocation> {
        @Option(name = "shared", description = "shared", inherited = true)
        String shared;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            return CommandResult.SUCCESS;
        }
    }

    private static CommandLineParser<CommandInvocation> rootParser() throws Exception {
        return new AeshCommandContainerBuilder<CommandInvocation>()
                .create(new ToolCommand()).getParser();
    }

    private static List<String> dynamicCandidates(String buffer) throws Exception {
        InvocationProviders providers = SettingsBuilder.builder().build().invocationProviders();
        AeshCompleteOperation operation = new AeshCompleteOperation(new DefaultAeshContext(),
                buffer, buffer.length());
        rootParser().complete(operation, providers);
        List<String> candidates = new ArrayList<>();
        for (TerminalString candidate : operation.getCompletionCandidates())
            candidates.add(candidate.getCharacters());
        return candidates;
    }

    @Test
    public void testEligibleOptionsAgreeAcrossSurfaces() throws Exception {
        String help = rootParser().getChildParser("sub").printHelp();
        String skill = DocumentationGenerator.builder()
                .commandClass(ToolCommand.class)
                .format(DocFormat.SKILL)
                .generateSingle();
        List<String> dynamic = dynamicCandidates("my-tool sub --");
        String script = new BashCompletionGenerator()
                .generate(rootParser(), "my-tool");

        // Own eligible options agree on every surface (negatables render
        // in --[no-] form in help and skill synopses).
        for (String form : new String[] { "--open" }) {
            assertTrue("help must show " + form + ", got: " + help, help.contains(form));
            assertTrue("skill must show " + form + ", got: " + skill, skill.contains(form));
            assertTrue("dynamic must offer " + form + ", got: " + dynamic,
                    dynamic.contains(form));
            assertTrue("static bash must offer " + form + ", got: " + script,
                    script.contains(form));
        }
        assertTrue("help must show the negatable form, got: " + help,
                help.contains("--[no-]cache"));
        assertTrue("skill must show the negatable form, got: " + skill,
                skill.contains("--[no-]cache"));
        // Inherited options are invocation-aware: completions (dynamic and
        // static) offer them at the leaf, while per-command references
        // (help, skill) list only own options.
        assertFalse("help lists own options only, got: " + help,
                help.contains("--shared"));
        assertTrue("dynamic must offer inherited options, got: " + dynamic,
                dynamic.contains("--shared"));
        assertTrue("static bash must offer inherited options, got: " + script,
                script.contains("--shared"));
        // Alias and negated forms travel everywhere eligible options go.
        assertTrue("dynamic must offer --op, got: " + dynamic, dynamic.contains("--op"));
        assertTrue("static bash must offer --op, got: " + script, script.contains("--op"));
        assertTrue("dynamic must offer --no-cache, got: " + dynamic,
                dynamic.contains("--no-cache"));
        assertTrue("static bash must offer --no-cache, got: " + script,
                script.contains("--no-cache"));
        // Positional label in help and skill docs.
        assertTrue("help must show the positional, got: " + help, help.contains("target"));
        assertTrue("skill must show the positional, got: " + skill, skill.contains("target"));
    }

    @Test
    public void testHiddenOptionsAbsentFromInvocationSurfaces() throws Exception {
        String help = rootParser().getChildParser("sub").printHelp();
        List<String> dynamic = dynamicCandidates("my-tool sub --");
        String script = new BashCompletionGenerator()
                .generate(rootParser(), "my-tool");

        assertFalse("help must not show hidden, got: " + help, help.contains("--secret"));
        assertFalse("dynamic must not offer hidden, got: " + dynamic,
                dynamic.contains("--secret"));
        assertFalse("static must not offer hidden, got: " + script,
                script.contains("--secret"));
    }

    @Test
    public void testHiddenOptionsMarkedInSkillDocs() throws Exception {
        // Skill docs deliberately expose the full surface for AI consumers,
        // marking hidden options instead of omitting them.
        String skill = DocumentationGenerator.builder()
                .commandClass(ToolCommand.class)
                .format(DocFormat.SKILL)
                .generateSingle();

        assertTrue("skill must list hidden options, got: " + skill,
                skill.contains("--secret"));
        assertTrue("skill must mark hidden options, got: " + skill,
                skill.contains("**hidden**"));
    }

    @Test
    public void testDeactivatedOptionsInDocsButNotCompletions() throws Exception {
        String help = rootParser().getChildParser("sub").printHelp();
        String skill = DocumentationGenerator.builder()
                .commandClass(ToolCommand.class)
                .format(DocFormat.SKILL)
                .generateSingle();
        List<String> dynamic = dynamicCandidates("my-tool sub --");
        String script = new BashCompletionGenerator()
                .generate(rootParser(), "my-tool");

        assertTrue("help documents deactivated options, got: " + help,
                help.contains("--off"));
        assertTrue("skill documents deactivated options, got: " + skill,
                skill.contains("--off"));
        assertFalse("dynamic must not offer deactivated options, got: " + dynamic,
                dynamic.contains("--off"));
        assertFalse("static must not offer deactivated options, got: " + script,
                script.contains("--off"));
    }
}

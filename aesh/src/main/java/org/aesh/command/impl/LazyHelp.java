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
package org.aesh.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.GroupCommand;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.AeshCommandLineParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.metadata.CommandMetadataProvider;
import org.aesh.command.metadata.MetadataProviderRegistry;
import org.aesh.command.operator.OperatorType;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.parser.ParsedLine;
import org.aesh.parser.ParsedWord;
import org.aesh.parser.ParserStatus;

public final class LazyHelp {

    private LazyHelp() {
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static String render(Class<? extends Command> rootClass, String[] args) {
        if (rootClass == null || !mayRequestHelp(args))
            return null;
        AeshCommandLineParser<CommandInvocation> root = buildTree(rootClass, args);
        if (root == null)
            return null;
        parse(root, root.getProcessedCommand().name(), args);
        if (findError(root) != null)
            return null;
        CommandLineParser<CommandInvocation> parsed = root.parsedCommand();
        if (parsed == null)
            return null;
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> parsedCommand = parsed.getProcessedCommand();
        if (parsedCommand.getHelpDocFormat() != null)
            return null;
        if (parsedCommand.generateHelp() && parsedCommand.isGenerateHelpOptionSet())
            return parsed.printHelp();
        if (parsedCommand.version() != null && parsedCommand.isGenerateVersionOptionSet())
            return parsedCommand.name() + " version: " + parsedCommand.version();
        return null;
    }

    private static boolean mayRequestHelp(String[] args) {
        if (args == null)
            return false;
        for (String arg : args) {
            if (arg == null)
                continue;
            if (arg.equals("-h") || arg.equals("--help") || arg.equals("-v") || arg.equals("--version")
                    || arg.startsWith("--help=") || arg.startsWith("--version="))
                return true;
        }
        return false;
    }

    private static AeshCommandLineParser<CommandInvocation> buildTree(Class<? extends Command> rootClass,
            String[] args) {
        AeshCommandLineParser<CommandInvocation> root = newParser(rootClass);
        if (root == null)
            return null;
        AeshCommandLineParser<CommandInvocation> current = root;
        Class<? extends Command> currentClass = rootClass;
        int index = 0;
        while (true) {
            List<Class<? extends Command>> children = childClasses(currentClass);
            if (children == null || children.isEmpty())
                return children == null ? null : root;
            for (Class<? extends Command> childClass : children) {
                AeshCommandLineParser<CommandInvocation> child = newParser(childClass);
                if (child == null)
                    return null;
                try {
                    current.addChildParser(child);
                } catch (CommandLineParserException e) {
                    return null;
                }
            }
            if (args == null || index >= args.length)
                return root;
            String token = args[index];
            if (token == null || token.isEmpty() || token.charAt(0) == '-' || token.equals("--"))
                return root;
            Class<? extends Command> matched = LazyRouteResolver.matchChild(currentClass, token);
            if (matched == null)
                return null;
            CommandLineParser<CommandInvocation> next = current.getChildParser(token);
            if (!(next instanceof AeshCommandLineParser))
                return null;
            current = (AeshCommandLineParser<CommandInvocation>) next;
            currentClass = matched;
            index++;
        }
    }

    private static List<Class<? extends Command>> childClasses(Class<? extends Command> parent) {
        if (GroupCommand.class.isAssignableFrom(parent))
            return null;
        CommandMetadataProvider<?> provider = MetadataProviderRegistry.getProvider(parent);
        if (provider != null) {
            if (!provider.isGroupCommand())
                return Collections.emptyList();
            Class<? extends Command>[] classes = provider.groupCommandClasses();
            if (classes == null)
                return Collections.emptyList();
            return Arrays.asList(classes);
        }
        CommandDefinition definition = parent.getAnnotation(CommandDefinition.class);
        if (definition == null || definition.groupCommands().length == 0)
            return Collections.emptyList();
        return Arrays.asList(definition.groupCommands());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static AeshCommandLineParser<CommandInvocation> newParser(Class<? extends Command> commandClass) {
        try {
            ProcessedCommand<Command<CommandInvocation>, CommandInvocation> processed;
            CommandMetadataProvider<?> provider = MetadataProviderRegistry.getProvider(commandClass);
            if (provider != null) {
                try {
                    processed = ((CommandMetadataProvider) provider).buildHelpProcessedCommand();
                } catch (UnsupportedOperationException e) {
                    processed = new AeshCommandContainerBuilder<CommandInvocation>()
                            .buildHelpProcessedCommand(commandClass);
                }
            } else {
                processed = new AeshCommandContainerBuilder<CommandInvocation>()
                        .buildHelpProcessedCommand(commandClass);
            }
            return new AeshCommandLineParser<>(processed);
        } catch (CommandLineParserException e) {
            return null;
        }
    }

    private static void parse(AeshCommandLineParser<CommandInvocation> root, String commandName, String[] args) {
        StringBuilder displayLine = new StringBuilder(commandName);
        List<ParsedWord> words = new ArrayList<>();
        words.add(new ParsedWord(commandName, 0));
        int offset = commandName.length() + 1;
        if (args != null) {
            for (String arg : args) {
                displayLine.append(' ').append(arg);
                words.add(new ParsedWord(arg, offset));
                offset += arg.length() + 1;
            }
        }
        ParsedLine line = new ParsedLine(displayLine.toString(), words,
                -1, -1, -1, ParserStatus.OK, "", OperatorType.NONE);
        root.parse(line.iterator(), CommandLineParser.Mode.STRICT);
    }

    private static CommandLineParserException findError(AeshCommandLineParser<CommandInvocation> parser) {
        if (!parser.getProcessedCommand().parserExceptions().isEmpty()
                && !parser.getProcessedCommand().hasOptionWithOverrideRequired())
            return parser.getProcessedCommand().parserExceptions().get(0);
        if (parser.isGroupCommand() && parser.getChildParsers() != null) {
            for (CommandLineParser<CommandInvocation> child : parser.getChildParsers()) {
                if (child instanceof AeshCommandLineParser) {
                    CommandLineParserException error = findError((AeshCommandLineParser<CommandInvocation>) child);
                    if (error != null)
                        return error;
                }
            }
        }
        return null;
    }
}

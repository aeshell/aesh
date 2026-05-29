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
package org.aesh.util.doc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.OptionVisibility;

/**
 * Renders command documentation in AsciiDoc format.
 * <p>
 * Produces pages compatible with Antora and standard AsciiDoc tooling.
 * Cross-reference links between pages use configurable prefixes.
 */
class AsciidocRenderer implements DocRenderer {

    private final String crossRefPrefix;

    AsciidocRenderer(String crossRefPrefix) {
        this.crossRefPrefix = crossRefPrefix != null ? crossRefPrefix : "";
    }

    @Override
    public String renderCommand(CommandLineParser<?> parser, String fullName, String parentName) {
        ProcessedCommand<?, ?> cmd = parser.getProcessedCommand();
        StringBuilder sb = new StringBuilder();

        // Title
        sb.append("= ").append(fullName.toUpperCase()).append("\n\n");

        // Name section
        sb.append("== NAME\n\n");
        sb.append(fullName.replace('-', ' ')).append(" -- ").append(cmd.description()).append("\n\n");

        // Synopsis section
        sb.append("== SYNOPSIS\n\n");
        sb.append(buildSynopsis(parser, fullName)).append("\n\n");

        // Description section
        if (cmd.description() != null && !cmd.description().isEmpty()) {
            sb.append("== DESCRIPTION\n\n");
            sb.append(cmd.description()).append("\n\n");
        }

        // Options section
        List<ProcessedOption> options = cmd.getDisplayOptions();
        boolean hasVisibleOptions = false;
        for (ProcessedOption opt : options) {
            if (opt.getVisibility() != OptionVisibility.HIDDEN) {
                hasVisibleOptions = true;
                break;
            }
        }
        if (hasVisibleOptions) {
            sb.append("== OPTIONS\n\n");
            for (ProcessedOption opt : options) {
                if (opt.getVisibility() == OptionVisibility.HIDDEN)
                    continue;
                renderOption(sb, opt);
            }
        }

        // Arguments section
        List<ProcessedOption> positionals = cmd.getPositionalOptionsInDisplayOrder();
        if (!positionals.isEmpty()) {
            sb.append("== ARGUMENTS\n\n");
            for (ProcessedOption pos : positionals) {
                renderArgument(sb, pos);
            }
        }

        // Subcommands section
        if (parser.isGroupCommand()) {
            List<CommandLineParser<CommandInvocation>> children = castChildren(parser);
            if (children != null && !children.isEmpty()) {
                sb.append("== COMMANDS\n\n");
                for (CommandLineParser<?> child : children) {
                    String childName = child.getProcessedCommand().name();
                    String childFullName = fullName + "-" + childName;
                    String childDesc = child.getProcessedCommand().description();
                    String fileName = childFullName + ".adoc";

                    if (!crossRefPrefix.isEmpty()) {
                        sb.append("* xref:").append(crossRefPrefix).append(fileName)
                                .append("[*").append(childName).append("*]");
                    } else {
                        sb.append("* link:").append(fileName)
                                .append("[*").append(childName).append("*]");
                    }
                    if (childDesc != null && !childDesc.isEmpty()) {
                        sb.append(" - ").append(childDesc);
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private void renderOption(StringBuilder sb, ProcessedOption opt) {
        // Option name line
        sb.append("*");

        // Short name
        if (opt.shortName() != null) {
            sb.append("-").append(opt.shortName());
            if (opt.name() != null && !opt.name().isEmpty()) {
                sb.append("*, *");
            }
        }

        // Long name (with negatable support)
        if (opt.name() != null && !opt.name().isEmpty()) {
            if (opt.isNegatable()) {
                sb.append("--[").append(opt.getNegationPrefix()).append("]").append(opt.name());
            } else {
                sb.append("--").append(opt.name());
            }
        }

        sb.append("*");

        // Value placeholder
        if (opt.hasValue() && opt.getOptionType() != OptionType.BOOLEAN
                && opt.type() != Boolean.class && opt.type() != boolean.class) {
            if (opt.getOptionType() == OptionType.GROUP) {
                sb.append("=<key>=<value>");
            } else {
                String label = opt.getArgument() != null && !opt.getArgument().isEmpty()
                        ? opt.getArgument()
                        : opt.name();
                sb.append("=<").append(label).append(">");
            }
        }

        sb.append("::\n");

        // Description
        if (opt.description() != null && !opt.description().isEmpty()) {
            sb.append(opt.description()).append("\n");
        }

        // Aliases
        if (!opt.getAliases().isEmpty()) {
            sb.append("+\nAliases: ");
            for (int i = 0; i < opt.getAliases().size(); i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append("`--").append(opt.getAliases().get(i)).append("`");
            }
            sb.append("\n");
        }

        // Default value
        if (!opt.getDefaultValues().isEmpty()) {
            sb.append("+\nDefault: `").append(String.join(", ", opt.getDefaultValues())).append("`\n");
        }

        sb.append("\n");
    }

    private void renderArgument(StringBuilder sb, ProcessedOption pos) {
        String label = pos.getDisplayLabel();
        sb.append("*<").append(label).append(">*::\n");

        if (pos.description() != null && !pos.description().isEmpty()) {
            sb.append(pos.description()).append("\n");
        }
        sb.append("\n");
    }

    private String buildSynopsis(CommandLineParser<?> parser, String fullName) {
        StringBuilder sb = new StringBuilder();
        sb.append("[source]\n----\n");
        sb.append(fullName.replace('-', ' '));

        ProcessedCommand<?, ?> cmd = parser.getProcessedCommand();
        List<ProcessedOption> options = cmd.getDisplayOptions();

        // Group boolean short flags
        StringBuilder shortFlags = new StringBuilder();
        for (ProcessedOption opt : options) {
            if (opt.getVisibility() == OptionVisibility.HIDDEN)
                continue;
            if (opt.getOptionType() == OptionType.BOOLEAN && opt.shortName() != null
                    && !opt.isRequired() && !opt.isNegatable()) {
                shortFlags.append(opt.shortName());
            }
        }
        if (shortFlags.length() > 0) {
            sb.append(" [-").append(shortFlags).append("]");
        }

        // Remaining options
        for (ProcessedOption opt : options) {
            if (opt.getVisibility() == OptionVisibility.HIDDEN)
                continue;
            if (opt.getOptionType() == OptionType.BOOLEAN && opt.shortName() != null
                    && !opt.isRequired() && !opt.isNegatable()) {
                continue; // already grouped
            }

            String optName;
            if (opt.isNegatable()) {
                optName = "--[" + opt.getNegationPrefix() + "]" + opt.name();
            } else {
                optName = opt.shortName() != null ? "-" + opt.shortName() : "--" + opt.name();
            }

            String rendered = optName;
            if (opt.getOptionType() == OptionType.GROUP) {
                rendered = optName + "<key>=<value>";
            } else if (opt.hasValue() && opt.getOptionType() != OptionType.BOOLEAN
                    && opt.type() != Boolean.class && opt.type() != boolean.class
                    && !opt.isOptionalValue() && !opt.hasFallbackValue()) {
                String label = opt.getArgument() != null && !opt.getArgument().isEmpty()
                        ? opt.getArgument()
                        : opt.name();
                rendered = optName + "=<" + label + ">";
            }

            if (opt.isRequired()) {
                sb.append(" ").append(rendered);
            } else {
                sb.append(" [").append(rendered).append("]");
            }
        }

        // Positional arguments
        for (ProcessedOption pos : cmd.getPositionalOptionsInDisplayOrder()) {
            String label = pos.getDisplayLabel();
            if (pos.isRequired()) {
                sb.append(" <").append(label).append(">");
            } else {
                sb.append(" [<").append(label).append(">]");
            }
        }

        // Subcommands placeholder
        if (parser.isGroupCommand()) {
            sb.append(" [COMMAND]");
        }

        sb.append("\n----");
        return sb.toString();
    }

    @Override
    public void writeNavFile(File navFile, List<DocumentationGenerator.NavEntry> entries) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (DocumentationGenerator.NavEntry entry : entries) {
            // Determine nesting depth from parent chain
            int depth = 0;
            String parent = entry.parentName;
            for (DocumentationGenerator.NavEntry e : entries) {
                if (e.fullName.equals(parent)) {
                    depth++;
                    parent = e.parentName;
                    break;
                }
            }

            // Antora nav format: * for top level, ** for children, etc.
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i <= depth; i++)
                stars.append('*');
            String prefix = stars.toString() + " ";
            String displayName = entry.fullName;
            // Use just the last segment for display
            int lastDash = displayName.lastIndexOf('-');
            if (lastDash > 0 && entry.parentName != null) {
                displayName = displayName.substring(lastDash + 1);
            }

            if (!crossRefPrefix.isEmpty()) {
                sb.append(prefix).append("xref:").append(crossRefPrefix).append(entry.fileName)
                        .append("[").append(displayName).append("]\n");
            } else {
                sb.append(prefix).append("link:").append(entry.fileName)
                        .append("[").append(displayName).append("]\n");
            }
        }

        if (!navFile.getParentFile().exists())
            navFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(navFile)) {
            writer.write(sb.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private List<CommandLineParser<CommandInvocation>> castChildren(CommandLineParser<?> parser) {
        return (List<CommandLineParser<CommandInvocation>>) (List<?>) parser.getAllChildParsers();
    }
}

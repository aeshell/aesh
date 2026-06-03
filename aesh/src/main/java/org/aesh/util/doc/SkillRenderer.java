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
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.aesh.command.HelpEntry;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;

/**
 * Renders command documentation as structured Markdown with YAML front matter,
 * optimized for AI agent/skill consumption following the
 * <a href="https://agentskills.io/specification">agentskills.io specification</a>.
 * <p>
 * Key differences from the regular Markdown renderer:
 * <ul>
 * <li>YAML front matter with name and description fields</li>
 * <li>Options and arguments rendered as tables with Type, Required, Default columns</li>
 * <li>All options included (including HIDDEN) since AI agents may need them</li>
 * <li>Subcommands listed in a table (not as links to separate files)</li>
 * <li>Format-specific description/sections from HelpSectionProvider</li>
 * </ul>
 */
class SkillRenderer implements DocRenderer {

    SkillRenderer() {
    }

    @Override
    public String renderCommand(CommandLineParser<?> parser, String fullName, String parentName,
            DocumentationGenerator.HelpSectionContent helpContent,
            DocumentationGenerator.NameContext nameCtx) {
        ProcessedCommand<?, ?> cmd = parser.getProcessedCommand();
        StringBuilder sb = new StringBuilder();

        // Resolve description: prefer format-specific override, then annotation description
        String description = helpContent.descriptionOverride != null
                ? helpContent.descriptionOverride
                : resolveDescription(cmd, cmd.description(), nameCtx);

        // YAML front matter (agentskills.io spec)
        sb.append("---\n");
        sb.append("name: ").append(cmd.name()).append("\n");
        if (description != null && !description.isEmpty()) {
            if (description.contains("\n")) {
                sb.append("description: >-\n");
                for (String line : description.split("\n")) {
                    sb.append("  ").append(line.trim()).append("\n");
                }
            } else {
                sb.append("description: ").append(escapeYaml(description)).append("\n");
            }
        }
        // Additional front matter from HelpSectionProvider
        if (helpContent.frontMatter != null) {
            for (Map.Entry<String, String> entry : helpContent.frontMatter.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(escapeYaml(entry.getValue())).append("\n");
            }
        }
        sb.append("---\n\n");

        // Usage / Synopsis
        sb.append("## Usage\n\n");
        sb.append("```\n");
        sb.append(buildSynopsis(parser, fullName));
        sb.append("\n```\n\n");

        // Header from HelpSectionProvider
        if (helpContent.header != null && !helpContent.header.isEmpty()) {
            sb.append(helpContent.header).append("\n\n");
        }

        // Options table (all options including HIDDEN)
        List<ProcessedOption> options = cmd.getDisplayOptions();
        if (!options.isEmpty()) {
            sb.append("## Options\n\n");
            sb.append("| Option | Type | Required | Default | Description |\n");
            sb.append("|--------|------|----------|---------|-------------|\n");
            for (ProcessedOption opt : options) {
                renderOptionRow(sb, opt, cmd, nameCtx);
            }
            sb.append("\n");
        }

        // Arguments table
        List<ProcessedOption> positionals = cmd.getPositionalOptionsInDisplayOrder();
        if (!positionals.isEmpty()) {
            sb.append("## Arguments\n\n");
            sb.append("| Argument | Type | Required | Description |\n");
            sb.append("|----------|------|----------|-------------|\n");
            for (ProcessedOption pos : positionals) {
                renderArgumentRow(sb, pos);
            }
            sb.append("\n");
        }

        // Subcommands table
        if (parser.isGroupCommand()) {
            @SuppressWarnings("unchecked")
            List<CommandLineParser<CommandInvocation>> children = (List<CommandLineParser<CommandInvocation>>) (List<?>) parser
                    .getAllChildParsers();
            if (children != null && !children.isEmpty()) {
                sb.append("## Commands\n\n");
                sb.append("| Command | Description |\n");
                sb.append("|---------|-------------|\n");
                for (CommandLineParser<?> child : children) {
                    String childName = child.getProcessedCommand().name();
                    String childFullName = fullName + "-" + childName;
                    String childDesc = resolveDescription(child.getProcessedCommand(),
                            child.getProcessedCommand().description(),
                            new DocumentationGenerator.NameContext(childName, childFullName,
                                    nameCtx.rootName, fullName));
                    sb.append("| `").append(childName).append("` | ");
                    sb.append(childDesc != null ? escapeMdTable(childDesc) : "").append(" |\n");
                }
                sb.append("\n");
            }
        }

        // Additional sections from HelpSectionProvider
        if (!helpContent.additionalSections.isEmpty()) {
            for (Map.Entry<String, List<HelpEntry>> section : helpContent.additionalSections.entrySet()) {
                sb.append("## ").append(section.getKey()).append("\n\n");
                for (HelpEntry entry : section.getValue()) {
                    sb.append("- `").append(entry.name()).append("`");
                    if (entry.description() != null && !entry.description().isEmpty()) {
                        sb.append(" -- ").append(entry.description());
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }

        // Footer from HelpSectionProvider
        if (helpContent.footer != null && !helpContent.footer.isEmpty()) {
            sb.append(helpContent.footer).append("\n");
        }

        return sb.toString();
    }

    private void renderOptionRow(StringBuilder sb, ProcessedOption opt,
            ProcessedCommand<?, ?> cmd, DocumentationGenerator.NameContext nameCtx) {
        sb.append("| `");

        // Option name(s)
        boolean hasShort = opt.shortName() != null && !opt.shortName().isEmpty();
        boolean hasLong = opt.name() != null && !opt.name().isEmpty();

        if (hasLong) {
            sb.append("--").append(opt.name());
        }
        if (hasShort) {
            if (hasLong)
                sb.append("`, `-");
            sb.append(opt.shortName());
        }

        // Value placeholder
        if (opt.hasValue() && opt.getOptionType() != OptionType.BOOLEAN
                && opt.type() != Boolean.class && opt.type() != boolean.class) {
            if (opt.getOptionType() == OptionType.GROUP) {
                sb.append(" <key>=<value>");
            } else {
                String label = opt.getArgument() != null && !opt.getArgument().isEmpty()
                        ? opt.getArgument()
                        : opt.name();
                sb.append("=<").append(label).append(">");
            }
        }
        sb.append("` | ");

        // Type
        String typeName = opt.type().getSimpleName();
        sb.append(typeName).append(" | ");

        // Required
        sb.append(opt.isRequired() ? "yes" : "no").append(" | ");

        // Default
        if (!opt.getDefaultValues().isEmpty()) {
            sb.append("`").append(String.join(", ", opt.getDefaultValues())).append("`");
        } else {
            sb.append("-");
        }
        sb.append(" | ");

        // Description
        String desc = resolveDescription(cmd, opt.description(), nameCtx);
        if (desc != null) {
            sb.append(escapeMdTable(desc));
        }

        // Allowed values
        if (opt.getAllowedValues() != null && !opt.getAllowedValues().isEmpty()) {
            sb.append(" Allowed: ").append(String.join(", ", opt.getAllowedValues()));
        }

        // Aliases
        if (!opt.getAliases().isEmpty()) {
            sb.append(" Aliases: ");
            for (int i = 0; i < opt.getAliases().size(); i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append("`--").append(opt.getAliases().get(i)).append("`");
            }
        }

        sb.append(" |\n");
    }

    private void renderArgumentRow(StringBuilder sb, ProcessedOption pos) {
        String label = pos.getDisplayLabel();
        sb.append("| `<").append(label).append(">` | ");
        sb.append(pos.type().getSimpleName()).append(" | ");
        sb.append(pos.isRequired() ? "yes" : "no").append(" | ");
        sb.append(pos.description() != null ? escapeMdTable(pos.description()) : "").append(" |\n");
    }

    private String buildSynopsis(CommandLineParser<?> parser, String fullName) {
        StringBuilder sb = new StringBuilder();
        sb.append(fullName.replace('-', ' '));

        ProcessedCommand<?, ?> cmd = parser.getProcessedCommand();
        List<ProcessedOption> options = cmd.getDisplayOptions();

        // All options shown (including HIDDEN for AI)
        for (ProcessedOption opt : options) {
            String optName;
            if (opt.shortName() != null && !opt.shortName().isEmpty()) {
                optName = "-" + opt.shortName();
            } else {
                optName = "--" + opt.name();
            }

            if (opt.isRequired()) {
                sb.append(" ").append(optName);
            } else {
                sb.append(" [").append(optName).append("]");
            }
        }

        for (ProcessedOption pos : cmd.getPositionalOptionsInDisplayOrder()) {
            String label = pos.getDisplayLabel();
            if (pos.isRequired()) {
                sb.append(" <").append(label).append(">");
            } else {
                sb.append(" [<").append(label).append(">]");
            }
        }

        if (parser.isGroupCommand()) {
            sb.append(" [COMMAND]");
        }

        return sb.toString();
    }

    private String resolveDescription(ProcessedCommand<?, ?> cmd, String raw,
            DocumentationGenerator.NameContext ctx) {
        if (raw == null || raw.isEmpty() || ctx == null)
            return raw;
        return cmd.resolveCommandDescription(raw,
                ctx.commandName,
                ctx.fullName != null ? ctx.fullName.replace('-', ' ') : ctx.commandName,
                ctx.rootName,
                ctx.parentName,
                ctx.parentName != null ? ctx.parentName.replace('-', ' ') : null);
    }

    /** Escape pipe characters in table cells. */
    private static String escapeMdTable(String text) {
        return text.replace("|", "\\|").replace("\n", " ");
    }

    /** Escape YAML special characters in values. */
    private static String escapeYaml(String text) {
        if (text.contains(":") || text.contains("#") || text.contains("\"")
                || text.startsWith("'") || text.startsWith("{") || text.startsWith("[")) {
            return "\"" + text.replace("\"", "\\\"") + "\"";
        }
        return text;
    }

    @Override
    public void writeNavFile(File navFile, List<DocumentationGenerator.NavEntry> entries) throws IOException {
        // Skill format doesn't use nav files -- all content is inline
    }
}

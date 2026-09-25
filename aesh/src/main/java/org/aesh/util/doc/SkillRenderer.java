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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.aesh.command.HelpEntry;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.OptionVisibility;

/**
 * Renders command documentation as structured Markdown with YAML front matter,
 * optimized for AI agent/skill consumption following the
 * <a href="https://agentskills.io/specification">agentskills.io specification</a>.
 * <p>
 * Key differences from the regular Markdown renderer:
 * <ul>
 * <li>YAML front matter with command (full path) and name fields</li>
 * <li>Options rendered as tables with Forms, Type, Required, Default, Description columns</li>
 * <li>All options included (including HIDDEN, labeled) since AI agents may need them</li>
 * <li>Subcommands listed with full invocation paths</li>
 * <li>Format-specific description/sections from HelpSectionProvider</li>
 * </ul>
 *
 * @since 3.18
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

        String commandPath = fullName.replace('-', ' ');

        // Resolve description: prefer format-specific override, then annotation description
        String description = helpContent.descriptionOverride != null
                ? helpContent.descriptionOverride
                : resolveDescription(cmd, cmd.description(), nameCtx);

        // YAML front matter (agentskills.io spec)
        sb.append("---\n");
        sb.append("command: ").append(quoteYaml(commandPath)).append("\n");
        sb.append("name: ").append(quoteYaml(cmd.name())).append("\n");
        if (description != null && !description.isEmpty()) {
            if (description.contains("\n")) {
                sb.append("description: >-\n");
                for (String line : description.split("\n")) {
                    sb.append("  ").append(line.trim()).append("\n");
                }
            } else {
                sb.append("description: ").append(quoteYaml(description)).append("\n");
            }
        }
        // Command aliases
        List<String> aliases = cmd.getAliases();
        if (aliases != null && !aliases.isEmpty()) {
            sb.append("aliases:\n");
            for (String alias : aliases) {
                sb.append("  - ").append(quoteYaml(alias)).append("\n");
            }
        }
        // Additional front matter from HelpSectionProvider
        if (helpContent.frontMatter != null) {
            for (Map.Entry<String, String> entry : helpContent.frontMatter.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(quoteYaml(entry.getValue())).append("\n");
            }
        }
        sb.append("---\n\n");

        // Heading: full invocation path
        sb.append("# ").append(commandPath).append("\n\n");

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
            sb.append("| Forms | Type | Required | Default | Description |\n");
            sb.append("|-------|------|----------|---------|-------------|\n");
            for (ProcessedOption opt : options) {
                renderOptionRow(sb, opt, cmd, nameCtx);
            }
            sb.append("\n");
        }

        // Arguments table
        List<ProcessedOption> positionals = cmd.getPositionalOptionsInDisplayOrder();
        if (!positionals.isEmpty()) {
            sb.append("## Arguments\n\n");
            sb.append("| Argument | Type | Required | Default | Description |\n");
            sb.append("|----------|------|----------|---------|-------------|\n");
            for (ProcessedOption pos : positionals) {
                renderArgumentRow(sb, pos, cmd, nameCtx);
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
                    String childPath = commandPath + " " + childName;
                    String childFullName = fullName + "-" + childName;
                    String childDesc = resolveDescription(child.getProcessedCommand(),
                            child.getProcessedCommand().description(),
                            new DocumentationGenerator.NameContext(childName, childFullName,
                                    nameCtx.rootName, fullName));
                    sb.append("| `").append(childPath).append("` | ");
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

        // Forms: all accepted flag forms in one code span
        sb.append("| `");
        sb.append(buildForms(opt));
        sb.append("` | ");

        // Type: CLI-facing label
        sb.append(cliTypeName(opt)).append(" | ");

        // Required
        sb.append(opt.isRequired() ? "yes" : "no").append(" | ");

        // Default
        if (!opt.getDefaultValues().isEmpty()) {
            sb.append(mdCode(join(opt.getDefaultValues(), ", ")));
        } else {
            sb.append("-");
        }
        sb.append(" | ");

        // Description (with visibility tag and extras)
        StringBuilder desc = new StringBuilder();
        if (opt.getVisibility() == OptionVisibility.HIDDEN) {
            desc.append("**hidden** ");
        }
        String resolved = resolveDescription(cmd, opt.description(), nameCtx);
        if (resolved != null) {
            desc.append(escapeMdTable(resolved));
        }
        if (opt.hasAllowedValues()) {
            desc.append(" Allowed: ");
            desc.append(mdCodeList(opt.getAllowedValues()));
        }
        if (!opt.getAliases().isEmpty()) {
            desc.append(" Aliases: ");
            List<String> aliasNames = new ArrayList<>(opt.getAliases().size());
            for (String alias : opt.getAliases()) {
                aliasNames.add("`--" + alias + "`");
            }
            desc.append(join(aliasNames, ", "));
        }
        sb.append(desc);

        sb.append(" |\n");
    }

    /**
     * Builds the forms string for an option: all accepted flag syntaxes.
     * Example: {@code --environment, -e <environment>} or {@code --[no-]cds}.
     */
    private String buildForms(ProcessedOption opt) {
        StringBuilder forms = new StringBuilder();
        boolean hasLong = opt.name() != null && !opt.name().isEmpty();
        boolean hasShort = opt.shortName() != null && !opt.shortName().isEmpty();
        boolean noDashes = opt.acceptNameWithoutDashes();
        String longPrefix = noDashes ? "" : "--";

        if (hasLong) {
            if (opt.isNegatable()) {
                forms.append(longPrefix).append("[").append(opt.getNegationPrefix()).append("]").append(opt.name());
            } else {
                forms.append(longPrefix).append(opt.name());
            }
        }
        if (hasShort) {
            if (forms.length() > 0)
                forms.append(", ");
            forms.append("-").append(opt.shortName());
        }

        // Value placeholder
        if (opt.hasValue() && opt.getOptionType() != OptionType.BOOLEAN
                && opt.type() != Boolean.class && opt.type() != boolean.class) {
            if (opt.getOptionType() == OptionType.GROUP) {
                forms.append(" <key>=<value>");
            } else if (!opt.isOptionalValue() && !opt.hasFallbackValue()) {
                String label = opt.getArgument() != null && !opt.getArgument().isEmpty()
                        ? opt.getArgument()
                        : opt.name();
                forms.append("=<").append(label).append(">");
            }
        }
        return forms.toString();
    }

    private void renderArgumentRow(StringBuilder sb, ProcessedOption pos,
            ProcessedCommand<?, ?> cmd, DocumentationGenerator.NameContext nameCtx) {
        String label = pos.getDisplayLabel();
        sb.append("| `<").append(label).append(">`");
        // Arity annotation
        if (pos.getArity() != null) {
            sb.append(" (arity: ").append(pos.getArity()).append(")");
        }
        // Index annotation
        if (pos.hasIndexRange()) {
            org.aesh.command.option.IndexRange ir = pos.getIndexRange();
            sb.append(" (index: ").append(ir.getMin());
            if (ir.getMax() != ir.getMin()) {
                sb.append("..").append(ir.getMax() == Integer.MAX_VALUE ? "*" : ir.getMax());
            }
            sb.append(")");
        }
        sb.append(" | ");

        sb.append(cliTypeName(pos)).append(" | ");
        sb.append(pos.isRequired() ? "yes" : "no").append(" | ");

        // Default
        if (!pos.getDefaultValues().isEmpty()) {
            sb.append(mdCode(join(pos.getDefaultValues(), ", ")));
        } else {
            sb.append("-");
        }
        sb.append(" | ");

        String desc = resolveDescription(cmd, pos.description(), nameCtx);
        sb.append(desc != null ? escapeMdTable(desc) : "").append(" |\n");
    }

    /**
     * Returns a CLI-facing type label instead of the Java class simple name.
     */
    private static String cliTypeName(ProcessedOption opt) {
        OptionType ot = opt.getOptionType();
        if (ot == OptionType.BOOLEAN)
            return "flag";
        if (ot == OptionType.LIST || ot == OptionType.ARGUMENTS)
            return "list";
        if (ot == OptionType.GROUP)
            return "map";

        Class<?> type = opt.type();
        if (type == Boolean.class || type == boolean.class)
            return "flag";
        if (type == Integer.class || type == int.class
                || type == Long.class || type == long.class
                || type == Short.class || type == short.class)
            return "integer";
        if (type == Float.class || type == float.class
                || type == Double.class || type == double.class)
            return "number";
        if (type == java.io.File.class || "Resource".equals(type.getSimpleName()))
            return "path";
        return "string";
    }

    private String buildSynopsis(CommandLineParser<?> parser, String fullName) {
        StringBuilder sb = new StringBuilder();
        sb.append(fullName.replace('-', ' '));
        sb.append(parser.getProcessedCommand().buildSynopsisString(true, parser.isGroupCommand()));
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

    /** Escape pipe characters and newlines in table cells. */
    private static String escapeMdTable(String text) {
        return text.replace("|", "\\|").replace("\n", " ");
    }

    /** Wrap a value in a Markdown code span, escaping backticks. */
    private static String mdCode(String text) {
        if (text.indexOf('`') >= 0) {
            return "`` " + text + " ``";
        }
        return "`" + text + "`";
    }

    /** Render a list of values as comma-separated Markdown code spans. */
    private static String mdCodeList(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(mdCode(values.get(i)));
        }
        return sb.toString();
    }

    /** Join strings with a delimiter (no Streams to avoid lambda overhead). */
    private static String join(List<String> items, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0)
                sb.append(delimiter);
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    /**
     * Quote a YAML scalar value safely.
     * Always double-quotes if the value contains characters that are ambiguous
     * in plain YAML (colons, hashes, quotes, brackets, leading indicators,
     * reserved words). Plain scalars are returned unquoted.
     */
    static String quoteYaml(String text) {
        if (text == null || text.isEmpty())
            return "\"\"";

        char first = text.charAt(0);
        boolean needsQuote = false;

        // Leading YAML indicators
        if (first == '-' || first == '?' || first == ':' || first == '!'
                || first == '&' || first == '*' || first == '{' || first == '}'
                || first == '[' || first == ']' || first == ',' || first == '#'
                || first == '|' || first == '>' || first == '@' || first == '`'
                || first == '\'' || first == '"' || first == '%'
                || first == ' ' || first == '\t') {
            needsQuote = true;
        }

        // Scan for inline ambiguity characters
        if (!needsQuote) {
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == ':' || c == '#' || c == '"' || c == '\''
                        || c == '\n' || c == '\r' || c == '\t'
                        || c == '\\' || c == '\0') {
                    needsQuote = true;
                    break;
                }
            }
        }

        // YAML boolean/null reserved words (case-insensitive)
        if (!needsQuote) {
            String lower = text.toLowerCase(java.util.Locale.ROOT);
            if ("true".equals(lower) || "false".equals(lower)
                    || "yes".equals(lower) || "no".equals(lower)
                    || "on".equals(lower) || "off".equals(lower)
                    || "null".equals(lower) || "~".equals(text)) {
                needsQuote = true;
            }
        }

        // Trailing space/tab
        if (!needsQuote) {
            char last = text.charAt(text.length() - 1);
            if (last == ' ' || last == '\t') {
                needsQuote = true;
            }
        }

        if (!needsQuote)
            return text;

        // Double-quote and escape internal double-quotes and backslashes
        StringBuilder sb = new StringBuilder(text.length() + 4);
        sb.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\');
            }
            sb.append(c);
        }
        sb.append('"');
        return sb.toString();
    }

    @Override
    public void writeNavFile(File navFile, List<DocumentationGenerator.NavEntry> entries) throws IOException {
        // Skill format doesn't use nav files -- all content is inline
    }
}

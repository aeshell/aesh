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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.aesh.command.Command;
import org.aesh.command.HelpEntry;
import org.aesh.command.HelpSectionProvider;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.provider.NullHelpSectionProvider;
import org.aesh.command.parser.CommandLineParserException;

/**
 * Generates documentation files from command metadata.
 * <p>
 * Produces one file per command/subcommand in AsciiDoc or Markdown format,
 * with optional navigation file generation for site frameworks like Antora.
 * <p>
 * Usage:
 *
 * <pre>
 * DocumentationGenerator.builder()
 *         .commandClass(MyCommand.class)
 *         .outputDir(new File("docs/pages"))
 *         .format(DocFormat.ASCIIDOC)
 *         .crossRefPrefix("myapp:cli:")
 *         .generate();
 * </pre>
 *
 * @author Aesh team
 */
public class DocumentationGenerator {

    private final CommandLineParser<?> parser;
    private final String programName;
    private final DocFormat format;
    private final String crossRefPrefix;
    private final File outputDir;
    private final File navFile;

    private DocumentationGenerator(Builder builder) {
        this.parser = builder.parser;
        this.programName = builder.programName != null ? builder.programName : parser.getProcessedCommand().name();
        this.format = builder.format;
        this.crossRefPrefix = builder.crossRefPrefix != null ? builder.crossRefPrefix : "";
        this.outputDir = builder.outputDir;
        this.navFile = builder.navFile;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Generate documentation for a single command (and its subcommands) to a string.
     * Useful for stdout output or testing.
     */
    public String generateSingle() {
        DocRenderer renderer = createRenderer();
        HelpSectionContent helpContent = resolveHelpContent(parser, programName, null);
        NameContext nameCtx = buildNameContext(parser, programName, null);
        return renderer.renderCommand(parser, programName, null, helpContent, nameCtx);
    }

    /**
     * Generate documentation files — one per command/subcommand.
     * If navFile is set, also generates a navigation file.
     */
    public void generate() throws IOException {
        if (outputDir == null)
            throw new IllegalStateException("outputDir must be set for file generation");

        if (!outputDir.exists())
            outputDir.mkdirs();

        DocRenderer renderer = createRenderer();
        List<NavEntry> navEntries = new ArrayList<>();

        generateRecursive(parser, programName, null, renderer, navEntries);

        if (navFile != null) {
            renderer.writeNavFile(navFile, navEntries);
        }
    }

    private void generateRecursive(CommandLineParser<?> parser, String fullName,
            String parentName, DocRenderer renderer, List<NavEntry> navEntries) throws IOException {
        HelpSectionContent helpContent = resolveHelpContent(parser, fullName, parentName);
        NameContext nameCtx = buildNameContext(parser, fullName, parentName);
        String content = renderer.renderCommand(parser, fullName, parentName, helpContent, nameCtx);
        String fileName = fullName.replace(' ', '-') + "." + format.extension();
        File outFile = new File(outputDir, fileName);

        try (FileWriter writer = new FileWriter(outFile)) {
            writer.write(content);
        }

        navEntries.add(new NavEntry(fullName, fileName, parentName));

        if (parser.isGroupCommand()) {
            for (CommandLineParser<?> child : parser.getAllChildParsers()) {
                String childFullName = fullName + "-" + child.getProcessedCommand().name();
                generateRecursive(child, childFullName, fullName, renderer, navEntries);
            }
        }
    }

    /**
     * Resolves HelpSectionProvider content from a parser, with variable interpolation.
     */
    private HelpSectionContent resolveHelpContent(CommandLineParser<?> parser,
            String fullName, String parentName) {
        HelpSectionProvider provider = parser.getProcessedCommand().getHelpSectionProvider();
        if (provider == null) {
            Class<? extends HelpSectionProvider> providerClass = parser.getProcessedCommand().getHelpSectionProviderClass();
            if (providerClass != null && providerClass != NullHelpSectionProvider.class) {
                try {
                    provider = providerClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    // fall through
                }
            }
        }
        if (provider == null) {
            return HelpSectionContent.EMPTY;
        }

        // Resolve ${COMMAND-NAME}, ${ROOT-COMMAND-NAME} etc. in provider content
        String commandName = parser.getProcessedCommand().name();
        String header = resolveVariables(provider.getHeader(), commandName, fullName, parentName);
        String footer = resolveVariables(provider.getFooter(), commandName, fullName, parentName);

        // Resolve variables in additional section entries
        Map<String, List<HelpEntry>> sections = provider.getAdditionalSections();
        if (sections != null && !sections.isEmpty()) {
            Map<String, List<HelpEntry>> resolved = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, List<HelpEntry>> entry : sections.entrySet()) {
                List<HelpEntry> resolvedEntries = new ArrayList<>();
                for (HelpEntry he : entry.getValue()) {
                    resolvedEntries.add(new HelpEntry(
                            resolveVariables(he.name(), commandName, fullName, parentName),
                            resolveVariables(he.description(), commandName, fullName, parentName)));
                }
                resolved.put(entry.getKey(), resolvedEntries);
            }
            sections = resolved;
        }

        return new HelpSectionContent(header, footer, sections);
    }

    private NameContext buildNameContext(CommandLineParser<?> parser, String fullName, String parentName) {
        String commandName = parser.getProcessedCommand().name();
        String rootName = programName;
        int dashIdx = rootName.indexOf('-');
        if (dashIdx > 0)
            rootName = rootName.substring(0, dashIdx);
        return new NameContext(commandName, fullName, rootName, parentName);
    }

    private String resolveVariables(String text, String commandName, String fullName, String parentName) {
        if (text == null || text.isEmpty())
            return text;
        // Derive root command name from fullName (first segment)
        String rootName = programName;
        int dashIdx = rootName.indexOf('-');
        if (dashIdx > 0)
            rootName = rootName.substring(0, dashIdx);

        return parser.getProcessedCommand().resolveCommandDescription(text,
                commandName,
                fullName != null ? fullName.replace('-', ' ') : commandName,
                rootName,
                parentName,
                parentName != null ? parentName.replace('-', ' ') : null);
    }

    /**
     * Name context for variable resolution in descriptions.
     */
    static class NameContext {
        final String commandName;
        final String fullName;
        final String rootName;
        final String parentName;

        NameContext(String commandName, String fullName, String rootName, String parentName) {
            this.commandName = commandName;
            this.fullName = fullName;
            this.rootName = rootName;
            this.parentName = parentName;
        }
    }

    /**
     * Resolved content from a HelpSectionProvider.
     */
    static class HelpSectionContent {
        static final HelpSectionContent EMPTY = new HelpSectionContent(null, null, Collections.emptyMap());

        final String header;
        final String footer;
        final Map<String, List<HelpEntry>> additionalSections;

        HelpSectionContent(String header, String footer, Map<String, List<HelpEntry>> additionalSections) {
            this.header = header;
            this.footer = footer;
            this.additionalSections = additionalSections != null ? additionalSections : Collections.emptyMap();
        }
    }

    private DocRenderer createRenderer() {
        switch (format) {
            case ASCIIDOC:
                return new AsciidocRenderer(crossRefPrefix);
            case MARKDOWN:
                return new MarkdownRenderer(crossRefPrefix);
            default:
                throw new IllegalArgumentException("Unknown format: " + format);
        }
    }

    /**
     * Navigation entry for building nav files.
     */
    static class NavEntry {
        final String fullName;
        final String fileName;
        final String parentName;

        NavEntry(String fullName, String fileName, String parentName) {
            this.fullName = fullName;
            this.fileName = fileName;
            this.parentName = parentName;
        }
    }

    public static class Builder {
        private CommandLineParser<?> parser;
        private String programName;
        private DocFormat format = DocFormat.ASCIIDOC;
        private String crossRefPrefix;
        private File outputDir;
        private File navFile;

        private Builder() {
        }

        /**
         * Set the command parser to generate docs for.
         */
        public Builder parser(CommandLineParser<?> parser) {
            this.parser = parser;
            return this;
        }

        /**
         * Set the command class to generate docs for.
         * Creates a parser automatically.
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public Builder commandClass(Class<? extends Command> commandClass) throws CommandLineParserException {
            AeshCommandContainerBuilder containerBuilder = new AeshCommandContainerBuilder();
            CommandContainer container = containerBuilder.create(commandClass);
            this.parser = container.getParser();
            return this;
        }

        /**
         * Set the program name used in documentation.
         * Defaults to the command name from the annotation.
         */
        public Builder programName(String programName) {
            this.programName = programName;
            return this;
        }

        /**
         * Set the output format. Default is ASCIIDOC.
         */
        public Builder format(DocFormat format) {
            this.format = format;
            return this;
        }

        /**
         * Set the cross-reference prefix for links between pages.
         * For Antora: "myapp:cli:" produces xref:myapp:cli:command.adoc[name]
         * For plain: "" produces link:command.adoc[name]
         */
        public Builder crossRefPrefix(String crossRefPrefix) {
            this.crossRefPrefix = crossRefPrefix;
            return this;
        }

        /**
         * Set the output directory for generated files.
         * One file per command/subcommand is created.
         */
        public Builder outputDir(File outputDir) {
            this.outputDir = outputDir;
            return this;
        }

        /**
         * Set the navigation file to generate.
         * Produces an Antora-compatible nav.adoc or Markdown index.
         */
        public Builder navFile(File navFile) {
            this.navFile = navFile;
            return this;
        }

        /**
         * Generate documentation files to outputDir.
         */
        public void generate() throws IOException {
            new DocumentationGenerator(this).generate();
        }

        /**
         * Generate documentation for a single command to a string.
         */
        public String generateSingle() {
            return new DocumentationGenerator(this).generateSingle();
        }

        /**
         * Build the generator without executing.
         */
        public DocumentationGenerator build() {
            return new DocumentationGenerator(this);
        }
    }
}

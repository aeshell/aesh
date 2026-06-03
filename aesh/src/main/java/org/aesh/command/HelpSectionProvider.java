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

import java.util.List;
import java.util.Map;

/**
 * Provider interface for dynamically adding entries to help output.
 * Implementations can discover external plugins, aliases, or any
 * other commands that should appear in help but aren't statically
 * defined via annotations.
 *
 * <p>
 * Only invoked at help-render time — no startup cost.
 * </p>
 * <p>
 * Format-aware overloads allow providing different content for different
 * output formats (e.g., AI skill documentation vs human-readable help).
 * The default implementations delegate to the parameterless methods,
 * so existing implementations are fully backward compatible.
 * </p>
 *
 * @author Aesh team
 */
public interface HelpSectionProvider {

    /**
     * Returns additional sections to display in help output.
     * Each map key is a section heading (e.g. "External", "Plugins"),
     * and the value is the list of entries to display under that heading.
     *
     * <p>
     * If a section name matches an existing {@code helpGroup} from
     * statically defined commands, the entries are appended to that group.
     * </p>
     *
     * @return map of section name to help entries, never null
     */
    Map<String, List<HelpEntry>> getAdditionalSections();

    /**
     * Returns header text to display before the synopsis in help output.
     * Can contain multiple lines separated by newline characters.
     * This is useful for brand text, taglines, or usage examples.
     *
     * @return header text, or null if no header should be shown
     */
    default String getHeader() {
        return null;
    }

    /**
     * Returns footer text to display after all other help content.
     * Can contain multiple lines separated by newline characters.
     * This is useful for copyright notices, links, or "see also" text.
     *
     * @return footer text, or null if no footer should be shown
     */
    default String getFooter() {
        return null;
    }

    // --- Format-aware overloads ---

    /**
     * Returns format-specific additional sections.
     * Override this to provide different sections for different output formats
     * (e.g., AI-specific examples for {@link DocFormat#SKILL}).
     *
     * @param format the documentation output format
     * @return map of section name to help entries, never null
     */
    default Map<String, List<HelpEntry>> getAdditionalSections(DocFormat format) {
        return getAdditionalSections();
    }

    /**
     * Returns format-specific header text.
     *
     * @param format the documentation output format
     * @return header text, or null if no header should be shown
     */
    default String getHeader(DocFormat format) {
        return getHeader();
    }

    /**
     * Returns format-specific footer text.
     *
     * @param format the documentation output format
     * @return footer text, or null if no footer should be shown
     */
    default String getFooter(DocFormat format) {
        return getFooter();
    }

    /**
     * Returns a format-specific description override for the command.
     * When non-null, this replaces the {@code @CommandDefinition(description=...)}
     * value in the generated documentation.
     * <p>
     * This is useful for AI skill documentation where the description
     * should explain both what the command does and when to use it,
     * which differs from the terse description shown in {@code --help}.
     *
     * @param format the documentation output format
     * @return description override, or null to use the annotation description
     */
    default String getDescription(DocFormat format) {
        return null;
    }

    /**
     * Returns additional YAML front matter key-value pairs for formats
     * that support it (e.g., {@link DocFormat#SKILL}).
     * <p>
     * Standard fields like {@code name} and {@code description} are
     * generated automatically. Use this for optional fields like
     * {@code license}, {@code compatibility}, or {@code metadata}.
     *
     * @param format the documentation output format
     * @return map of front matter keys to values, or null
     */
    default Map<String, String> getFrontMatter(DocFormat format) {
        return null;
    }
}

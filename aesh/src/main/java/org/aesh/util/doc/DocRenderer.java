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

import org.aesh.command.impl.parser.CommandLineParser;

/**
 * Renders documentation for a command in a specific format.
 */
interface DocRenderer {

    /**
     * Render documentation for a single command.
     *
     * @param parser the command parser
     * @param fullName the full command name (e.g., "jbang-run")
     * @param parentName the parent command name (null for top-level)
     * @return the rendered documentation as a string
     */
    String renderCommand(CommandLineParser<?> parser, String fullName, String parentName,
            DocumentationGenerator.HelpSectionContent helpContent);

    /**
     * Write a navigation file listing all generated pages.
     */
    void writeNavFile(File navFile, List<DocumentationGenerator.NavEntry> entries) throws IOException;
}

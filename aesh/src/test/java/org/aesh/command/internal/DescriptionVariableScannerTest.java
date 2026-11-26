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
package org.aesh.command.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.junit.Test;

/**
 * Pins the plain-scanner semantics of {@code ${...}} description variables
 * (COMMAND-NAME, ROOT-COMMAND-NAME, ...) to the historical regex
 * {@code \$\{([^}]+)}: at least one non-} character between the braces,
 * left-to-right, unresolved keys and malformed references stay literal, and
 * replacements are literal text even when they contain {@code $} (#793).
 */
public class DescriptionVariableScannerTest {

    private static ProcessedCommand<?, ?> command(String name, String parentName, String programName) {
        try {
            ProcessedCommandBuilder<?, ?> builder = ProcessedCommandBuilder.builder()
                    .name(name)
                    .description("");
            if (parentName != null)
                builder.description("parent is " + parentName);
            return builder.create();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String resolve(String description, String commandName, String parentName, String programName) {
        return ProcessedCommand.resolveDescription(command(commandName, parentName, programName),
                description, programName, parentName);
    }

    @Test
    public void testPassthroughWithoutVariables() {
        assertNull(resolve(null, "build", "jbang", "jbang"));
        assertEquals("", resolve("", "build", "jbang", "jbang"));
        String plain = "plain text, no dollar at all";
        assertSame(plain, resolve(plain, "build", "jbang", "jbang"));
        String dollarOnly = "costs 5$ or $$";
        assertSame(dollarOnly, resolve(dollarOnly, "build", "jbang", "jbang"));
    }

    @Test
    public void testKnownVariablesResolve() {
        assertEquals("Run build now",
                resolve("Run ${COMMAND-NAME} now", "build", "jbang", "jbang"));
        assertEquals("jbang build",
                resolve("${ROOT-COMMAND-NAME} ${COMMAND-NAME}", "build", "jbang", "jbang"));
        // Adjacent variables with no separator
        assertEquals("buildbuild",
                resolve("${COMMAND-NAME}${COMMAND-NAME}", "build", "jbang", "jbang"));
        assertEquals("jbang build",
                resolve("${COMMAND-FULL-NAME}", "build", "jbang", "jbang"));
        assertEquals("under jbang",
                resolve("under ${PARENT-COMMAND-NAME}", "build", "jbang", "jbang"));
    }

    @Test
    public void testUnknownKeysStayLiteral() {
        assertEquals("x ${UNKNOWN} y",
                resolve("x ${UNKNOWN} y", "build", "jbang", "jbang"));
    }

    @Test
    public void testMalformedReferencesStayLiteral() {
        // Empty braces: the historical pattern required at least one character
        assertEquals("a ${} b", resolve("a ${} b", "build", "jbang", "jbang"));
        // Unclosed reference
        assertEquals("a ${build", resolve("a ${build", "build", "jbang", "jbang"));
        // Opening at the very end
        assertEquals("abc${", resolve("abc${", "build", "jbang", "jbang"));
        // Brace without dollar
        assertEquals("a {build} b", resolve("a {build} b", "build", "jbang", "jbang"));
        // Empty braces followed by a valid variable: only the valid one resolves
        assertEquals("foo${}bar build",
                resolve("foo${}bar ${COMMAND-NAME}", "build", "jbang", "jbang"));
        // An unclosed opening followed by a later complete pair: the greedy
        // key swallows everything up to the first '}', so the whole span
        // stays literal (identical to the historical regex)
        assertEquals("${open ${COMMAND-NAME}",
                resolve("${open ${COMMAND-NAME}", "build", "jbang", "jbang"));
    }

    @Test
    public void testKeysWithBracesOrDollarsStayLiteral() {
        // A key may contain '{' or '$'; it is still unknown, so it stays literal
        assertEquals("${A{B}", resolve("${A{B}", "build", "jbang", "jbang"));
        assertEquals("${$A}", resolve("${$A}", "build", "jbang", "jbang"));
        // Nested-looking reference: the inner text is the key, which is unknown
        assertEquals("${${COMMAND-NAME}}",
                resolve("${${COMMAND-NAME}}", "build", "jbang", "jbang"));
    }

    @Test
    public void testReplacementsAreLiteralText() {
        // A program name containing '$' must be inserted verbatim (the old
        // regex path needed Matcher.quoteReplacement for this)
        assertEquals("my$cli build",
                resolve("${ROOT-COMMAND-NAME} ${COMMAND-NAME}", "build", null, "my$cli"));
        assertEquals("a$b\\d",
                resolve("${COMMAND-NAME}", "a$b\\d", null, "prog"));
    }

    @Test
    public void testOptionDescriptionResolution() {
        try {
            ProcessedCommand<?, ?> cmd = ProcessedCommandBuilder.builder()
                    .name("build")
                    .description("")
                    .create();
            ProcessedOption option = ProcessedOptionBuilder.builder()
                    .name("output")
                    .type(String.class)
                    .description("Output file for ${ROOT-COMMAND-NAME}")
                    .build();
            cmd.addOption(option);
            assertEquals("Output file for myprog",
                    ProcessedCommand.resolveOptionDesc(cmd, option, "myprog", null));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

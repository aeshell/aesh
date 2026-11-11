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
package org.aesh.command.impl.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.aesh.command.parser.CommandLineParserException;

/**
 * Expands {@code @argfile} arguments into the arguments listed in that file,
 * like picocli and the JDK tools do. Only whole arguments are expanded:
 * {@code tool @args.txt} reads the file, {@code --body=@x} stays as it is.
 * Inside files, arguments are whitespace separated, single or double quotes
 * group words, a {@code #} starts a comment line, and {@code @@} escapes a
 * literal {@code @}. A lone {@code @} is kept literally. Files are read as
 * UTF-8, resolved against the working directory, and may reference further
 * files unless that would loop back onto themselves.
 */
public final class ArgfileExpander {

    private ArgfileExpander() {
    }

    /**
     * Replace every {@code @file} word with the tokenized content of that
     * file. Returns the input array untouched when no word starts with
     * {@code @}, so disabled-looking input costs nothing.
     *
     * @param args the pre-tokenized arguments, may be null
     * @return the expanded arguments, or the input when there is nothing to do
     * @throws CommandLineParserException when a file cannot be read or files
     *         reference each other in a circle
     */
    public static String[] expand(String[] args) throws CommandLineParserException {
        if (args == null)
            return null;
        boolean any = false;
        for (String arg : args) {
            if (arg != null && arg.startsWith("@")) {
                any = true;
                break;
            }
        }
        if (!any)
            return args;
        List<String> expanded = new ArrayList<>(args.length);
        expandInto(Arrays.asList(args), expanded, new LinkedHashSet<>());
        return expanded.toArray(new String[0]);
    }

    private static void expandInto(List<String> words, List<String> out, Set<Path> stack)
            throws CommandLineParserException {
        for (String word : words) {
            if (word != null && word.startsWith("@@")) {
                out.add(word.substring(1));
            } else if (word != null && word.length() > 1 && word.startsWith("@")) {
                expandFile(word.substring(1), out, stack);
            } else {
                out.add(word);
            }
        }
    }

    private static void expandFile(String name, List<String> out, Set<Path> stack)
            throws CommandLineParserException {
        Path file = Paths.get(name);
        Path real;
        try {
            if (!Files.isRegularFile(file) || !Files.isReadable(file))
                throw new CommandLineParserException("Argument file not found: '" + name + "'");
            real = file.toRealPath();
        } catch (IOException e) {
            throw new CommandLineParserException("Argument file not found: '" + name + "'", e);
        }
        if (!stack.add(real))
            throw new CommandLineParserException("Circular argument file reference: '" + name + "'");
        try {
            String content;
            try {
                content = new String(Files.readAllBytes(real), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new CommandLineParserException("Cannot read argument file: '" + name + "'", e);
            }
            expandInto(tokenize(content), out, stack);
        } finally {
            stack.remove(real);
        }
    }

    /**
     * Split file content into words: blanks and newlines separate, single or
     * double quotes group (and are stripped), a {@code #} as the first
     * non-blank character drops the line. Backslashes are literal, so Windows
     * paths survive untouched.
     */
    static List<String> tokenize(String content) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inToken = false;
        char quote = 0;
        boolean lineStart = true;
        int i = 0;
        int length = content.length();
        while (i < length) {
            char c = content.charAt(i);
            if (c == '\r') {
                i++;
            } else if (c == '\n') {
                if (inToken) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    inToken = false;
                }
                lineStart = true;
                i++;
            } else if (lineStart) {
                if (c == ' ' || c == '\t') {
                    i++;
                } else if (c == '#') {
                    while (i < length && content.charAt(i) != '\n')
                        i++;
                } else {
                    lineStart = false;
                }
            } else if (quote != 0) {
                if (c == quote)
                    quote = 0;
                else
                    current.append(c);
                inToken = true;
                i++;
            } else if (c == '\'' || c == '"') {
                quote = c;
                inToken = true;
                i++;
            } else if (c == ' ' || c == '\t') {
                tokens.add(current.toString());
                current.setLength(0);
                inToken = false;
                i++;
            } else {
                current.append(c);
                inToken = true;
                i++;
            }
        }
        if (inToken)
            tokens.add(current.toString());
        return tokens;
    }
}

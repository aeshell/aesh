/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
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
package org.aesh.impl.util;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.aesh.command.impl.converter.FileConverter;

import org.aesh.io.Resource;
import org.aesh.readline.util.LoggerUtil;
import org.aesh.readline.util.Parser;
import org.aesh.terminal.utils.Config;

/**
 * Helper class to list possible files during a complete operation.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileLister {

    private final String token;
    private final Resource cwd;

    private static final Logger LOGGER = LoggerUtil.getLogger(FileLister.class.getName());

    public FileLister(String token, Resource cwd) {
        if (token == null)
            throw new IllegalArgumentException("Incoming directory cannot be null");
        if (cwd == null)
            throw new IllegalArgumentException("Current working directory cannot be null");
        this.token = Parser.switchEscapedSpacesToSpacesInWord(token);
        this.cwd = cwd;
    }

    @Override
    public String toString() {
        return "FileLister{"
                + "token='" + token + '\''
                + ", cwd=" + cwd + '}';
    }

    public int findMatchingDirectories(List<String> candidates) {
        int result = getCandidates(token, candidates);
        Collections.sort(candidates);
        postProcess(token, candidates);
        if (candidates.size() == 1) {
            candidates.set(0, escapeQuotes(candidates.get(0)));
        }
        return result;
    }

    private int getCandidates(String buffer, List<String> candidates) {
        //First translate the path
        String translated = FileConverter.translatePath(cwd.getAbsolutePath(), buffer);

        final File f = new File(translated);
        final File dir;
        if (translated.endsWith(File.separator)) {
            dir = f;
        } else {
            dir = f.getParentFile();
        }

        final File[] entries = (dir == null) ? new File[0] : dir.listFiles();
        return matchFiles(buffer, translated, entries, candidates);
    }

    private int matchFiles(String buffer, String translated, File[] entries, List<String> candidates) {
        if (entries == null) {
            return -1;
        }

        boolean isDirectory = false;
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].getAbsolutePath().startsWith(translated)) {
                isDirectory = entries[i].isDirectory();
                if (isDirectory) {
                    candidates.add(entries[i].getName() + File.separator);
                } else {
                    candidates.add(entries[i].getName());
                }
            }
        }
        // inline only the subpath from last File.separator or 0.
        int index = buffer.lastIndexOf(File.separatorChar) + 1;
        return index;
    }

    private static String escapeQuotes(String name) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < name.length(); ++i) {
            char ch = name.charAt(i);
            if (ch == '"') {
                escaped.append('\\');
            }
            escaped.append(ch);
        }
        return escaped.toString();
    }

    void postProcess(String buffer, List<String> candidates) {
        if (candidates.size() == 1) {
            String candidate = candidates.get(0);
            if (!buffer.contains(File.separator)) {
                if (buffer.startsWith("~")) {
                    candidate = "~" + candidate;
                }
            }
            candidates.set(0, candidate);
        } else if (candidates.isEmpty() && Config.isWindows()) {
            if (buffer.length() == 2 && buffer.endsWith(":")) {
                candidates.add(buffer + File.separator);
            }
        }
    }
}

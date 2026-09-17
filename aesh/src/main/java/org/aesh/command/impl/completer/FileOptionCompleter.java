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
package org.aesh.command.impl.completer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.impl.converter.FileConverter;
import org.aesh.impl.util.FileLister;
import org.aesh.io.FileResource;
import org.aesh.io.Resource;
import org.aesh.io.filter.AllResourceFilter;
import org.aesh.io.filter.ResourceFilter;

/**
 * Completes {@link Resource} objects
 *
 * @author Aesh team
 */
public class FileOptionCompleter implements OptionCompleter<CompleterInvocation> {

    private final ResourceFilter filter;

    public FileOptionCompleter() {
        this(new AllResourceFilter());
    }

    public FileOptionCompleter(ResourceFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("A valid filter must be informed");
        }
        this.filter = filter;
    }

    @Override
    public void complete(CompleterInvocation completerInvocation) {
        List<String> candidates = new ArrayList<>();
        int cursor = new FileLister(completerInvocation.getGivenCompleteValue(),
                completerInvocation.getAeshContext().getCurrentWorkingDirectory()).findMatchingDirectories(candidates);
        candidates = filterCandidates(candidates, completerInvocation);
        boolean appendSpace = false;
        if (candidates.size() == 1) {
            if (completerInvocation.getGivenCompleteValue().endsWith(candidates.get(0))) {
                appendSpace = true;
            }
        }
        completerInvocation.addAllCompleterValues(candidates);
        completerInvocation.setOffset(completerInvocation.getGivenCompleteValue().length() - cursor);
        completerInvocation.setAppendSpace(appendSpace);
    }

    private List<String> filterCandidates(List<String> candidates,
            CompleterInvocation completerInvocation) {
        File dir = searchDirectory(completerInvocation.getGivenCompleteValue(),
                completerInvocation.getAeshContext().getCurrentWorkingDirectory());
        if (dir == null)
            return candidates;
        List<String> filtered = new ArrayList<>(candidates.size());
        for (String candidate : candidates) {
            String name = candidate;
            while (name.endsWith(File.separator))
                name = name.substring(0, name.length() - File.separator.length());
            if (filter.accept(new FileResource(new File(dir, name))))
                filtered.add(candidate);
        }
        return filtered;
    }

    private static File searchDirectory(String token, Resource cwd) {
        String translated = FileConverter.translatePath(cwd.getAbsolutePath(), token);
        File base = new File(translated);
        if (translated.endsWith(File.separator))
            return base;
        return base.getParentFile();
    }

    public ResourceFilter getFilter() {
        return filter;
    }
}

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
package org.aesh.command.impl.completer;

import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.impl.util.FileLister;
import org.aesh.io.Resource;
import org.aesh.io.filter.AllResourceFilter;
import org.aesh.io.filter.ResourceFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Completes {@link Resource} objects
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
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
                completerInvocation.getAeshContext().getCurrentWorkingDirectory()).
                findMatchingDirectories(candidates);
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

    public ResourceFilter getFilter() {
        return filter;
    }
}

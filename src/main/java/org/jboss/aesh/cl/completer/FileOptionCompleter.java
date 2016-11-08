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
package org.jboss.aesh.cl.completer;

import org.jboss.aesh.complete.AeshCompleteOperation;
import org.jboss.aesh.console.command.completer.CompleterInvocation;
import org.jboss.aesh.io.filter.AllResourceFilter;
import org.jboss.aesh.io.filter.ResourceFilter;
import org.aesh.readline.completion.CompleteOperation;
import org.jboss.aesh.util.FileLister;

/**
 * Completes {@link org.jboss.aesh.io.Resource} objects
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
    public void complete(CompleterInvocation completerData) {

        CompleteOperation completeOperation =
                new AeshCompleteOperation(completerData.getAeshContext(), completerData.getGivenCompleteValue(), 0);
        if (completerData.getGivenCompleteValue() == null)
            new FileLister("", completerData.getAeshContext().getCurrentWorkingDirectory(), filter)
                    .findMatchingDirectories(completeOperation);
        else
            new FileLister(completerData.getGivenCompleteValue(),
                    completerData.getAeshContext().getCurrentWorkingDirectory(), filter)
                    .findMatchingDirectories(completeOperation);

        if (completeOperation.getCompletionCandidates().size() > 1) {
            completeOperation.removeEscapedSpacesFromCompletionCandidates();
        }

        completerData.setCompleterValuesTerminalString(completeOperation.getCompletionCandidates());
        if (completerData.getGivenCompleteValue() != null && completerData.getCompleterValues().size() == 1) {
            completerData.setAppendSpace(completeOperation.hasAppendSeparator());
        }

        if(completeOperation.doIgnoreOffset())
            completerData.setIgnoreOffset(completeOperation.doIgnoreOffset());

        completerData.setIgnoreStartsWith(true);
    }

    public ResourceFilter getFilter() {
        return filter;
    }
}

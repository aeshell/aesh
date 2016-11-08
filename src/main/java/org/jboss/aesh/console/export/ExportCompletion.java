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
package org.jboss.aesh.console.export;

import org.jboss.aesh.parser.Parser;
import org.aesh.readline.completion.CompleteOperation;
import org.aesh.readline.completion.Completion;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ExportCompletion implements Completion {

    private static final String EXPORT = "export";
    private static final String EXPORT_SPACE = "export ";
    private final ExportManager exportManager;

    public ExportCompletion(ExportManager manager) {
        this.exportManager = manager;
    }

    @Override
    public void complete(CompleteOperation completeOperation) {

        if(completeOperation.getBuffer() == null || completeOperation.getBuffer().length() < 1) {
            completeOperation.addCompletionCandidate(EXPORT);
        }
        else if(EXPORT.startsWith(completeOperation.getBuffer()))
            completeOperation.addCompletionCandidate(EXPORT);
        else if(EXPORT_SPACE.equals(completeOperation.getBuffer()) ||
                EXPORT.equals(completeOperation.getBuffer().trim())) {
            completeOperation.addCompletionCandidates(exportManager.getAllNamesWithEquals());
            completeOperation.setOffset(completeOperation.getCursor());
        }
        else if(completeOperation.getBuffer().startsWith(EXPORT_SPACE)) {
            String word = Parser.findCurrentWordFromCursor(completeOperation.getBuffer(), completeOperation.getCursor());
            if(word.length() > 0) {
                completeOperation.addCompletionCandidates( exportManager.findAllMatchingKeys(word));
                if(Parser.containsNonEscapedDollar(word)) {
                    int index = word.lastIndexOf('$');
                    completeOperation.setOffset(completeOperation.getCursor()-(word.length()-index));
                }
                else
                    completeOperation.setOffset(completeOperation.getCursor()-word.length());
            }
        }
        else if(Parser.containsNonEscapedDollar( completeOperation.getBuffer())) {
            String word = Parser.findCurrentWordFromCursor(completeOperation.getBuffer(), completeOperation.getCursor());
            if(Parser.containsNonEscapedDollar(word)) {
                completeOperation.addCompletionCandidates(exportManager.findAllMatchingKeys(word));
                completeOperation.setOffset(completeOperation.getCursor()-word.length());
            }
        }
    }
}

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
package org.aesh.command.export;

import org.aesh.parser.LineParser;
import org.aesh.readline.completion.CompleteOperation;
import org.aesh.readline.completion.Completion;
import org.aesh.readline.util.Parser;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ExportCompletion implements Completion {

    private static final String EXPORT = "export";
    private static final String EXPORT_SPACE = "export ";
    private final ExportManager exportManager;
    private final LineParser lineParser;

    public ExportCompletion(ExportManager manager) {
        this.exportManager = manager;
        lineParser = new LineParser();
    }

    @Override
    public void complete(CompleteOperation completeOperation) {

        if(EXPORT_SPACE.equals(completeOperation.getBuffer()) ||
                EXPORT.equals(completeOperation.getBuffer().trim())) {
            completeOperation.addCompletionCandidates(exportManager.getAllNamesWithEquals());
            completeOperation.setOffset(completeOperation.getCursor());
        }
        else if(completeOperation.getBuffer().startsWith(EXPORT_SPACE)) {
            String word = lineParser.parseLine(completeOperation.getBuffer(), completeOperation.getCursor()).selectedWord().word();
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
            String word = lineParser.parseLine(completeOperation.getBuffer(), completeOperation.getCursor()).selectedWord().word();
            if(Parser.containsNonEscapedDollar(word)) {
                completeOperation.addCompletionCandidates(exportManager.findAllMatchingKeys(word));
                completeOperation.setOffset(completeOperation.getCursor()-word.length());
            }
        }
    }
}

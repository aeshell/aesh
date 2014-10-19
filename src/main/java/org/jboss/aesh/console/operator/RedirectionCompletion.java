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
package org.jboss.aesh.console.operator;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.util.FileLister;
import org.jboss.aesh.parser.Parser;

/**
 * ControlOperator completor
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class RedirectionCompletion implements Completion {

    @Override
    public void complete(CompleteOperation completeOperation) {

        if(ControlOperatorParser.doStringContainRedirectionNoPipeline(completeOperation.getBuffer())) {
            int redirectPos =  ControlOperatorParser.findLastRedirectionPositionBeforeCursor(
                    completeOperation.getBuffer(), completeOperation.getCursor());

            String word = Parser.findCurrentWordFromCursor(completeOperation.getBuffer().substring(redirectPos, completeOperation.getCursor()), completeOperation.getCursor() - redirectPos);

            completeOperation.setOffset(completeOperation.getCursor());
            FileLister lister = new FileLister(word, completeOperation.getAeshContext().getCurrentWorkingDirectory());
            lister.findMatchingDirectories(completeOperation);
            //if we only have one complete candidate, leave the escaped space be
            if(completeOperation.getCompletionCandidates().size() > 1)
                completeOperation.removeEscapedSpacesFromCompletionCandidates();
            completeOperation.setIgnoreStartsWith(true);
        }
    }
}

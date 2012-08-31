/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.jreadline.console.alias;

import org.jboss.jreadline.complete.CompleteOperation;
import org.jboss.jreadline.complete.Completion;
import org.jboss.jreadline.console.Console;
import org.jboss.jreadline.console.settings.Settings;
import org.jboss.jreadline.util.Parser;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AliasCompletion implements Completion {

    private String completionName = "alias";
    private AliasManager manager;
    public AliasCompletion(AliasManager manager) {
        this.manager = manager;
    }

    @Override
    public void complete(CompleteOperation completeOperation) {
        completeOperation.addCompletionCandidates(manager.findAllMatchingNames(completeOperation.getBuffer().trim()));

        if(completionName.startsWith(completeOperation.getBuffer()))
            completeOperation.addCompletionCandidate(completionName);
        else if(completeOperation.getBuffer().equals("alias ")) {
            completeOperation.addCompletionCandidates(manager.getAllNames());
        }
        else if(completeOperation.getBuffer().startsWith("alias ")) {
            String word = Parser.findWordClosestToCursor(completeOperation.getBuffer(), completeOperation.getCursor());
            completeOperation.addCompletionCandidates(manager.findAllMatchingNames(word));
            completeOperation.setOffset(completeOperation.getCursor()-word.length());

        }
    }

}

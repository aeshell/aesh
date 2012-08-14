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
package org.jboss.jreadline.console;

import org.jboss.jreadline.complete.CompleteOperation;
import org.jboss.jreadline.complete.Completion;
import org.jboss.jreadline.util.FileUtils;
import org.jboss.jreadline.util.Parser;

import java.io.File;

/**
 * Redirection completor
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class RedirectionCompletion implements Completion {

    @Override
    public void complete(CompleteOperation completeOperation) {

        if(completeOperation.getBuffer().contains(">") &&
                completeOperation.getCursor() >= completeOperation.getBuffer().indexOf(">")) {

            String word =
                    Parser.findWordClosestToCursorDividedByRedirectOrPipe(
                            completeOperation.getBuffer(), completeOperation.getCursor());
            completeOperation.addCompletionCandidates(
                    FileUtils.listMatchingDirectories(word,  new File(System.getProperty("user.dir"))));
            completeOperation.setOffset(completeOperation.getCursor());
        }
    }
}

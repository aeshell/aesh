/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.operator;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.util.FileLister;
import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.util.LoggerUtil;

import java.io.File;
import java.util.logging.Logger;

/**
 * ControlOperator completor
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class RedirectionCompletion implements Completion {

    private Logger logger = LoggerUtil.getLogger(this.getClass().getName());

    @Override
    public void complete(CompleteOperation completeOperation) {

        if(ControlOperatorParser.doStringContainRedirectionNoPipeline(completeOperation.getBuffer())) {
            int redirectPos =  ControlOperatorParser.findLastRedirectionPositionBeforeCursor(
                    completeOperation.getBuffer(), completeOperation.getCursor());

            String word = Parser.findWordClosestToCursor(completeOperation.getBuffer().substring(redirectPos, completeOperation.getCursor()), completeOperation.getCursor()-redirectPos);

            completeOperation.setOffset(completeOperation.getCursor());
            FileLister lister = new FileLister(word, new File(System.getProperty("user.dir")));
            lister.findMatchingDirectories(completeOperation);
            logger.info("FILELISTER: "+lister.toString());
            //if we only have one complete candidate, leave the escaped space be
            if(completeOperation.getCompletionCandidates().size() > 1)
                completeOperation.removeEscapedSpacesFromCompletionCandidates();
        }
    }
}

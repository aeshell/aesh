/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.parser;


import java.util.ArrayList;
import java.util.List;

/**
 * Immutable value object that contain a parsed command line.
 * The command line is splitted into words based on white spaces.
 * Escaped whitespaces, single and double quotes are also parsed.
 *
 * This object have not been populated to any Commands yet.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshLine {
    private final String errorMessage;
    private final List<String> words;
    private final ParserStatus status;

    public AeshLine(List<String> words, ParserStatus status, String errorMessage) {
        if(words == null)
            this.words = new ArrayList<>(0);
        else {
            this.words = new ArrayList<>(words.size());
            this.words.addAll(words);
        }

        this.status = status;
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<String> getWords() {
        return words;
    }

    public ParserStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "AeshLine{" +
                "errorMessage='" + errorMessage + '\'' +
                ", words=" + words +
                ", status=" + status +
                '}';
    }
}

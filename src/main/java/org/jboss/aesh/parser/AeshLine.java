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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

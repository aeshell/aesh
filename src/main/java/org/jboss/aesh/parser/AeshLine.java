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
package org.jboss.aesh.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable value object that contain a parsed command line. The command line is split into words based on white spaces.
 * Escaped white spaces, single and double quotes are also parsed.
 *
 * This object have not been populated to any Commands yet.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshLine {

    private final String originalInput;
    private final String errorMessage;
    private final List<String> words;
    private final ParserStatus status;

    public AeshLine(String originalInput, List<String> words, ParserStatus status, String errorMessage) {
        this.originalInput = originalInput;
        this.status = status;
        this.errorMessage = errorMessage;

        if (words == null) {
            this.words = new ArrayList<>(0);
            return;
        }

        this.words = words;
    }

    public String getOriginalInput() {
        return originalInput;
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
            ", originalInput=" + originalInput +
            '}';
    }
}

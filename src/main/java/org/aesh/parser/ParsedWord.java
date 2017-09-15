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
package org.aesh.parser;

/**
 * Representing a "word" parsed by LineParser and used in ParsedLine.
 * A word is a collection of letters separated by space.
 * A word can contain spaces if they are escaped or if the word is wrapped in
 * a single or double quote.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParsedWord {

    private final String word;
    private final int lineIndex;
    private final Status status;

    public ParsedWord(String word, int lineIndex) {
        this.word = word;
        this.lineIndex = lineIndex;
        this.status = Status.OK;
    }

    public ParsedWord(String word, int lineIndex, Status status) {
        this.word = word;
        this.lineIndex = lineIndex;
        this.status = status;
    }

    public int lineIndex() {
        return lineIndex;
    }

    public String word() {
        return word;
    }

    public Status status() {
        return status;
    }

    public enum Status {
        OK, OPEN_BRACKET, OPEN_QUOTE, OPEN_DOUBLE_QUOTE
    }
}

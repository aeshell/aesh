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
package org.aesh.command.impl.parser;

import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.parser.ParsedLine;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CommandLineCompletionParser {
    /**
     * 1. find the "word" connected with cursor
     *   if it starts with '-', we need to check if its a value or name
     * @param line buffer
     * @return ParsedCompleteObject
     */
    ParsedCompleteObject findCompleteObject(String line, int cursor) throws CommandLineParserException;

    void injectValuesAndComplete(ParsedCompleteObject completeObject,
                                 AeshCompleteOperation completeOperation,
                                 InvocationProviders invocationProviders);


    void injectValuesAndComplete(AeshCompleteOperation completeOperation,
                                 InvocationProviders invocationProviders,
                                 ParsedLine line);
}

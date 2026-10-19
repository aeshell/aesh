/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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
package org.aesh.command.metadata;

import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.parser.CommandLineParserException;

/**
 * Provides compile-time generated metadata for a single
 * {@code @MethodCommand}-annotated method, eliminating runtime reflection
 * for parameter scanning and reflective invocation.
 * <p>
 * Unlike {@link CommandMetadataProvider}, the owning object is supplied by
 * the caller at execution time: method owners are plain objects, not
 * {@code Command} implementations, and the framework never instantiates
 * them. Generated implementations execute the method with a direct call.
 *
 * @author Aesh team
 */
public interface MethodCommandMetadataProvider {

    /**
     * @return the class declaring the method
     */
    Class<?> ownerType();

    /**
     * @return the name of the annotated method
     */
    String methodName();

    /**
     * @return the command name as specified in the annotation, without building the full ProcessedCommand
     */
    String commandName();

    /**
     * @return the command aliases as specified in the annotation, or empty array if none
     */
    default String[] commandAliases() {
        return new String[0];
    }

    /**
     * Build the processed command metadata using literal values
     * extracted at compile time, avoiding annotation scanning at runtime.
     *
     * @param owner the caller-supplied object the method is invoked on
     * @return the fully built ProcessedCommand
     * @throws CommandLineParserException if command metadata is invalid
     */
    @SuppressWarnings("rawtypes")
    ProcessedCommand buildProcessedCommand(Object owner) throws CommandLineParserException;

    /**
     * Metadata-only variant for help listings, touching no
     * converters: mirrors {@link CommandMetadataProvider#buildHelpProcessedCommand()}.
     *
     * @return the fully built ProcessedCommand without an attached command
     * @throws CommandLineParserException if command metadata is invalid
     */
    @SuppressWarnings("rawtypes")
    default ProcessedCommand buildHelpProcessedCommand() throws CommandLineParserException {
        throw new UnsupportedOperationException("Metadata-only help is not supported by this provider");
    }
}

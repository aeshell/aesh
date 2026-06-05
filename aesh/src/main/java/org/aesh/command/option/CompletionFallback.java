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
package org.aesh.command.option;

/**
 * Controls what shell completion scripts offer as a fallback when no
 * other completion candidates are available for an option or argument.
 *
 * @author Aesh team
 */
public enum CompletionFallback {
    /**
     * Auto-detect based on field type:
     * {@link #FILES} for String, File, Path, Resource types;
     * {@link #NONE} for enum types and types with custom completers.
     */
    DEFAULT,

    /** Offer file and directory path completion. */
    FILES,

    /** Offer only directory completion (no regular files). */
    DIRECTORIES,

    /** No fallback -- only show candidates from completers. */
    NONE
}

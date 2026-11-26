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
package org.aesh.command.impl.internal;

/**
 * Compile-time option-name lookup returning indexes into a generated command's
 * option list. Implementations must be immutable; each ProcessedCommand keeps
 * its own mutable options.
 */
public interface GeneratedOptionLookup {

    /**
     * Shared empty lookup for generated commands with no named options (#790).
     */
    GeneratedOptionLookup EMPTY = new GeneratedOptionLookup() {
    };

    default int longOptionIndex(String name) {
        return -1;
    }

    default int shortOptionIndex(String name) {
        return -1;
    }

    default int negatedOptionIndex(String name) {
        return -1;
    }

    default int bareOptionIndex(String name) {
        return -1;
    }
}

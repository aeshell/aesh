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
package org.aesh.util.doc;

import java.util.Collections;
import java.util.List;

import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;

/**
 * Shared rule for the {@code Default} shown in generated reference docs.
 * Generated docs never bake in environment-resolved secrets: they show the
 * raw declared expression (e.g. {@code ${env:API_KEY:-}}) instead. The
 * block is rendered only when the owning command asks for default values
 * via {@code showDefaultValues}.
 */
final class DocDefaults {

    private DocDefaults() {
    }

    /**
     * Default values to display for an option or positional, or an empty
     * list when the command does not show defaults.
     *
     * @param command the owning command, carrying the resolved toggle
     * @param option the option or positional
     * @return raw declared values when present, else the resolved ones
     */
    static List<String> displayDefaultValues(ProcessedCommand<?, ?> command, ProcessedOption option) {
        if (command == null || !command.showDefaultValues())
            return Collections.emptyList();
        List<String> raw = option.getRawDefaultValues();
        if (raw != null && !raw.isEmpty())
            return raw;
        List<String> resolved = option.getDefaultValues();
        return resolved != null ? resolved : Collections.<String> emptyList();
    }
}

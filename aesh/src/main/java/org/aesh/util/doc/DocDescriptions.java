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

import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;

/**
 * Single home for the {@code NameContext} to resolver-argument mapping used
 * by every renderer, so command-level and option-level description resolution
 * cannot drift apart between formats again (#777, #778).
 */
final class DocDescriptions {

    private DocDescriptions() {
    }

    /**
     * Resolve {@code ${...}} variables in a command or positional description.
     */
    static String resolveDescription(ProcessedCommand<?, ?> cmd, String raw,
            DocumentationGenerator.NameContext ctx) {
        if (raw == null || raw.isEmpty() || ctx == null)
            return raw;
        return cmd.resolveCommandDescription(raw,
                ctx.commandName,
                ctx.fullName != null ? ctx.fullName : ctx.commandName,
                ctx.rootName,
                ctx.parentName,
                ctx.parentName);
    }

    /**
     * Resolve an option description, appending the {@code Valid values:}
     * suffix for enum options and resolving option tokens like
     * {@code ${DEFAULT-VALUE}}.
     */
    static String resolveOptionDescription(ProcessedCommand<?, ?> cmd, ProcessedOption opt,
            DocumentationGenerator.NameContext ctx) {
        if (ctx == null)
            return opt != null ? opt.description() : null;
        return cmd.resolveOptionDescription(opt,
                ctx.commandName,
                ctx.fullName != null ? ctx.fullName : ctx.commandName,
                ctx.rootName,
                ctx.parentName,
                ctx.parentName);
    }
}

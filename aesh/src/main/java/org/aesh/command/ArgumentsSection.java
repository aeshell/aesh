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
package org.aesh.command;

/**
 * Where positional arguments render in help output: hidden in the positional
 * flow, in their own {@code Arguments:} section first (right after the usage
 * line) or last (after all option groups). Used by
 * {@code @CommandDefinition} so a fleet-wide default and per-group
 * inheritance both work, while any explicit value always wins.
 * <p>
 * Resolution order, applied once when the command is built: an explicit
 * non-{@code INHERIT} value wins; otherwise the nearest ancestor command
 * with a resolved value wins; otherwise the fleet default wins; otherwise
 * the section stays off.
 */
public enum ArgumentsSection {
    /**
     * Not set here: inherit from the parent command, else the fleet default,
     * else off. This is the default for every command.
     */
    INHERIT,
    /** Positional arguments trail the last option section (legacy). */
    OFF,
    /** Their own {@code Arguments:} heading right after the usage line. */
    FIRST,
    /** Their own {@code Arguments:} heading after all option groups. */
    LAST
}

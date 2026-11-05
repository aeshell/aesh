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
 * A three-way switch for command behavior: on, off, or inherit the value
 * from elsewhere. Used by {@code @CommandDefinition} toggles such as
 * {@code collapseSynopsis} so a fleet-wide default (see {@link HelpFeature})
 * and per-group inheritance both work, while any explicit value always wins.
 * <p>
 * Resolution order, applied once when the command is built: an explicit
 * {@code ON} or {@code OFF} wins; otherwise the nearest ancestor command
 * with a resolved value wins; otherwise the fleet default wins; otherwise
 * the behavior is off.
 */
public enum Toggle {
    /**
     * Not set here: inherit from the parent command, else the fleet default,
     * else off. This is the default for every toggle.
     */
    INHERIT,
    /** Force the behavior on, overriding parent and fleet default. */
    ON,
    /** Force the behavior off, overriding parent and fleet default. */
    OFF
}

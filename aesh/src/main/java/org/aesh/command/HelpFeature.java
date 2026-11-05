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
 * Command behaviors that can be switched on fleet-wide with
 * {@code AeshCommandRegistryBuilder.helpDefaults(...)} instead of repeating
 * the annotation on every command. Each constant mirrors a
 * {@code @CommandDefinition} {@link Toggle} attribute of the same name:
 * an explicit {@code ON}/{@code OFF} always wins, {@code INHERIT} falls back
 * to the parent command and then to this set.
 */
public enum HelpFeature {
    /** Mirror of {@code collapseSynopsis}. */
    COLLAPSE_SYNOPSIS,
    /** Mirror of {@code defaultOptionsFirst}. */
    DEFAULT_OPTIONS_FIRST,
    /** Mirror of {@code showDefaultValues}. */
    SHOW_DEFAULT_VALUES,
    /** Mirror of {@code failOnDuplicateOptions}. */
    FAIL_ON_DUPLICATE_OPTIONS
}

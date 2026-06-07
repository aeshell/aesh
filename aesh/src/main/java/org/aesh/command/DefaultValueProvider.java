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

import org.aesh.command.impl.internal.ProcessedOption;

/**
 * Provides dynamic values for command options at different stages of the
 * value resolution chain.
 * <p>
 * Register on a command via {@code @CommandDefinition(defaultValueProvider = MyProvider.class)}.
 * <p>
 * The full resolution order (highest to lowest priority):
 * <ol>
 * <li><b>Explicit user value</b> ({@code --option=value}) — always wins</li>
 * <li><b>{@link #fallbackValue}</b> — when option is present but bare ({@code --option})</li>
 * <li><b>Annotation {@code fallbackValue}</b> — static fallback for bare options</li>
 * <li><b>{@link #defaultValue}</b> — when option is omitted entirely</li>
 * <li><b>Annotation {@code defaultValue}</b> — static default for omitted options</li>
 * <li><b>Field type default</b> (null, 0, false)</li>
 * </ol>
 * <p>
 * At each stage, returning {@code null} means "fall through to the next stage."
 * <p>
 * The provider can use {@link ProcessedOption#name()} and {@link ProcessedOption#parent()}
 * to identify which option is being queried.
 *
 * @author Aesh team
 */
public interface DefaultValueProvider {

    /**
     * Called when the option is NOT specified on the command line at all.
     * Returning {@code null} falls through to the annotation's {@code defaultValue}.
     *
     * @param option the option to provide a default for
     * @return default value string, or null
     * @throws Exception if the provider cannot resolve the default
     */
    String defaultValue(ProcessedOption option) throws Exception;

    /**
     * Called when the option IS specified but without a value (bare flag).
     * For example, {@code --debug} without {@code =value}.
     * <p>
     * Returning {@code null} falls through to the annotation's {@code fallbackValue}.
     * <p>
     * This is useful for options that should resolve their "bare" value from
     * configuration or environment at runtime, rather than using a static
     * annotation value.
     *
     * @param option the option to provide a fallback value for
     * @return fallback value string, or null to use the annotation fallbackValue
     * @throws Exception if the provider cannot resolve the fallback
     */
    default String fallbackValue(ProcessedOption option) throws Exception {
        return null;
    }
}

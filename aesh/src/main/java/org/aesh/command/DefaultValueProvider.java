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
 * Provides dynamic default values for command options at runtime.
 *
 * <p>
 * Register on a command via {@code @CommandDefinition(defaultValueProvider = MyProvider.class)}.
 * The provider is called during command population for any option that was not explicitly set
 * by the user. If the provider returns a non-null value, it takes precedence over the static
 * {@code defaultValue} from the annotation.
 *
 * <p>
 * The provider can use {@link ProcessedOption#name()} and {@link ProcessedOption#parent()}
 * to identify which option is being queried.
 *
 * @author Aesh team
 */
public interface DefaultValueProvider {

    /**
     * Return the default value for the given option, or {@code null} to fall back
     * to the static default from the annotation.
     *
     * @param option the option to provide a default for
     * @return default value string, or null
     * @throws Exception if the provider cannot resolve the default
     */
    String defaultValue(ProcessedOption option) throws Exception;
}

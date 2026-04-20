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

import java.util.List;
import java.util.Map;

/**
 * Provider interface for dynamically adding entries to help output.
 * Implementations can discover external plugins, aliases, or any
 * other commands that should appear in help but aren't statically
 * defined via annotations.
 *
 * <p>
 * Only invoked at help-render time — no startup cost.
 * </p>
 *
 * @author Aesh team
 */
public interface HelpSectionProvider {

    /**
     * Returns additional sections to display in help output.
     * Each map key is a section heading (e.g. "External", "Plugins"),
     * and the value is the list of entries to display under that heading.
     *
     * <p>
     * If a section name matches an existing {@code helpGroup} from
     * statically defined commands, the entries are appended to that group.
     * </p>
     *
     * @return map of section name to help entries, never null
     */
    Map<String, List<HelpEntry>> getAdditionalSections();
}

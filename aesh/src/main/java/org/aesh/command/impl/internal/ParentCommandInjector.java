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

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Injects parent commands selected by the annotated field type. The legacy
 * {@link #accept(Object, Object)} form remains available for callers that have
 * only a single direct parent; runtime paths should use {@link #inject(Object, Function)}.
 */
public interface ParentCommandInjector extends BiConsumer<Object, Object> {

    /**
     * Inject each {@code @ParentCommand} field from the nearest matching parent.
     *
     * @param command child command instance
     * @param parentLookup resolves an ancestor by the field's declared type
     */
    void inject(Object command, Function<Class<?>, Object> parentLookup);
}

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Reusable empty-collection factories for generated command metadata, avoiding
 * per-site {@code invokedynamic} / {@code LambdaMetafactory} overhead on
 * startup (#789).
 */
public enum CollectionFactory implements Supplier<Object> {
    ARRAY_LIST,
    LINKED_LIST,
    HASH_SET,
    LINKED_HASH_SET,
    TREE_SET,
    HASH_MAP,
    LINKED_HASH_MAP,
    TREE_MAP;

    @Override
    public Object get() {
        switch (this) {
            case ARRAY_LIST:
                return new ArrayList<>();
            case LINKED_LIST:
                return new LinkedList<>();
            case HASH_SET:
                return new HashSet<>();
            case LINKED_HASH_SET:
                return new LinkedHashSet<>();
            case TREE_SET:
                return new TreeSet<>();
            case HASH_MAP:
                return new HashMap<>();
            case LINKED_HASH_MAP:
                return new LinkedHashMap<>();
            case TREE_MAP:
                return new TreeMap<>();
            default:
                throw new AssertionError(this);
        }
    }
}

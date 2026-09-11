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
package org.aesh.builtins.pushd;

import java.util.ArrayDeque;
import java.util.Deque;

import org.aesh.io.Resource;

class DirectoryStack {

    private static final DirectoryStack INSTANCE = new DirectoryStack();

    private final Deque<Resource> stack = new ArrayDeque<>();

    static DirectoryStack getInstance() {
        return INSTANCE;
    }

    synchronized void push(Resource directory) {
        stack.push(directory);
    }

    synchronized Resource pop() {
        return stack.poll();
    }

    synchronized boolean isEmpty() {
        return stack.isEmpty();
    }

    synchronized String asString() {
        StringBuilder builder = new StringBuilder();
        for (Resource directory : stack) {
            if (builder.length() > 0)
                builder.append(' ');
            builder.append(directory);
        }
        return builder.toString();
    }

    synchronized void clear() {
        stack.clear();
    }
}

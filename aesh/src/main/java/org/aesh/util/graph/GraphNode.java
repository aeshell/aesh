/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
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
package org.aesh.util.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple DAG node for building directed acyclic graph structures manually.
 * Unlike {@link org.aesh.util.tree.TreeNode}, the same instance can be added
 * as a child of multiple parents, enabling shared dependencies.
 *
 * <pre>
 * GraphNode shared = GraphNode.of("C");
 * GraphNode root = GraphNode.of("Root")
 *         .child(GraphNode.of("A").child(shared))
 *         .child(GraphNode.of("B").child(shared));
 * </pre>
 */
public class GraphNode {

    private final String label;
    private final List<GraphNode> children = new ArrayList<>();

    private GraphNode(String label) {
        this.label = label;
    }

    /**
     * Creates a new graph node with the given label.
     */
    public static GraphNode of(String label) {
        return new GraphNode(label);
    }

    /**
     * Adds an existing node as a child of this node.
     *
     * @return this node for chaining
     */
    public GraphNode child(GraphNode node) {
        children.add(node);
        return this;
    }

    /**
     * Creates and adds a leaf node with the given label.
     *
     * @return this node for chaining
     */
    public GraphNode child(String label) {
        children.add(new GraphNode(label));
        return this;
    }

    /**
     * Returns the label of this node.
     */
    public String label() {
        return label;
    }

    /**
     * Returns an unmodifiable view of this node's children.
     */
    public List<GraphNode> children() {
        return Collections.unmodifiableList(children);
    }
}

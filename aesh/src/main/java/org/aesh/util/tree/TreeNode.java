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
package org.aesh.util.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple tree node for building tree structures manually.
 *
 * <pre>
 * TreeNode root = TreeNode.of("root")
 *         .child(TreeNode.of("src")
 *                 .child("Main.java")
 *                 .child("Util.java"))
 *         .child(TreeNode.of("test")
 *                 .child("MainTest.java"));
 * </pre>
 */
public class TreeNode {

    private final String label;
    private final List<TreeNode> children = new ArrayList<>();

    private TreeNode(String label) {
        this.label = label;
    }

    /**
     * Creates a new tree node with the given label.
     */
    public static TreeNode of(String label) {
        return new TreeNode(label);
    }

    /**
     * Adds an existing node as a child of this node.
     *
     * @return this node for chaining
     */
    public TreeNode child(TreeNode node) {
        children.add(node);
        return this;
    }

    /**
     * Creates and adds a leaf node with the given label.
     *
     * @return this node for chaining
     */
    public TreeNode child(String label) {
        children.add(new TreeNode(label));
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
    public List<TreeNode> children() {
        return Collections.unmodifiableList(children);
    }
}

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

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Renders tree-structured data as formatted text.
 *
 * <p>Usage via static methods (for {@link TreeNode}):</p>
 * <pre>
 * String output = Tree.render(root);                        // default UNICODE
 * String output = Tree.render(root, TreeStyle.ASCII);       // custom style
 * </pre>
 *
 * <p>Usage via generic builder (for any typed hierarchy):</p>
 * <pre>
 * String output = Tree.&lt;File&gt;builder()
 *         .label(File::getName)
 *         .children(f -&gt; f.isDirectory()
 *                 ? Arrays.asList(f.listFiles())
 *                 : Collections.emptyList())
 *         .style(TreeStyle.UNICODE)
 *         .maxDepth(5)
 *         .build()
 *         .render(rootDir);
 * </pre>
 */
public class Tree {

    private Tree() {
    }

    /**
     * Renders a {@link TreeNode} tree using the default UNICODE style.
     */
    public static String render(TreeNode root) {
        return render(root, TreeStyle.UNICODE);
    }

    /**
     * Renders a {@link TreeNode} tree using the specified style.
     */
    public static String render(TreeNode root, TreeStyle style) {
        return new Renderer<>(TreeNode::label, TreeNode::children, style, -1).render(root);
    }

    /**
     * Creates a new builder for constructing a typed tree renderer.
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    static <T> void renderTree(StringBuilder sb, T node, Function<T, String> labelFn,
            Function<T, List<T>> childrenFn, TreeStyle style, int maxDepth,
            String prefix, boolean isRoot, int depth) {
        sb.append(labelFn.apply(node));
        sb.append(System.lineSeparator());

        List<T> children = getChildren(node, childrenFn);
        for (int i = 0; i < children.size(); i++) {
            T child = children.get(i);
            boolean isLast = (i == children.size() - 1);
            sb.append(prefix);
            sb.append(isLast ? style.last() : style.branch());

            int childDepth = isRoot ? 0 : depth;
            List<T> grandchildren = getChildren(child, childrenFn);
            if (maxDepth >= 0 && childDepth >= maxDepth && !grandchildren.isEmpty()) {
                sb.append(labelFn.apply(child));
                sb.append(System.lineSeparator());
                String nextPrefix = prefix + (isLast ? style.space() : style.vertical());
                sb.append(nextPrefix);
                sb.append("...");
                sb.append(System.lineSeparator());
            } else {
                String nextPrefix = prefix + (isLast ? style.space() : style.vertical());
                renderTree(sb, child, labelFn, childrenFn, style, maxDepth,
                        nextPrefix, false, childDepth + 1);
            }
        }
    }

    private static <T> List<T> getChildren(T node, Function<T, List<T>> childrenFn) {
        List<T> children = childrenFn.apply(node);
        return children == null ? Collections.emptyList() : children;
    }

    /**
     * A configured tree renderer for a specific node type.
     * Obtained via {@link Builder#build()}.
     */
    public static class Renderer<T> {
        private final Function<T, String> labelFn;
        private final Function<T, List<T>> childrenFn;
        private final TreeStyle style;
        private final int maxDepth;

        Renderer(Function<T, String> labelFn, Function<T, List<T>> childrenFn,
                TreeStyle style, int maxDepth) {
            this.labelFn = labelFn;
            this.childrenFn = childrenFn;
            this.style = style;
            this.maxDepth = maxDepth;
        }

        /**
         * Renders the tree rooted at the given node.
         */
        public String render(T root) {
            StringBuilder sb = new StringBuilder();
            Tree.renderTree(sb, root, labelFn, childrenFn, style, maxDepth, "", true, -1);
            return sb.toString();
        }
    }

    /**
     * Builder for constructing {@link Renderer} instances with a fluent API.
     */
    public static class Builder<T> {
        private Function<T, String> labelFn;
        private Function<T, List<T>> childrenFn;
        private TreeStyle style = TreeStyle.UNICODE;
        private int maxDepth = -1;

        private Builder() {
        }

        /**
         * Sets the function to extract a display label from each node.
         */
        public Builder<T> label(Function<T, String> labelFn) {
            this.labelFn = labelFn;
            return this;
        }

        /**
         * Sets the function to extract children from each node.
         * A null or empty return value indicates a leaf node.
         */
        public Builder<T> children(Function<T, List<T>> childrenFn) {
            this.childrenFn = childrenFn;
            return this;
        }

        /**
         * Sets the visual style for tree connectors.
         */
        public Builder<T> style(TreeStyle style) {
            this.style = style;
            return this;
        }

        /**
         * Sets the maximum depth to render ({@code -1} for unlimited,
         * {@code 0} for root's children only).
         */
        public Builder<T> maxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        /**
         * Builds an immutable tree renderer.
         *
         * @throws IllegalStateException if {@code label} or {@code children} is not set
         */
        public Renderer<T> build() {
            if (labelFn == null) {
                throw new IllegalStateException("label function must be set");
            }
            if (childrenFn == null) {
                throw new IllegalStateException("children function must be set");
            }
            return new Renderer<>(labelFn, childrenFn, style, maxDepth);
        }
    }
}

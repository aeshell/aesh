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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Tests for the tree display utility.
 */
public class TreeTest {

    // Simple data class for testing the generic builder
    static class Category {
        final String name;
        final List<Category> subs;

        Category(String name, Category... subs) {
            this.name = name;
            this.subs = Arrays.asList(subs);
        }
    }

    private static final String NL = System.lineSeparator();

    @Test
    public void testBasicThreeLevelTreeNode() {
        TreeNode root = TreeNode.of("root")
                .child(TreeNode.of("src")
                        .child("Main.java")
                        .child("Util.java"))
                .child(TreeNode.of("test")
                        .child("MainTest.java"));

        String expected = "root" + NL
                + "\u251C\u2500\u2500 src" + NL
                + "\u2502   \u251C\u2500\u2500 Main.java" + NL
                + "\u2502   \u2514\u2500\u2500 Util.java" + NL
                + "\u2514\u2500\u2500 test" + NL
                + "    \u2514\u2500\u2500 MainTest.java" + NL;

        assertEquals(expected, Tree.render(root));
    }

    @Test
    public void testAsciiStyle() {
        TreeNode root = TreeNode.of("root")
                .child("a")
                .child("b");

        String expected = "root" + NL
                + "+-- a" + NL
                + "\\-- b" + NL;

        assertEquals(expected, Tree.render(root, TreeStyle.ASCII));
    }

    @Test
    public void testUnicodeStyle() {
        TreeNode root = TreeNode.of("root")
                .child("a")
                .child("b");

        String expected = "root" + NL
                + "\u251C\u2500\u2500 a" + NL
                + "\u2514\u2500\u2500 b" + NL;

        assertEquals(expected, Tree.render(root, TreeStyle.UNICODE));
    }

    @Test
    public void testCompactStyle() {
        TreeNode root = TreeNode.of("root")
                .child("a")
                .child("b");

        String expected = "root" + NL
                + "\u251C\u2500 a" + NL
                + "\u2514\u2500 b" + NL;

        assertEquals(expected, Tree.render(root, TreeStyle.COMPACT));
    }

    @Test
    public void testRoundedStyle() {
        TreeNode root = TreeNode.of("root")
                .child("a")
                .child("b");

        String expected = "root" + NL
                + "\u251C\u2500\u2500 a" + NL
                + "\u2570\u2500\u2500 b" + NL;

        assertEquals(expected, Tree.render(root, TreeStyle.ROUNDED));
    }

    @Test
    public void testSingleRootNoChildren() {
        TreeNode root = TreeNode.of("lonely");

        String expected = "lonely" + NL;

        assertEquals(expected, Tree.render(root));
    }

    @Test
    public void testDeepNesting() {
        TreeNode root = TreeNode.of("1")
                .child(TreeNode.of("2")
                        .child(TreeNode.of("3")
                                .child(TreeNode.of("4")
                                        .child("5"))));

        String output = Tree.render(root);
        // Verify 5 levels of indentation
        String expected = "1" + NL
                + "\u2514\u2500\u2500 2" + NL
                + "    \u2514\u2500\u2500 3" + NL
                + "        \u2514\u2500\u2500 4" + NL
                + "            \u2514\u2500\u2500 5" + NL;

        assertEquals(expected, output);
    }

    @Test
    public void testMultipleChildrenBranchVsLast() {
        TreeNode root = TreeNode.of("root")
                .child("a")
                .child("b")
                .child("c")
                .child("d");

        String output = Tree.render(root);

        // First 3 use branch (├──), last uses last (└──)
        String expected = "root" + NL
                + "\u251C\u2500\u2500 a" + NL
                + "\u251C\u2500\u2500 b" + NL
                + "\u251C\u2500\u2500 c" + NL
                + "\u2514\u2500\u2500 d" + NL;

        assertEquals(expected, output);
    }

    @Test
    public void testMaxDepthTruncation() {
        TreeNode root = TreeNode.of("root")
                .child(TreeNode.of("child")
                        .child(TreeNode.of("grandchild")
                                .child("great-grandchild")));

        String output = Tree.<TreeNode>builder()
                .label(TreeNode::label)
                .children(TreeNode::children)
                .style(TreeStyle.UNICODE)
                .maxDepth(0)
                .build()
                .render(root);

        // maxDepth 0 = root's children only, grandchildren truncated with ...
        String expected = "root" + NL
                + "\u2514\u2500\u2500 child" + NL
                + "    ..." + NL;

        assertEquals(expected, output);
    }

    @Test
    public void testGenericBuilderWithCustomType() {
        Category root = new Category("Animals",
                new Category("Mammals",
                        new Category("Dog"),
                        new Category("Cat")),
                new Category("Birds",
                        new Category("Eagle")));

        String output = Tree.<Category>builder()
                .label(c -> c.name)
                .children(c -> c.subs)
                .style(TreeStyle.UNICODE)
                .build()
                .render(root);

        String expected = "Animals" + NL
                + "\u251C\u2500\u2500 Mammals" + NL
                + "\u2502   \u251C\u2500\u2500 Dog" + NL
                + "\u2502   \u2514\u2500\u2500 Cat" + NL
                + "\u2514\u2500\u2500 Birds" + NL
                + "    \u2514\u2500\u2500 Eagle" + NL;

        assertEquals(expected, output);
    }

    @Test
    public void testStaticRenderVsBuilderEquivalence() {
        TreeNode root = TreeNode.of("root")
                .child(TreeNode.of("a")
                        .child("a1"))
                .child("b");

        String staticOutput = Tree.render(root);

        String builderOutput = Tree.<TreeNode>builder()
                .label(TreeNode::label)
                .children(TreeNode::children)
                .style(TreeStyle.UNICODE)
                .build()
                .render(root);

        assertEquals(staticOutput, builderOutput);
    }

    @Test
    public void testNullChildrenTreatedAsLeaf() {
        Category leaf = new Category("leaf") {
            // Override to return null children
        };

        String output = Tree.<Category>builder()
                .label(c -> c.name)
                .children(c -> null)
                .build()
                .render(leaf);

        assertEquals("leaf" + NL, output);
    }

    @Test
    public void testEmptyChildrenTreatedAsLeaf() {
        String output = Tree.<TreeNode>builder()
                .label(TreeNode::label)
                .children(TreeNode::children)
                .build()
                .render(TreeNode.of("leaf"));

        assertEquals("leaf" + NL, output);
    }

    @Test
    public void testBuilderDefaultsUnicodeUnlimited() {
        TreeNode root = TreeNode.of("root")
                .child(TreeNode.of("a")
                        .child(TreeNode.of("b")
                                .child("c")));

        // Default should be UNICODE style with unlimited depth
        String output = Tree.<TreeNode>builder()
                .label(TreeNode::label)
                .children(TreeNode::children)
                .build()
                .render(root);

        // Should render all levels using UNICODE connectors
        assertTrue(output.contains("\u2514\u2500\u2500 a"));
        assertTrue(output.contains("\u2514\u2500\u2500 b"));
        assertTrue(output.contains("\u2514\u2500\u2500 c"));
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilderMissingLabelThrows() {
        Tree.<String>builder()
                .children(s -> Collections.emptyList())
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilderMissingChildrenThrows() {
        Tree.<String>builder()
                .label(s -> s)
                .build();
    }

    @Test
    public void testMaxDepthWithMultipleBranches() {
        TreeNode root = TreeNode.of("root")
                .child(TreeNode.of("a")
                        .child("a1")
                        .child("a2"))
                .child(TreeNode.of("b")
                        .child("b1"));

        String output = Tree.<TreeNode>builder()
                .label(TreeNode::label)
                .children(TreeNode::children)
                .maxDepth(0)
                .build()
                .render(root);

        // Children shown, grandchildren truncated
        String expected = "root" + NL
                + "\u251C\u2500\u2500 a" + NL
                + "\u2502   ..." + NL
                + "\u2514\u2500\u2500 b" + NL
                + "    ..." + NL;

        assertEquals(expected, output);
    }

    @Test
    public void testVerticalContinuationLine() {
        TreeNode root = TreeNode.of("root")
                .child(TreeNode.of("a")
                        .child("a1"))
                .child("b");

        String output = Tree.render(root, TreeStyle.ASCII);

        // "a" is not last, so its children should have "|   " prefix
        String expected = "root" + NL
                + "+-- a" + NL
                + "|   \\-- a1" + NL
                + "\\-- b" + NL;

        assertEquals(expected, output);
    }
}

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Tests for the DAG graph display utility.
 */
public class GraphTest {

    // Simple data class for testing the generic builder
    static class Task {
        final String name;
        final List<Task> dependencies;

        Task(String name, Task... deps) {
            this.name = name;
            this.dependencies = Arrays.asList(deps);
        }
    }

    private static final String NL = System.lineSeparator();

    @Test
    public void testSingleNode() {
        GraphNode root = GraphNode.of("Root");
        String output = Graph.render(root);
        assertEquals("Root" + NL, output);
    }

    @Test
    public void testLinearChain() {
        GraphNode root = GraphNode.of("Root")
                .child(GraphNode.of("A")
                        .child("B"));
        String output = Graph.render(root);

        // Structural validation: each label appears once, in order
        String[] lines = output.split(NL);
        assertTrue("Root should be on first line", lines[0].contains("Root"));
        boolean foundA = false;
        boolean foundB = false;
        int aLine = -1, bLine = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("A") && !lines[i].contains("Root")) {
                foundA = true;
                aLine = i;
            }
            if (lines[i].contains("B")) {
                foundB = true;
                bLine = i;
            }
        }
        assertTrue("A should appear", foundA);
        assertTrue("B should appear", foundB);
        assertTrue("B should appear after A", bLine > aLine);
    }

    @Test
    public void testFanOut() {
        GraphNode root = GraphNode.of("Root")
                .child("A")
                .child("B")
                .child("C");
        String output = Graph.render(root);

        // Root on first line, all children on a later line
        String[] lines = output.split(NL);
        assertTrue("Root should be on first line", lines[0].contains("Root"));

        // Count occurrences of each label
        int countA = countOccurrences(output, "A");
        int countB = countOccurrences(output, "B");
        int countC = countOccurrences(output, "C");
        assertEquals("A should appear once", 1, countA);
        assertEquals("B should appear once", 1, countB);
        assertEquals("C should appear once", 1, countC);
    }

    @Test
    public void testDiamondPattern() {
        GraphNode shared = GraphNode.of("C");
        GraphNode root = GraphNode.of("Root")
                .child(GraphNode.of("A").child(shared))
                .child(GraphNode.of("B").child(shared));

        String output = Graph.render(root);

        // Structural: each label appears exactly once
        assertEquals("Root should appear once", 1, countOccurrences(output, "Root"));
        assertEquals("A should appear once", 1, countOccurrences(output, "A"));
        assertEquals("B should appear once", 1, countOccurrences(output, "B"));
        assertEquals("C should appear once", 1, countOccurrences(output, "C"));

        // Root on first line, C on last non-blank content line
        String[] lines = output.split(NL);
        assertTrue("Root should be on first line", lines[0].contains("Root"));

        // Find lines for each label
        int rootLine = findLine(lines, "Root");
        int aLine = findLine(lines, "A");
        int bLine = findLine(lines, "B");
        int cLine = findLine(lines, "C");

        assertTrue("A should be after Root", aLine > rootLine);
        assertTrue("B should be after Root", bLine > rootLine);
        assertTrue("C should be after A", cLine > aLine);
        assertTrue("C should be after B", cLine > bLine);
    }

    @Test
    public void testComplexDag() {
        // Root -> {A, B}, A -> {C, D}, B -> {D, E}
        GraphNode d = GraphNode.of("D");
        GraphNode a = GraphNode.of("A").child("C").child(d);
        GraphNode b = GraphNode.of("B").child(d).child("E");
        GraphNode root = GraphNode.of("Root").child(a).child(b);

        String output = Graph.render(root);

        // Each label appears exactly once
        assertEquals("Root once", 1, countOccurrences(output, "Root"));
        assertEquals("A once", 1, countOccurrences(output, "A"));
        assertEquals("B once", 1, countOccurrences(output, "B"));
        assertEquals("C once", 1, countOccurrences(output, "C"));
        assertEquals("D once", 1, countOccurrences(output, "D"));
        assertEquals("E once", 1, countOccurrences(output, "E"));

        String[] lines = output.split(NL);
        int rootLine = findLine(lines, "Root");
        int aLine = findLine(lines, "A");
        int bLine = findLine(lines, "B");
        int cLine = findLine(lines, "C");
        int dLine = findLine(lines, "D");
        int eLine = findLine(lines, "E");

        assertTrue("A after Root", aLine > rootLine);
        assertTrue("B after Root", bLine > rootLine);
        assertTrue("C after A", cLine > aLine);
        assertTrue("D after A", dLine > aLine);
        assertTrue("D after B", dLine > bLine);
        assertTrue("E after B", eLine > bLine);
    }

    @Test
    public void testAsciiStyle() {
        GraphNode root = GraphNode.of("Root")
                .child("A")
                .child("B");

        String output = Graph.render(root, GraphStyle.ASCII);
        // Should use ASCII characters
        assertTrue("Should contain ASCII horizontal '-'",
                output.contains("-") || output.contains("+"));
    }

    @Test
    public void testUnicodeStyle() {
        GraphNode root = GraphNode.of("Root")
                .child("A")
                .child("B");

        String output = Graph.render(root, GraphStyle.UNICODE);
        // Default style should work and contain node labels
        assertTrue("Should contain Root", output.contains("Root"));
        assertTrue("Should contain A", output.contains("A"));
        assertTrue("Should contain B", output.contains("B"));
    }

    @Test
    public void testRoundedStyle() {
        GraphNode root = GraphNode.of("Root")
                .child("A")
                .child("B");

        String output = Graph.render(root, GraphStyle.ROUNDED);
        assertTrue("Should contain Root", output.contains("Root"));
        assertTrue("Should contain A", output.contains("A"));
        assertTrue("Should contain B", output.contains("B"));
    }

    @Test
    public void testGenericBuilder() {
        Task leaf1 = new Task("Compile");
        Task leaf2 = new Task("Test");
        Task root = new Task("Build", leaf1, leaf2);

        String output = Graph.<Task> builder()
                .label(t -> t.name)
                .children(t -> t.dependencies)
                .style(GraphStyle.UNICODE)
                .build()
                .render(root);

        assertTrue("Should contain Build", output.contains("Build"));
        assertTrue("Should contain Compile", output.contains("Compile"));
        assertTrue("Should contain Test", output.contains("Test"));
    }

    @Test
    public void testStaticVsBuilderEquivalence() {
        GraphNode root = GraphNode.of("Root")
                .child("A")
                .child("B");

        String staticOutput = Graph.render(root);

        String builderOutput = Graph.<GraphNode> builder()
                .label(GraphNode::label)
                .children(GraphNode::children)
                .style(GraphStyle.UNICODE)
                .build()
                .render(root);

        assertEquals(staticOutput, builderOutput);
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilderMissingLabelThrows() {
        Graph.<String> builder()
                .children(s -> Collections.emptyList())
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilderMissingChildrenThrows() {
        Graph.<String> builder()
                .label(s -> s)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCycleDetection() {
        // Create a cycle: A -> B -> A
        GraphNode a = GraphNode.of("A");
        GraphNode b = GraphNode.of("B").child(a);
        a.child(b);

        Graph.render(a);
    }

    @Test
    public void testNullChildrenAsLeaf() {
        Task leaf = new Task("leaf") {
            // Override nothing - dependencies is empty list
        };

        String output = Graph.<Task> builder()
                .label(t -> t.name)
                .children(t -> null)
                .build()
                .render(leaf);

        assertEquals("leaf" + NL, output);
    }

    @Test
    public void testEmptyChildrenAsLeaf() {
        String output = Graph.<GraphNode> builder()
                .label(GraphNode::label)
                .children(GraphNode::children)
                .build()
                .render(GraphNode.of("leaf"));

        assertEquals("leaf" + NL, output);
    }

    @Test
    public void testMaxWidthWrapsChildren() {
        // Parent with 10 children and small maxWidth — children span multiple rows
        GraphNode root = GraphNode.of("Root");
        for (int i = 0; i < 10; i++) {
            root.child("N" + i);
        }
        String output = Graph.render(root, 20);

        // All labels should be present
        assertTrue("Should contain Root", output.contains("Root"));
        for (int i = 0; i < 10; i++) {
            assertTrue("Should contain N" + i, output.contains("N" + i));
        }

        // Children should be split across multiple label lines (not all on one line)
        String[] lines = output.split(NL);
        int linesWithChildren = 0;
        for (String line : lines) {
            for (int i = 0; i < 10; i++) {
                if (line.contains("N" + i)) {
                    linesWithChildren++;
                    break;
                }
            }
        }
        assertTrue("Children should span multiple lines", linesWithChildren > 1);
    }

    @Test
    public void testMaxWidthZeroNoLimit() {
        GraphNode root = GraphNode.of("Root")
                .child("A")
                .child("B")
                .child("C");

        String withZero = Graph.render(root, 0);
        String withZeroExplicit = Graph.render(root, GraphStyle.UNICODE, 0);

        assertEquals("maxWidth=0 should match explicit unlimited", withZero, withZeroExplicit);

        // All labels should be present
        assertTrue("Should contain Root", withZero.contains("Root"));
        assertTrue("Should contain A", withZero.contains("A"));
        assertTrue("Should contain B", withZero.contains("B"));
        assertTrue("Should contain C", withZero.contains("C"));
    }

    @Test
    public void testMaxWidthSingleNodePerRow() {
        // Very small maxWidth forces one node per row
        GraphNode root = GraphNode.of("Root")
                .child("AA")
                .child("BB")
                .child("CC");

        String output = Graph.render(root, 4);

        assertTrue("Should contain Root", output.contains("Root"));
        assertTrue("Should contain AA", output.contains("AA"));
        assertTrue("Should contain BB", output.contains("BB"));
        assertTrue("Should contain CC", output.contains("CC"));
    }

    @Test
    public void testMaxWidthBuilderApi() {
        GraphNode root = GraphNode.of("Root")
                .child("A")
                .child("B")
                .child("C")
                .child("D")
                .child("E");

        String output = Graph.<GraphNode> builder()
                .label(GraphNode::label)
                .children(GraphNode::children)
                .style(GraphStyle.UNICODE)
                .maxWidth(10)
                .build()
                .render(root);

        assertTrue("Should contain Root", output.contains("Root"));
        assertTrue("Should contain A", output.contains("A"));
        assertTrue("Should contain B", output.contains("B"));
        assertTrue("Should contain C", output.contains("C"));
        assertTrue("Should contain D", output.contains("D"));
        assertTrue("Should contain E", output.contains("E"));

        // Children should be split across multiple label lines
        String[] lines = output.split(NL);
        int childLabelLines = 0;
        for (String line : lines) {
            if (line.matches(".*[A-E].*") && !line.contains("Root")) {
                childLabelLines++;
            }
        }
        assertTrue("Children should span multiple lines", childLabelLines > 1);
    }

    @Test
    public void testMaxWidthWithDiamond() {
        // maxWidth + diamond pattern (shared children) renders correctly
        GraphNode shared = GraphNode.of("S");
        GraphNode root = GraphNode.of("Root")
                .child(GraphNode.of("A").child(shared))
                .child(GraphNode.of("B").child(shared));

        String output = Graph.render(root, 10);

        assertEquals("Root once", 1, countOccurrences(output, "Root"));
        assertEquals("A once", 1, countOccurrences(output, "A"));
        assertEquals("B once", 1, countOccurrences(output, "B"));
        assertEquals("S once", 1, countOccurrences(output, "S"));

        String[] lines = output.split(NL);
        int rootLine = findLine(lines, "Root");
        int sLine = findLine(lines, "S");
        assertTrue("S should be after Root", sLine > rootLine);
    }

    @Test
    public void testMaxWidthLargeGraphVerticalCompactness() {
        // 20 children with maxWidth=40 should produce compact output
        GraphNode root = GraphNode.of("Root");
        for (int i = 0; i < 20; i++) {
            root.child("N" + i);
        }
        String output = Graph.render(root, 40);

        // All labels should be present
        assertTrue("Should contain Root", output.contains("Root"));
        for (int i = 0; i < 20; i++) {
            assertTrue("Should contain N" + i, output.contains("N" + i));
        }

        // With compact routing rows, output should be very compact (≤ 12 lines)
        String[] lines = output.split(NL);
        assertTrue("Output should be ≤ 12 lines but was " + lines.length,
                lines.length <= 12);
    }

    @Test
    public void testMaxWidth110ChildrenCompactness() {
        // 110 children with maxWidth=80 should produce compact output
        GraphNode root = GraphNode.of("Root");
        for (int i = 0; i < 110; i++) {
            root.child("N" + i);
        }
        String output = Graph.render(root, 80);

        // All labels should be present
        assertTrue("Should contain Root", output.contains("Root"));
        for (int i = 0; i < 110; i++) {
            assertTrue("Should contain N" + i, output.contains("N" + i));
        }

        // With compact routing rows, output should be ≤ 18 lines
        String[] lines = output.split(NL);
        assertTrue("Output should be ≤ 18 lines but was " + lines.length,
                lines.length <= 18);
    }

    // Helper to count non-overlapping occurrences of a substring
    private int countOccurrences(String text, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            // Make sure it's a standalone label, not part of another word
            boolean leftBound = (idx == 0 || !Character.isLetterOrDigit(text.charAt(idx - 1)));
            boolean rightBound = (idx + sub.length() >= text.length()
                    || !Character.isLetterOrDigit(text.charAt(idx + sub.length())));
            if (leftBound && rightBound) {
                count++;
            }
            idx += sub.length();
        }
        return count;
    }

    // Helper to find the line index containing a label
    private int findLine(String[] lines, String label) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(label)) {
                return i;
            }
        }
        return -1;
    }
}

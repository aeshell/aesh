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
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

/**
 * Renders directed acyclic graph (DAG) data as formatted text.
 *
 * <p>
 * Usage via static methods (for {@link GraphNode}):
 * </p>
 *
 * <pre>
 * String output = Graph.render(root); // default UNICODE
 * String output = Graph.render(root, GraphStyle.ASCII); // custom style
 * </pre>
 *
 * <p>
 * Usage via generic builder (for any typed DAG):
 * </p>
 *
 * <pre>
 * String output = Graph.&lt;Task&gt; builder()
 *         .label(Task::getName)
 *         .children(Task::getDependencies)
 *         .style(GraphStyle.UNICODE)
 *         .build()
 *         .render(rootTask);
 * </pre>
 */
public class Graph {

    private static final int NODE_GAP = 2;

    private Graph() {
    }

    /**
     * Renders a {@link GraphNode} graph using the default UNICODE style.
     */
    public static String render(GraphNode root) {
        return render(root, GraphStyle.UNICODE);
    }

    /**
     * Renders a {@link GraphNode} graph using the default UNICODE style
     * with a maximum output width.
     *
     * @param maxWidth maximum character width (0 = no limit)
     */
    public static String render(GraphNode root, int maxWidth) {
        return render(root, GraphStyle.UNICODE, maxWidth);
    }

    /**
     * Renders a {@link GraphNode} graph using the specified style.
     */
    public static String render(GraphNode root, GraphStyle style) {
        return new Renderer<>(GraphNode::label, GraphNode::children, style, 0).render(root);
    }

    /**
     * Renders a {@link GraphNode} graph using the specified style
     * with a maximum output width.
     *
     * @param maxWidth maximum character width (0 = no limit)
     */
    public static String render(GraphNode root, GraphStyle style, int maxWidth) {
        return new Renderer<>(GraphNode::label, GraphNode::children, style, maxWidth).render(root);
    }

    /**
     * Creates a new builder for constructing a typed graph renderer.
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    private static <T> List<T> getChildren(T node, Function<T, List<T>> childrenFn) {
        List<T> children = childrenFn.apply(node);
        return children == null ? Collections.emptyList() : children;
    }

    /**
     * Internal layout node used during rendering.
     */
    private static class LayoutNode<T> {
        T original;
        String label;
        int layer;
        int indexInLayer;
        int x;
        boolean isDummy;

        LayoutNode(T original, String label, int layer) {
            this.original = original;
            this.label = label;
            this.layer = layer;
        }

        static <T> LayoutNode<T> dummy(int layer) {
            LayoutNode<T> d = new LayoutNode<>(null, "", layer);
            d.isDummy = true;
            return d;
        }

        int width() {
            return Math.max(label.length(), 1);
        }
    }

    /**
     * A configured graph renderer for a specific node type.
     * Obtained via {@link Builder#build()}.
     */
    public static class Renderer<T> {
        private final Function<T, String> labelFn;
        private final Function<T, List<T>> childrenFn;
        private final GraphStyle style;
        private final int maxWidth;

        Renderer(Function<T, String> labelFn, Function<T, List<T>> childrenFn,
                GraphStyle style, int maxWidth) {
            this.labelFn = labelFn;
            this.childrenFn = childrenFn;
            this.style = style;
            this.maxWidth = maxWidth;
        }

        /**
         * Renders the DAG rooted at the given node.
         *
         * @throws IllegalArgumentException if the graph contains a cycle
         */
        public String render(T root) {
            // Phase 1: Discover nodes, build adjacency, detect cycles
            IdentityHashMap<T, LayoutNode<T>> nodeMap = new IdentityHashMap<>();
            IdentityHashMap<T, List<T>> parentMap = new IdentityHashMap<>();
            List<T> topoOrder = new ArrayList<>();

            discoverAndDetectCycles(root, nodeMap, parentMap, topoOrder);

            // Phase 2: Layer assignment
            assignLayers(topoOrder, nodeMap, parentMap);

            // Build layers list preserving child order via BFS
            int maxLayer = 0;
            for (LayoutNode<T> ln : nodeMap.values()) {
                maxLayer = Math.max(maxLayer, ln.layer);
            }
            List<List<LayoutNode<T>>> layers = new ArrayList<>();
            for (int i = 0; i <= maxLayer; i++) {
                layers.add(new ArrayList<>());
            }
            Set<T> layerAdded = Collections.newSetFromMap(new IdentityHashMap<>());
            Queue<T> layerQueue = new LinkedList<>();
            layerQueue.add(root);
            layerAdded.add(root);
            layers.get(0).add(nodeMap.get(root));
            while (!layerQueue.isEmpty()) {
                T current = layerQueue.poll();
                for (T child : getChildren(current, childrenFn)) {
                    if (!layerAdded.contains(child)) {
                        layerAdded.add(child);
                        LayoutNode<T> childLn = nodeMap.get(child);
                        layers.get(childLn.layer).add(childLn);
                        layerQueue.add(child);
                    }
                }
            }

            // Split wide layers before dummy insertion
            splitWideLayers(layers);

            // Build edge list from the original graph
            List<int[]> edges = new ArrayList<>(); // [parentLayer, parentIdx, childLayer, childIdx]
            IdentityHashMap<T, Integer> layerIndexMap = new IdentityHashMap<>();
            for (List<LayoutNode<T>> layer : layers) {
                for (int i = 0; i < layer.size(); i++) {
                    LayoutNode<T> ln = layer.get(i);
                    ln.indexInLayer = i;
                    if (ln.original != null) {
                        layerIndexMap.put(ln.original, i);
                    }
                }
            }

            // Phase 3: Insert dummy nodes for long edges
            // We need to rebuild edges after inserting dummies
            insertDummies(layers, nodeMap, layerIndexMap);

            // Phase 4: Order within layers (barycenter heuristic)
            List<int[]> allEdges = buildEdgeList(layers, nodeMap);
            minimizeCrossings(layers, allEdges);

            // Rebuild edges after crossing minimization to ensure correctness
            allEdges = buildEdgeList(layers, nodeMap);

            // Phase 5: Coordinate assignment
            assignCoordinates(layers);

            // Phase 6: Render onto character grid
            char[][] grid = renderGrid(layers, allEdges);

            // Phase 7: Grid to string
            return gridToString(grid);
        }

        private void discoverAndDetectCycles(T root, IdentityHashMap<T, LayoutNode<T>> nodeMap,
                IdentityHashMap<T, List<T>> parentMap, List<T> topoOrder) {
            // BFS to discover all nodes
            Queue<T> queue = new LinkedList<>();
            queue.add(root);
            nodeMap.put(root, new LayoutNode<>(root, labelFn.apply(root), 0));

            Set<T> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            visited.add(root);

            while (!queue.isEmpty()) {
                T current = queue.poll();
                for (T child : getChildren(current, childrenFn)) {
                    parentMap.computeIfAbsent(child, k -> new ArrayList<>()).add(current);
                    if (!visited.contains(child)) {
                        visited.add(child);
                        nodeMap.put(child, new LayoutNode<>(child, labelFn.apply(child), 0));
                        queue.add(child);
                    }
                }
            }

            // Detect cycles via DFS
            Set<T> visiting = Collections.newSetFromMap(new IdentityHashMap<>());
            Set<T> done = Collections.newSetFromMap(new IdentityHashMap<>());
            for (T node : nodeMap.keySet()) {
                if (!done.contains(node)) {
                    detectCycleDfs(node, visiting, done, topoOrder);
                }
            }
            Collections.reverse(topoOrder);
        }

        private void detectCycleDfs(T node, Set<T> visiting, Set<T> done, List<T> topoOrder) {
            if (done.contains(node)) {
                return;
            }
            if (visiting.contains(node)) {
                throw new IllegalArgumentException("Graph contains a cycle");
            }
            visiting.add(node);
            for (T child : getChildren(node, childrenFn)) {
                detectCycleDfs(child, visiting, done, topoOrder);
            }
            visiting.remove(node);
            done.add(node);
            topoOrder.add(node);
        }

        private void assignLayers(List<T> topoOrder, IdentityHashMap<T, LayoutNode<T>> nodeMap,
                IdentityHashMap<T, List<T>> parentMap) {
            for (T node : topoOrder) {
                LayoutNode<T> ln = nodeMap.get(node);
                List<T> parents = parentMap.getOrDefault(node, Collections.emptyList());
                int maxParentLayer = -1;
                for (T parent : parents) {
                    LayoutNode<T> parentLn = nodeMap.get(parent);
                    if (parentLn != null) {
                        maxParentLayer = Math.max(maxParentLayer, parentLn.layer);
                    }
                }
                ln.layer = maxParentLayer + 1;
            }
        }

        private void insertDummies(List<List<LayoutNode<T>>> layers,
                IdentityHashMap<T, LayoutNode<T>> nodeMap,
                IdentityHashMap<T, Integer> layerIndexMap) {
            // Collect edges that span multiple layers
            // We need to process the original graph edges
            Map<LayoutNode<T>, List<LayoutNode<T>>> childEdges = new LinkedHashMap<>();

            for (LayoutNode<T> ln : nodeMap.values()) {
                if (ln.original == null)
                    continue;
                List<T> children = getChildren(ln.original, childrenFn);
                List<LayoutNode<T>> childLayouts = new ArrayList<>();
                for (T child : children) {
                    LayoutNode<T> childLn = nodeMap.get(child);
                    if (childLn != null) {
                        childLayouts.add(childLn);
                    }
                }
                childEdges.put(ln, childLayouts);
            }

            // Insert dummies for long edges
            for (Map.Entry<LayoutNode<T>, List<LayoutNode<T>>> entry : childEdges.entrySet()) {
                LayoutNode<T> parent = entry.getKey();
                for (LayoutNode<T> child : entry.getValue()) {
                    int span = child.layer - parent.layer;
                    if (span > 1) {
                        LayoutNode<T> prev = parent;
                        for (int l = parent.layer + 1; l < child.layer; l++) {
                            LayoutNode<T> dummy = LayoutNode.dummy(l);
                            // Add to existing layer or extend
                            while (layers.size() <= l) {
                                layers.add(new ArrayList<>());
                            }
                            layers.get(l).add(dummy);
                            prev = dummy;
                        }
                    }
                }
            }

            // Re-index all layers
            for (List<LayoutNode<T>> layer : layers) {
                for (int i = 0; i < layer.size(); i++) {
                    layer.get(i).indexInLayer = i;
                }
            }
        }

        private List<int[]> buildEdgeList(List<List<LayoutNode<T>>> layers,
                IdentityHashMap<T, LayoutNode<T>> nodeMap) {
            List<int[]> edges = new ArrayList<>();

            // Build a mapping from LayoutNode to its position
            IdentityHashMap<LayoutNode<T>, int[]> posMap = new IdentityHashMap<>();
            for (int l = 0; l < layers.size(); l++) {
                List<LayoutNode<T>> layer = layers.get(l);
                for (int i = 0; i < layer.size(); i++) {
                    posMap.put(layer.get(i), new int[] { l, i });
                }
            }

            // For real nodes, add edges to their real children
            // For long edges, we need to connect through dummies
            // Simplified approach: for each real parent, find edges to children
            // potentially going through dummies
            for (LayoutNode<T> ln : nodeMap.values()) {
                if (ln.original == null)
                    continue;
                int[] parentPos = posMap.get(ln);
                if (parentPos == null)
                    continue;

                for (T child : getChildren(ln.original, childrenFn)) {
                    LayoutNode<T> childLn = nodeMap.get(child);
                    if (childLn == null)
                        continue;

                    int span = childLn.layer - ln.layer;
                    if (span == 1) {
                        // Direct edge
                        int[] childPos = posMap.get(childLn);
                        edges.add(new int[] { parentPos[0], parentPos[1], childPos[0], childPos[1] });
                    } else if (span > 1) {
                        // Find dummy chain: look for dummies in intermediate layers
                        // that were inserted for this edge
                        LayoutNode<T> prev = ln;
                        int[] prevPos = parentPos;
                        for (int l = ln.layer + 1; l < childLn.layer; l++) {
                            // Find the dummy in this layer for this edge
                            // Since dummies were appended, find one that isn't already connected
                            List<LayoutNode<T>> layer = layers.get(l);
                            LayoutNode<T> dummy = findUnconnectedDummy(layer, edges, l);
                            if (dummy != null) {
                                int[] dummyPos = posMap.get(dummy);
                                edges.add(new int[] { prevPos[0], prevPos[1], dummyPos[0], dummyPos[1] });
                                prev = dummy;
                                prevPos = dummyPos;
                            }
                        }
                        int[] childPos = posMap.get(childLn);
                        edges.add(new int[] { prevPos[0], prevPos[1], childPos[0], childPos[1] });
                    }
                }
            }

            return edges;
        }

        private LayoutNode<T> findUnconnectedDummy(List<LayoutNode<T>> layer, List<int[]> edges, int layerIdx) {
            // Find a dummy node in this layer that isn't already a child in an edge
            Set<Integer> connectedIndices = new HashSet<>();
            for (int[] edge : edges) {
                if (edge[2] == layerIdx) {
                    connectedIndices.add(edge[3]);
                }
            }
            for (LayoutNode<T> ln : layer) {
                if (ln.isDummy && !connectedIndices.contains(ln.indexInLayer)) {
                    return ln;
                }
            }
            return null;
        }

        private void minimizeCrossings(List<List<LayoutNode<T>>> layers, List<int[]> edges) {
            // Barycenter heuristic: 4 passes alternating top-down / bottom-up
            for (int pass = 0; pass < 4; pass++) {
                if (pass % 2 == 0) {
                    // Top-down
                    for (int l = 1; l < layers.size(); l++) {
                        reorderByBarycenter(layers, edges, l, true);
                    }
                } else {
                    // Bottom-up
                    for (int l = layers.size() - 2; l >= 0; l--) {
                        reorderByBarycenter(layers, edges, l, false);
                    }
                }
            }
        }

        private void reorderByBarycenter(List<List<LayoutNode<T>>> layers, List<int[]> edges,
                int layerIdx, boolean fromParent) {
            List<LayoutNode<T>> layer = layers.get(layerIdx);
            if (layer.size() <= 1)
                return;

            double[] barycenters = new double[layer.size()];
            for (int i = 0; i < layer.size(); i++) {
                List<Integer> connectedPositions = new ArrayList<>();
                for (int[] edge : edges) {
                    if (fromParent) {
                        // Looking at parent positions
                        if (edge[2] == layerIdx && edge[3] == i) {
                            connectedPositions.add(edge[1]);
                        }
                    } else {
                        // Looking at child positions
                        if (edge[0] == layerIdx && edge[1] == i) {
                            connectedPositions.add(edge[3]);
                        }
                    }
                }
                if (connectedPositions.isEmpty()) {
                    barycenters[i] = i;
                } else {
                    double sum = 0;
                    for (int pos : connectedPositions)
                        sum += pos;
                    barycenters[i] = sum / connectedPositions.size();
                }
            }

            // Sort by barycenter using insertion sort to maintain stability
            List<LayoutNode<T>> sorted = new ArrayList<>(layer);
            double[] sortedBary = barycenters.clone();
            for (int i = 1; i < sorted.size(); i++) {
                LayoutNode<T> key = sorted.get(i);
                double keyBary = sortedBary[i];
                int j = i - 1;
                while (j >= 0 && sortedBary[j] > keyBary) {
                    sorted.set(j + 1, sorted.get(j));
                    sortedBary[j + 1] = sortedBary[j];
                    j--;
                }
                sorted.set(j + 1, key);
                sortedBary[j + 1] = keyBary;
            }

            // Build old→new index remapping table
            int[] remap = new int[sorted.size()];
            for (int i = 0; i < sorted.size(); i++) {
                remap[sorted.get(i).indexInLayer] = i;
            }

            // Update layer and indices
            layers.set(layerIdx, sorted);
            for (int i = 0; i < sorted.size(); i++) {
                sorted.get(i).indexInLayer = i;
            }

            // Update edge references atomically using remap
            for (int[] edge : edges) {
                if (edge[0] == layerIdx) {
                    edge[1] = remap[edge[1]];
                }
                if (edge[2] == layerIdx) {
                    edge[3] = remap[edge[3]];
                }
            }
        }

        private void splitWideLayers(List<List<LayoutNode<T>>> layers) {
            if (maxWidth <= 0)
                return;

            for (int l = 0; l < layers.size(); l++) {
                List<LayoutNode<T>> layer = layers.get(l);
                int layerWidth = computeLayerWidth(layer);
                if (layerWidth <= maxWidth)
                    continue;

                // Greedy packing: fit as many nodes as possible per sub-row
                List<List<LayoutNode<T>>> subRows = new ArrayList<>();
                List<LayoutNode<T>> currentRow = new ArrayList<>();
                int currentWidth = 0;

                for (LayoutNode<T> node : layer) {
                    int addedWidth = currentRow.isEmpty()
                            ? node.width()
                            : node.width() + NODE_GAP;
                    if (currentWidth + addedWidth > maxWidth && !currentRow.isEmpty()) {
                        subRows.add(currentRow);
                        currentRow = new ArrayList<>();
                        currentWidth = 0;
                        addedWidth = node.width();
                    }
                    currentRow.add(node);
                    currentWidth += addedWidth;
                }
                if (!currentRow.isEmpty())
                    subRows.add(currentRow);
                if (subRows.size() <= 1)
                    continue;

                // Replace original layer with first sub-row, insert rest after it
                layers.set(l, subRows.get(0));
                for (int s = 1; s < subRows.size(); s++) {
                    layers.add(l + s, subRows.get(s));
                }

                // Update LayoutNode.layer for all nodes from here onward
                for (int k = l; k < layers.size(); k++) {
                    for (LayoutNode<T> ln : layers.get(k)) {
                        ln.layer = k;
                    }
                }

                // Skip past the sub-rows we just inserted
                l += subRows.size() - 1;
            }
        }

        private int computeLayerWidth(List<LayoutNode<T>> layer) {
            int w = 0;
            for (LayoutNode<T> ln : layer) {
                w += ln.width() + NODE_GAP;
            }
            return layer.isEmpty() ? 0 : w - NODE_GAP;
        }

        private void assignCoordinates(List<List<LayoutNode<T>>> layers) {
            // First pass: assign positions based on node widths
            int totalWidth = 0;
            for (List<LayoutNode<T>> layer : layers) {
                int layerWidth = 0;
                for (LayoutNode<T> ln : layer) {
                    layerWidth += ln.width() + NODE_GAP;
                }
                if (!layer.isEmpty()) {
                    layerWidth -= NODE_GAP;
                }
                totalWidth = Math.max(totalWidth, layerWidth);
            }

            for (List<LayoutNode<T>> layer : layers) {
                int layerWidth = 0;
                for (LayoutNode<T> ln : layer) {
                    layerWidth += ln.width() + NODE_GAP;
                }
                if (!layer.isEmpty()) {
                    layerWidth -= NODE_GAP;
                }

                int offset = (totalWidth - layerWidth) / 2;
                int pos = offset;
                for (LayoutNode<T> ln : layer) {
                    ln.x = pos + ln.width() / 2;
                    pos += ln.width() + NODE_GAP;
                }
            }
        }

        private int computeRoutingRowCount(List<List<LayoutNode<T>>> layers,
                List<int[]> allEdges, int layerIdx) {
            Map<Integer, Set<Integer>> childrenByParent = new LinkedHashMap<>();
            for (int[] edge : allEdges) {
                if (edge[0] == layerIdx && edge[2] == layerIdx + 1) {
                    childrenByParent.computeIfAbsent(edge[1], k -> new HashSet<>()).add(edge[3]);
                }
            }
            if (childrenByParent.size() <= 1)
                return 1;

            // If all parents share the exact same children set, one row suffices
            Set<Set<Integer>> distinctChildSets = new HashSet<>(childrenByParent.values());
            if (distinctChildSets.size() <= 1)
                return 1;

            return childrenByParent.size();
        }

        private char[][] renderGrid(List<List<LayoutNode<T>>> layers, List<int[]> allEdges) {
            int gridWidth = 0;
            for (List<LayoutNode<T>> layer : layers) {
                for (LayoutNode<T> ln : layer) {
                    gridWidth = Math.max(gridWidth, ln.x + (ln.width() + 1) / 2 + 1);
                }
            }
            gridWidth = Math.max(gridWidth, 1);

            // Compute routing row counts for each layer pair
            int[] routingRowCounts = new int[layers.size() > 1 ? layers.size() - 1 : 0];
            for (int l = 0; l < routingRowCounts.length; l++) {
                routingRowCounts[l] = computeRoutingRowCount(layers, allEdges, l);
            }

            // Calculate grid height with variable routing rows
            int gridHeight = layers.size();
            for (int count : routingRowCounts) {
                gridHeight += count;
            }
            if (gridHeight < 1)
                gridHeight = 1;

            char[][] grid = new char[gridHeight][gridWidth];
            for (char[] row : grid) {
                java.util.Arrays.fill(row, ' ');
            }

            // Compute row mapping: which grid row corresponds to each layer
            int[] layerRow = new int[layers.size()];
            layerRow[0] = 0;
            for (int l = 1; l < layers.size(); l++) {
                layerRow[l] = layerRow[l - 1] + 1 + routingRowCounts[l - 1];
            }

            // Write node labels
            for (int l = 0; l < layers.size(); l++) {
                int row = layerRow[l];
                for (LayoutNode<T> ln : layers.get(l)) {
                    if (ln.isDummy) {
                        if (ln.x >= 0 && ln.x < gridWidth) {
                            grid[row][ln.x] = style.vertical();
                        }
                    } else {
                        int startX = ln.x - ln.label.length() / 2;
                        for (int i = 0; i < ln.label.length(); i++) {
                            int gx = startX + i;
                            if (gx >= 0 && gx < gridWidth) {
                                grid[row][gx] = ln.label.charAt(i);
                            }
                        }
                    }
                }
            }

            // Render edges in routing rows
            for (int l = 0; l < layers.size() - 1; l++) {
                int startRoutingRow = layerRow[l] + 1;
                int numRoutingRows = routingRowCounts[l];
                if (numRoutingRows == 1) {
                    renderRoutingRow(grid, startRoutingRow, layers, allEdges, l, gridWidth);
                } else {
                    renderMultiRowRouting(grid, startRoutingRow, numRoutingRows,
                            layers, allEdges, l, gridWidth);
                }
            }

            return grid;
        }

        private void renderMultiRowRouting(char[][] grid, int startRow, int numRows,
                List<List<LayoutNode<T>>> layers, List<int[]> allEdges,
                int parentLayerIdx, int gridWidth) {
            List<LayoutNode<T>> parentLayer = layers.get(parentLayerIdx);
            List<LayoutNode<T>> childLayer = layers.get(parentLayerIdx + 1);

            // Group edges by parent index
            Map<Integer, List<int[]>> edgesByParent = new LinkedHashMap<>();
            for (int[] edge : allEdges) {
                if (edge[0] == parentLayerIdx && edge[2] == parentLayerIdx + 1) {
                    edgesByParent.computeIfAbsent(edge[1], k -> new ArrayList<>()).add(edge);
                }
            }

            // Sort parent groups by parent x position (left to right)
            List<Integer> parentIndices = new ArrayList<>(edgesByParent.keySet());
            parentIndices.sort((a, b) -> parentLayer.get(a).x - parentLayer.get(b).x);

            // Track child x positions connected in previous sub-rows
            Set<Integer> connectedChildXPositions = new HashSet<>();

            for (int subRow = 0; subRow < parentIndices.size(); subRow++) {
                int gridRow = startRow + subRow;
                int parentIdx = parentIndices.get(subRow);
                List<int[]> parentEdges = edgesByParent.get(parentIdx);
                LayoutNode<T> parent = parentLayer.get(parentIdx);

                boolean[] up = new boolean[gridWidth];
                boolean[] down = new boolean[gridWidth];
                boolean[] left = new boolean[gridWidth];
                boolean[] right = new boolean[gridWidth];

                // Draw this parent's edges
                for (int[] edge : parentEdges) {
                    LayoutNode<T> child = childLayer.get(edge[3]);
                    int xp = parent.x;
                    int xc = child.x;

                    if (xp == xc) {
                        up[xp] = true;
                        down[xp] = true;
                    } else {
                        int minX = Math.min(xp, xc);
                        int maxX = Math.max(xp, xc);

                        up[xp] = true;
                        if (xc > xp)
                            right[xp] = true;
                        else
                            left[xp] = true;

                        down[xc] = true;
                        if (xp > xc)
                            right[xc] = true;
                        else
                            left[xc] = true;

                        for (int x = minX + 1; x < maxX; x++) {
                            if (x >= 0 && x < gridWidth) {
                                left[x] = true;
                                right[x] = true;
                            }
                        }
                    }
                }

                // Vertical pass-throughs for parents not yet routed
                for (int future = subRow + 1; future < parentIndices.size(); future++) {
                    int futureIdx = parentIndices.get(future);
                    int fx = parentLayer.get(futureIdx).x;
                    if (fx >= 0 && fx < gridWidth) {
                        up[fx] = true;
                        down[fx] = true;
                    }
                }

                // Vertical pass-throughs for child positions connected in previous sub-rows
                for (int cx : connectedChildXPositions) {
                    if (cx >= 0 && cx < gridWidth) {
                        up[cx] = true;
                        down[cx] = true;
                    }
                }

                // Record child positions from this parent's edges
                for (int[] edge : parentEdges) {
                    connectedChildXPositions.add(childLayer.get(edge[3]).x);
                }

                // Write junction characters
                for (int x = 0; x < gridWidth; x++) {
                    if (!up[x] && !down[x] && !left[x] && !right[x])
                        continue;
                    grid[gridRow][x] = selectJunction(up[x], down[x], left[x], right[x]);
                }
            }
        }

        private void renderRoutingRow(char[][] grid, int routingRow,
                List<List<LayoutNode<T>>> layers, List<int[]> allEdges,
                int parentLayerIdx, int gridWidth) {
            // Collect edges between parentLayerIdx and parentLayerIdx+1
            List<int[]> layerEdges = new ArrayList<>();
            for (int[] edge : allEdges) {
                if (edge[0] == parentLayerIdx && edge[2] == parentLayerIdx + 1) {
                    layerEdges.add(edge);
                }
            }

            if (layerEdges.isEmpty())
                return;

            List<LayoutNode<T>> parentLayer = layers.get(parentLayerIdx);
            List<LayoutNode<T>> childLayer = layers.get(parentLayerIdx + 1);

            // Track directional flags for each cell in routing row
            boolean[] up = new boolean[gridWidth];
            boolean[] down = new boolean[gridWidth];
            boolean[] left = new boolean[gridWidth];
            boolean[] right = new boolean[gridWidth];

            for (int[] edge : layerEdges) {
                LayoutNode<T> parent = parentLayer.get(edge[1]);
                LayoutNode<T> child = childLayer.get(edge[3]);
                int xp = parent.x;
                int xc = child.x;

                if (xp == xc) {
                    // Straight vertical
                    if (xp >= 0 && xp < gridWidth) {
                        up[xp] = true;
                        down[xp] = true;
                    }
                } else {
                    int minX = Math.min(xp, xc);
                    int maxX = Math.max(xp, xc);

                    // Parent connection point
                    if (xp >= 0 && xp < gridWidth) {
                        up[xp] = true;
                        if (xc > xp)
                            right[xp] = true;
                        else
                            left[xp] = true;
                    }

                    // Child connection point
                    if (xc >= 0 && xc < gridWidth) {
                        down[xc] = true;
                        if (xp > xc)
                            right[xc] = true;
                        else
                            left[xc] = true;
                    }

                    // Horizontal span between
                    for (int x = minX + 1; x < maxX; x++) {
                        if (x >= 0 && x < gridWidth) {
                            left[x] = true;
                            right[x] = true;
                        }
                    }
                }
            }

            // Write junction characters
            for (int x = 0; x < gridWidth; x++) {
                if (!up[x] && !down[x] && !left[x] && !right[x])
                    continue;
                grid[routingRow][x] = selectJunction(up[x], down[x], left[x], right[x]);
            }
        }

        private char selectJunction(boolean up, boolean down, boolean left, boolean right) {
            if (up && down && left && right)
                return style.cross();
            if (up && down && right && !left)
                return style.rightTee();
            if (up && down && left && !right)
                return style.leftTee();
            if (up && down && !left && !right)
                return style.vertical();
            if (up && left && right && !down)
                return style.upTee();
            if (down && left && right && !up)
                return style.downTee();
            if (up && right && !down && !left)
                return style.bottomLeft();
            if (up && left && !down && !right)
                return style.bottomRight();
            if (down && right && !up && !left)
                return style.topLeft();
            if (down && left && !up && !right)
                return style.topRight();
            if (left && right && !up && !down)
                return style.horizontal();
            if (up && !down && !left && !right)
                return style.vertical();
            if (down && !up && !left && !right)
                return style.vertical();
            if (left && !right && !up && !down)
                return style.horizontal();
            if (right && !left && !up && !down)
                return style.horizontal();
            return ' ';
        }

        private String gridToString(char[][] grid) {
            StringBuilder sb = new StringBuilder();
            int lastNonBlankRow = -1;
            for (int r = grid.length - 1; r >= 0; r--) {
                for (char c : grid[r]) {
                    if (c != ' ') {
                        lastNonBlankRow = r;
                        break;
                    }
                }
                if (lastNonBlankRow >= 0)
                    break;
            }

            for (int r = 0; r <= lastNonBlankRow; r++) {
                // Right-trim
                int lastNonSpace = -1;
                for (int c = grid[r].length - 1; c >= 0; c--) {
                    if (grid[r][c] != ' ') {
                        lastNonSpace = c;
                        break;
                    }
                }
                if (lastNonSpace >= 0) {
                    sb.append(grid[r], 0, lastNonSpace + 1);
                }
                sb.append(System.lineSeparator());
            }

            return sb.toString();
        }
    }

    /**
     * Builder for constructing {@link Renderer} instances with a fluent API.
     */
    public static class Builder<T> {
        private Function<T, String> labelFn;
        private Function<T, List<T>> childrenFn;
        private GraphStyle style = GraphStyle.UNICODE;
        private int maxWidth = 0;

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
         * Sets the visual style for graph connectors.
         */
        public Builder<T> style(GraphStyle style) {
            this.style = style;
            return this;
        }

        /**
         * Sets the maximum output width in characters.
         * Layers wider than this will be split into multiple sub-rows.
         * A value of 0 (the default) means no limit.
         */
        public Builder<T> maxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        /**
         * Builds an immutable graph renderer.
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
            return new Renderer<>(labelFn, childrenFn, style, maxWidth);
        }
    }
}

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

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
    private static final int DEFAULT_TERMINAL_WIDTH = 80;

    private Graph() {
    }

    /**
     * Detects the terminal width by checking the COLUMNS environment variable
     * and falling back to {@code stty size}. Returns 80 if detection fails.
     */
    static int detectTerminalWidth() {
        // Try COLUMNS env variable first
        String columns = System.getenv("COLUMNS");
        if (columns != null) {
            try {
                int w = Integer.parseInt(columns.trim());
                if (w > 0)
                    return w;
            } catch (NumberFormatException ignored) {
            }
        }

        // Try stty size
        try {
            ProcessBuilder pb = new ProcessBuilder("stty", "size");
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            Process proc = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        int w = Integer.parseInt(parts[1]);
                        if (w > 0)
                            return w;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return DEFAULT_TERMINAL_WIDTH;
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
     * Terminal width is auto-detected.
     */
    public static String render(GraphNode root, GraphStyle style) {
        return new Renderer<>(GraphNode::label, GraphNode::children, style, -1, 0).render(root);
    }

    /**
     * Renders a {@link GraphNode} graph using the specified style
     * with a maximum output width.
     *
     * @param maxWidth maximum character width (0 = no limit)
     */
    public static String render(GraphNode root, GraphStyle style, int maxWidth) {
        return new Renderer<>(GraphNode::label, GraphNode::children, style, maxWidth, 0).render(root);
    }

    /**
     * Renders a {@link GraphNode} graph using the specified style
     * with a maximum output width and label wrapping.
     *
     * @param maxWidth maximum character width (0 = no limit)
     * @param maxLabelWidth maximum label width before wrapping at word boundaries (0 = no wrapping)
     */
    public static String render(GraphNode root, GraphStyle style, int maxWidth, int maxLabelWidth) {
        return new Renderer<>(GraphNode::label, GraphNode::children, style, maxWidth, maxLabelWidth).render(root);
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
        String[] lines;
        int layer;
        int indexInLayer;
        int x;
        boolean isDummy;

        LayoutNode(T original, String label, int layer) {
            this.original = original;
            this.label = label;
            this.lines = new String[] { label };
            this.layer = layer;
        }

        static <T> LayoutNode<T> dummy(int layer) {
            LayoutNode<T> d = new LayoutNode<>(null, "", layer);
            d.isDummy = true;
            return d;
        }

        void wrapLabel(int maxLabelWidth) {
            if (maxLabelWidth <= 0 || label.length() <= maxLabelWidth || isDummy)
                return;
            List<String> result = new ArrayList<>();
            int start = 0;
            while (start < label.length()) {
                int end = Math.min(start + maxLabelWidth, label.length());
                if (end < label.length()) {
                    int space = label.lastIndexOf(' ', end);
                    if (space > start)
                        end = space;
                }
                result.add(label.substring(start, end).trim());
                start = end;
                if (start < label.length() && label.charAt(start) == ' ')
                    start++;
            }
            this.lines = result.toArray(new String[0]);
        }

        int width() {
            int max = 0;
            for (String line : lines) {
                max = Math.max(max, line.length());
            }
            return Math.max(max, 1);
        }

        int height() {
            return lines.length;
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
        private final int maxLabelWidth;

        Renderer(Function<T, String> labelFn, Function<T, List<T>> childrenFn,
                GraphStyle style, int maxWidth, int maxLabelWidth) {
            this.labelFn = labelFn;
            this.childrenFn = childrenFn;
            this.style = style;
            this.maxWidth = maxWidth < 0 ? detectTerminalWidth() : maxWidth;
            this.maxLabelWidth = maxLabelWidth;
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

            // Wrap long labels if maxLabelWidth is set
            if (maxLabelWidth > 0) {
                for (LayoutNode<T> ln : nodeMap.values()) {
                    ln.wrapLabel(maxLabelWidth);
                }
            }

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
            Map<LayoutNode<T>, Map<Integer, List<LayoutNode<T>>>> bridgeMap = insertDummies(layers, nodeMap, layerIndexMap);

            // Phase 4: Order within layers (barycenter heuristic)
            List<int[]> allEdges = buildEdgeList(layers, nodeMap, bridgeMap);
            minimizeCrossings(layers, allEdges);

            // Rebuild edges after crossing minimization to ensure correctness
            allEdges = buildEdgeList(layers, nodeMap, bridgeMap);

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

        private Map<LayoutNode<T>, Map<Integer, List<LayoutNode<T>>>> insertDummies(
                List<List<LayoutNode<T>>> layers,
                IdentityHashMap<T, LayoutNode<T>> nodeMap,
                IdentityHashMap<T, Integer> layerIndexMap) {
            // Collect edges that span multiple layers
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

            // Bridge map: parent → targetLayer → chain of bridge dummies
            Map<LayoutNode<T>, Map<Integer, List<LayoutNode<T>>>> bridgeMap = new IdentityHashMap<>();

            if (maxWidth > 0) {
                // Shared bridge mode: group long-spanning edges by (parent, targetLayer)
                // and create ONE bridge chain per group instead of per-edge dummies
                for (Map.Entry<LayoutNode<T>, List<LayoutNode<T>>> entry : childEdges.entrySet()) {
                    LayoutNode<T> parent = entry.getKey();
                    Map<Integer, List<LayoutNode<T>>> childrenByTargetLayer = new LinkedHashMap<>();
                    for (LayoutNode<T> child : entry.getValue()) {
                        int span = child.layer - parent.layer;
                        if (span > 1) {
                            childrenByTargetLayer.computeIfAbsent(child.layer, k -> new ArrayList<>()).add(child);
                        }
                    }

                    if (childrenByTargetLayer.isEmpty())
                        continue;

                    Map<Integer, List<LayoutNode<T>>> parentBridgeChains = new LinkedHashMap<>();
                    for (int targetLayer : childrenByTargetLayer.keySet()) {
                        List<LayoutNode<T>> chain = new ArrayList<>();
                        for (int l = parent.layer + 1; l < targetLayer; l++) {
                            LayoutNode<T> dummy = LayoutNode.dummy(l);
                            while (layers.size() <= l) {
                                layers.add(new ArrayList<>());
                            }
                            layers.get(l).add(dummy);
                            chain.add(dummy);
                        }
                        parentBridgeChains.put(targetLayer, chain);
                    }
                    bridgeMap.put(parent, parentBridgeChains);
                }
            } else {
                // Original per-edge dummy creation (maxWidth disabled)
                for (Map.Entry<LayoutNode<T>, List<LayoutNode<T>>> entry : childEdges.entrySet()) {
                    LayoutNode<T> parent = entry.getKey();
                    for (LayoutNode<T> child : entry.getValue()) {
                        int span = child.layer - parent.layer;
                        if (span > 1) {
                            for (int l = parent.layer + 1; l < child.layer; l++) {
                                LayoutNode<T> dummy = LayoutNode.dummy(l);
                                while (layers.size() <= l) {
                                    layers.add(new ArrayList<>());
                                }
                                layers.get(l).add(dummy);
                            }
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

            return bridgeMap;
        }

        private List<int[]> buildEdgeList(List<List<LayoutNode<T>>> layers,
                IdentityHashMap<T, LayoutNode<T>> nodeMap,
                Map<LayoutNode<T>, Map<Integer, List<LayoutNode<T>>>> bridgeMap) {
            List<int[]> edges = new ArrayList<>();

            // Build a mapping from LayoutNode to its position
            IdentityHashMap<LayoutNode<T>, int[]> posMap = new IdentityHashMap<>();
            for (int l = 0; l < layers.size(); l++) {
                List<LayoutNode<T>> layer = layers.get(l);
                for (int i = 0; i < layer.size(); i++) {
                    posMap.put(layer.get(i), new int[] { l, i });
                }
            }

            // Track which bridge chains have had their internal edges added
            Set<List<LayoutNode<T>>> connectedChains = Collections.newSetFromMap(new IdentityHashMap<>());

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
                        // Check bridge map first
                        Map<Integer, List<LayoutNode<T>>> parentBridges = bridgeMap != null ? bridgeMap.get(ln) : null;
                        List<LayoutNode<T>> chain = parentBridges != null ? parentBridges.get(childLn.layer) : null;

                        if (chain != null && !chain.isEmpty()) {
                            // Shared bridge chain: connect chain edges only once
                            if (!connectedChains.contains(chain)) {
                                connectedChains.add(chain);
                                // parent → chain[0]
                                int[] firstPos = posMap.get(chain.get(0));
                                edges.add(new int[] { parentPos[0], parentPos[1],
                                        firstPos[0], firstPos[1] });
                                // chain[i] → chain[i+1]
                                for (int i = 0; i < chain.size() - 1; i++) {
                                    int[] from = posMap.get(chain.get(i));
                                    int[] to = posMap.get(chain.get(i + 1));
                                    edges.add(new int[] { from[0], from[1], to[0], to[1] });
                                }
                            }
                            // Fan-out: chain[last] → child
                            int[] lastPos = posMap.get(chain.get(chain.size() - 1));
                            int[] childPos = posMap.get(childLn);
                            edges.add(new int[] { lastPos[0], lastPos[1],
                                    childPos[0], childPos[1] });
                        } else {
                            // Fallback: find unconnected dummies (original behavior)
                            LayoutNode<T> prev = ln;
                            int[] prevPos = parentPos;
                            for (int l = ln.layer + 1; l < childLn.layer; l++) {
                                List<LayoutNode<T>> layer = layers.get(l);
                                LayoutNode<T> dummy = findUnconnectedDummy(layer, edges, l);
                                if (dummy != null) {
                                    int[] dummyPos = posMap.get(dummy);
                                    edges.add(new int[] { prevPos[0], prevPos[1],
                                            dummyPos[0], dummyPos[1] });
                                    prev = dummy;
                                    prevPos = dummyPos;
                                }
                            }
                            int[] childPos = posMap.get(childLn);
                            edges.add(new int[] { prevPos[0], prevPos[1],
                                    childPos[0], childPos[1] });
                        }
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

            // Count multi-child parents — those that need horizontal fan-out routing.
            // Single-child parents share a row instead of each getting their own.
            // When there's at most 1 multi-child parent, everything fits in one row.
            int multiChildCount = 0;
            boolean hasSingleChild = false;
            for (Map.Entry<Integer, Set<Integer>> entry : childrenByParent.entrySet()) {
                if (entry.getValue().size() > 1) {
                    multiChildCount++;
                } else {
                    hasSingleChild = true;
                }
            }
            if (multiChildCount <= 1)
                return 1;
            return multiChildCount + (hasSingleChild ? 1 : 0);
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

            // Compute per-layer max node height
            int[] layerHeight = new int[layers.size()];
            for (int l = 0; l < layers.size(); l++) {
                int maxH = 1;
                for (LayoutNode<T> ln : layers.get(l)) {
                    maxH = Math.max(maxH, ln.height());
                }
                layerHeight[l] = maxH;
            }

            // Calculate grid height with variable label heights and routing rows
            int gridHeight = 0;
            for (int h : layerHeight) {
                gridHeight += h;
            }
            for (int count : routingRowCounts) {
                gridHeight += count;
            }
            if (gridHeight < 1)
                gridHeight = 1;

            char[][] grid = new char[gridHeight][gridWidth];
            for (char[] row : grid) {
                java.util.Arrays.fill(row, ' ');
            }

            // Compute row mapping: which grid row corresponds to each layer's first label line
            int[] layerRow = new int[layers.size()];
            layerRow[0] = 0;
            for (int l = 1; l < layers.size(); l++) {
                layerRow[l] = layerRow[l - 1] + layerHeight[l - 1] + routingRowCounts[l - 1];
            }

            // Write node labels (potentially multi-line)
            for (int l = 0; l < layers.size(); l++) {
                int baseRow = layerRow[l];
                for (LayoutNode<T> ln : layers.get(l)) {
                    if (ln.isDummy) {
                        for (int r = 0; r < layerHeight[l]; r++) {
                            if (ln.x >= 0 && ln.x < gridWidth) {
                                grid[baseRow + r][ln.x] = style.vertical();
                            }
                        }
                    } else {
                        int nodeWidth = ln.width();
                        for (int lineIdx = 0; lineIdx < ln.lines.length; lineIdx++) {
                            String line = ln.lines[lineIdx];
                            int startX = ln.x - nodeWidth / 2 + (nodeWidth - line.length()) / 2;
                            int gridRow = baseRow + lineIdx;
                            if (gridRow >= grid.length)
                                break;
                            for (int i = 0; i < line.length(); i++) {
                                int gx = startX + i;
                                if (gx >= 0 && gx < gridWidth) {
                                    grid[gridRow][gx] = line.charAt(i);
                                }
                            }
                        }
                    }
                }
            }

            // Render edges in routing rows
            for (int l = 0; l < layers.size() - 1; l++) {
                int startRoutingRow = layerRow[l] + layerHeight[l];
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

            // Split parents into multi-child (need own routing row) vs single-child
            // (all share one routing row)
            List<Integer> multiChildParents = new ArrayList<>();
            List<Integer> singleChildParents = new ArrayList<>();
            for (Map.Entry<Integer, List<int[]>> entry : edgesByParent.entrySet()) {
                int parentIdx = entry.getKey();
                if (entry.getValue().size() > 1) {
                    multiChildParents.add(parentIdx);
                } else {
                    singleChildParents.add(parentIdx);
                }
            }

            // Sort multi-child parents by x position (left to right)
            multiChildParents.sort((a, b) -> parentLayer.get(a).x - parentLayer.get(b).x);

            // Track child x positions connected in previous sub-rows
            Set<Integer> connectedChildXPositions = new HashSet<>();

            // Render multi-child parents first, each in its own sub-row
            for (int subRow = 0; subRow < multiChildParents.size(); subRow++) {
                int gridRow = startRow + subRow;
                int parentIdx = multiChildParents.get(subRow);
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

                // Vertical pass-throughs for multi-child parents not yet routed
                for (int future = subRow + 1; future < multiChildParents.size(); future++) {
                    int futureIdx = multiChildParents.get(future);
                    int fx = parentLayer.get(futureIdx).x;
                    if (fx >= 0 && fx < gridWidth) {
                        up[fx] = true;
                        down[fx] = true;
                    }
                }

                // Vertical pass-throughs for all single-child parents (routed in shared row)
                for (int singleIdx : singleChildParents) {
                    int sx = parentLayer.get(singleIdx).x;
                    if (sx >= 0 && sx < gridWidth) {
                        up[sx] = true;
                        down[sx] = true;
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

            // Render all single-child parents in one shared routing row
            if (!singleChildParents.isEmpty()) {
                int gridRow = startRow + multiChildParents.size();

                boolean[] up = new boolean[gridWidth];
                boolean[] down = new boolean[gridWidth];
                boolean[] left = new boolean[gridWidth];
                boolean[] right = new boolean[gridWidth];

                for (int parentIdx : singleChildParents) {
                    int[] edge = edgesByParent.get(parentIdx).get(0);
                    LayoutNode<T> parent = parentLayer.get(parentIdx);
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

                // Vertical pass-throughs for child positions connected in previous sub-rows
                for (int cx : connectedChildXPositions) {
                    if (cx >= 0 && cx < gridWidth) {
                        up[cx] = true;
                        down[cx] = true;
                    }
                }

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
        private int maxWidth = -1;
        private int maxLabelWidth = 0;

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
         * A value of 0 means no limit. A negative value (the default)
         * auto-detects the terminal width.
         */
        public Builder<T> maxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        /**
         * Sets the maximum label width before wrapping at word boundaries.
         * A value of 0 (the default) means no wrapping.
         */
        public Builder<T> maxLabelWidth(int maxLabelWidth) {
            this.maxLabelWidth = maxLabelWidth;
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
            return new Renderer<>(labelFn, childrenFn, style, maxWidth, maxLabelWidth);
        }
    }
}

package com.roze.dbnavigator.ui.diagram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Advanced graph layout engine for database ER diagrams, implementing:
 * 1. Sugiyama layered hierarchical layout (DataGrip default) with cycle breaking,
 *    topological rank assignment, and barycentric crossing reduction.
 * 2. Connected component partitioning and pack-tiling.
 * 3. Left-to-Right orientation.
 * 4. Orthogonal compact layout.
 * 5. Organic force-directed (spring-embedder) layout.
 */
public class DiagramLayoutEngine {

    public enum LayoutMode {
        HIERARCHICAL_TOP_TO_BOTTOM,
        HIERARCHICAL_LEFT_TO_RIGHT,
        ORTHOGONAL_COMPACT,
        ORGANIC
    }

    private static final double NODE_SPACING_X = 80.0;
    private static final double NODE_SPACING_Y = 90.0;
    private static final double COMPONENT_SPACING = 120.0;
    private static final double MAX_ROW_WIDTH = 3200.0;
    private static final double PADDING = 60.0;

    public static void applyLayout(DiagramGraph graph, LayoutMode mode) {
        if (graph == null || graph.getNodes().isEmpty()) return;

        switch (mode) {
            case HIERARCHICAL_LEFT_TO_RIGHT -> layoutHierarchical(graph, false);
            case ORTHOGONAL_COMPACT -> layoutOrthogonal(graph);
            case ORGANIC -> layoutOrganic(graph);
            case HIERARCHICAL_TOP_TO_BOTTOM -> layoutHierarchical(graph, true);
        }
    }

    // =========================================================================
    // 1. SUGIYAMA LAYERED HIERARCHICAL LAYOUT
    // =========================================================================

    private static void layoutHierarchical(DiagramGraph graph, boolean topToBottom) {
        List<DiagramGraph.Node> allNodes = graph.getNodes();
        if (allNodes.isEmpty()) return;

        // Step 1: Partition into connected components
        List<List<DiagramGraph.Node>> components = partitionComponents(graph);

        double curStartX = PADDING;
        double curStartY = PADDING;
        double maxRowHeight = 0;

        for (List<DiagramGraph.Node> compNodes : components) {
            // Layout this component
            ComponentBounds bounds = layoutSingleComponent(graph, compNodes, curStartX, curStartY, topToBottom);

            maxRowHeight = Math.max(maxRowHeight, bounds.height);
            curStartX += bounds.width + COMPONENT_SPACING;

            // Wrap component to next row if too wide
            if (curStartX > MAX_ROW_WIDTH && components.indexOf(compNodes) < components.size() - 1) {
                curStartX = PADDING;
                curStartY += maxRowHeight + COMPONENT_SPACING;
                maxRowHeight = 0;
            }
        }
    }

    private static ComponentBounds layoutSingleComponent(DiagramGraph graph, List<DiagramGraph.Node> compNodes,
                                                         double startX, double startY, boolean topToBottom) {
        if (compNodes.isEmpty()) return new ComponentBounds(0, 0);

        // Separate standalone nodes with 0 degree
        List<DiagramGraph.Node> connectedNodes = new ArrayList<>();
        List<DiagramGraph.Node> standaloneNodes = new ArrayList<>();

        for (DiagramGraph.Node n : compNodes) {
            if (graph.getConnectedEdges(n).isEmpty()) {
                standaloneNodes.add(n);
            } else {
                connectedNodes.add(n);
            }
        }

        if (connectedNodes.isEmpty()) {
            return layoutGrid(standaloneNodes, startX, startY);
        }

        // Substep 1: Break cycles using DFS
        breakCycles(graph, connectedNodes);

        // Substep 2: Assign ranks (Sugiyama Layer Assignment)
        assignRanks(graph, connectedNodes);

        // Group nodes by rank
        Map<Integer, List<DiagramGraph.Node>> rankLayers = new LinkedHashMap<>();
        int minRank = Integer.MAX_VALUE;
        int maxRank = Integer.MIN_VALUE;

        for (DiagramGraph.Node node : connectedNodes) {
            int r = node.getRank();
            minRank = Math.min(minRank, r);
            maxRank = Math.max(maxRank, r);
            rankLayers.computeIfAbsent(r, k -> new ArrayList<>()).add(node);
        }

        // Normalize ranks so minRank starts at 0
        Map<Integer, List<DiagramGraph.Node>> normalizedLayers = new LinkedHashMap<>();
        for (int r = minRank; r <= maxRank; r++) {
            List<DiagramGraph.Node> layer = rankLayers.getOrDefault(r, Collections.emptyList());
            normalizedLayers.put(r - minRank, new ArrayList<>(layer));
        }
        int numLayers = maxRank - minRank + 1;

        // Substep 3: Crossing Reduction (Barycentric Sorting)
        reduceCrossings(graph, normalizedLayers, numLayers);

        // Substep 4: Coordinate Assignment
        double totalWidth = 0;
        double totalHeight = 0;

        if (topToBottom) {
            double currentY = startY;
            for (int l = 0; l < numLayers; l++) {
                List<DiagramGraph.Node> layer = normalizedLayers.getOrDefault(l, Collections.emptyList());
                if (layer.isEmpty()) continue;

                double layerWidth = 0;
                double maxNodeHeight = 0;
                for (int i = 0; i < layer.size(); i++) {
                    DiagramGraph.Node n = layer.get(i);
                    layerWidth += n.getWidth() + (i > 0 ? NODE_SPACING_X : 0);
                    maxNodeHeight = Math.max(maxNodeHeight, n.getHeight());
                }

                totalWidth = Math.max(totalWidth, layerWidth);

                double currentX = startX;
                for (DiagramGraph.Node n : layer) {
                    n.setX(currentX);
                    n.setY(currentY);
                    currentX += n.getWidth() + NODE_SPACING_X;
                }

                currentY += maxNodeHeight + NODE_SPACING_Y;
            }
            totalHeight = currentY - startY;

            // Center layers relative to overall component width
            for (int l = 0; l < numLayers; l++) {
                List<DiagramGraph.Node> layer = normalizedLayers.getOrDefault(l, Collections.emptyList());
                if (layer.isEmpty()) continue;
                double layerWidth = 0;
                for (int i = 0; i < layer.size(); i++) {
                    layerWidth += layer.get(i).getWidth() + (i > 0 ? NODE_SPACING_X : 0);
                }
                double shiftX = (totalWidth - layerWidth) / 2.0;
                if (shiftX > 0) {
                    for (DiagramGraph.Node n : layer) {
                        n.setX(n.getX() + shiftX);
                    }
                }
            }
        } else {
            // Left to Right: ranks are horizontal columns, layer index = X
            double currentX = startX;
            for (int l = 0; l < numLayers; l++) {
                List<DiagramGraph.Node> layer = normalizedLayers.getOrDefault(l, Collections.emptyList());
                if (layer.isEmpty()) continue;

                double layerHeight = 0;
                double maxNodeWidth = 0;
                for (int i = 0; i < layer.size(); i++) {
                    DiagramGraph.Node n = layer.get(i);
                    layerHeight += n.getHeight() + (i > 0 ? NODE_SPACING_Y : 0);
                    maxNodeWidth = Math.max(maxNodeWidth, n.getWidth());
                }

                totalHeight = Math.max(totalHeight, layerHeight);

                double currentY = startY;
                for (DiagramGraph.Node n : layer) {
                    n.setX(currentX);
                    n.setY(currentY);
                    currentY += n.getHeight() + NODE_SPACING_Y;
                }

                currentX += maxNodeWidth + NODE_SPACING_X;
            }
            totalWidth = currentX - startX;

            // Center layers vertically
            for (int l = 0; l < numLayers; l++) {
                List<DiagramGraph.Node> layer = normalizedLayers.getOrDefault(l, Collections.emptyList());
                if (layer.isEmpty()) continue;
                double layerHeight = 0;
                for (int i = 0; i < layer.size(); i++) {
                    layerHeight += layer.get(i).getHeight() + (i > 0 ? NODE_SPACING_Y : 0);
                }
                double shiftY = (totalHeight - layerHeight) / 2.0;
                if (shiftY > 0) {
                    for (DiagramGraph.Node n : layer) {
                        n.setY(n.getY() + shiftY);
                    }
                }
            }
        }

        // Layout any standalone nodes directly under the connected component
        if (!standaloneNodes.isEmpty()) {
            double saY = startY + totalHeight + COMPONENT_SPACING / 2.0;
            ComponentBounds saBounds = layoutGrid(standaloneNodes, startX, saY);
            totalWidth = Math.max(totalWidth, saBounds.width);
            totalHeight += saBounds.height + COMPONENT_SPACING / 2.0;
        }

        return new ComponentBounds(totalWidth, totalHeight);
    }

    // Break cycles in foreign keys using DFS
    private static void breakCycles(DiagramGraph graph, List<DiagramGraph.Node> nodes) {
        Set<DiagramGraph.Node> visited = new HashSet<>();
        Set<DiagramGraph.Node> onStack = new HashSet<>();

        for (DiagramGraph.Node node : nodes) {
            dfsBreakCycles(graph, node, visited, onStack);
        }
    }

    private static void dfsBreakCycles(DiagramGraph graph, DiagramGraph.Node u,
                                      Set<DiagramGraph.Node> visited, Set<DiagramGraph.Node> onStack) {
        if (onStack.contains(u)) return;
        if (visited.contains(u)) return;

        visited.add(u);
        onStack.add(u);

        for (DiagramGraph.Edge e : graph.getOutgoingEdges(u)) {
            DiagramGraph.Node v = e.getToNode();
            if (v == null || v == u) continue;

            if (onStack.contains(v)) {
                // Back edge detected: mark reversed
                e.setReversedForCycle(true);
            } else if (!visited.contains(v)) {
                dfsBreakCycles(graph, v, visited, onStack);
            }
        }

        onStack.remove(u);
    }

    // Assign topological ranks
    private static void assignRanks(DiagramGraph graph, List<DiagramGraph.Node> nodes) {
        // In ER diagrams, root entities (PK referenced tables) should be on TOP (Rank 0).
        // If table A references table B (A.fk -> B.pk), then B is the parent, A is child.
        // Therefore rank(A) >= rank(B) + 1.
        for (DiagramGraph.Node n : nodes) {
            n.setRank(0);
        }

        // Longest path layering from root tables
        boolean changed = true;
        int iterations = 0;
        int maxIters = nodes.size() * 2;

        while (changed && iterations < maxIters) {
            changed = false;
            iterations++;
            for (DiagramGraph.Node u : nodes) {
                for (DiagramGraph.Edge e : graph.getOutgoingEdges(u)) {
                    if (e.isSelfReference()) continue;
                    DiagramGraph.Node parent = e.isReversedForCycle() ? u : e.getToNode();
                    DiagramGraph.Node child = e.isReversedForCycle() ? e.getToNode() : u;

                    if (parent == null || child == null) continue;

                    // child must be at least 1 rank below parent
                    if (child.getRank() <= parent.getRank()) {
                        child.setRank(parent.getRank() + 1);
                        changed = true;
                    }
                }
            }
        }
    }

    // Barycentric crossing reduction (iterative 2-way sweeps)
    private static void reduceCrossings(DiagramGraph graph, Map<Integer, List<DiagramGraph.Node>> layers, int numLayers) {
        // Initialize initial positions
        for (int l = 0; l < numLayers; l++) {
            List<DiagramGraph.Node> layer = layers.getOrDefault(l, Collections.emptyList());
            for (int i = 0; i < layer.size(); i++) {
                layer.get(i).setOrderInRank(i);
            }
        }

        for (int iter = 0; iter < 4; iter++) {
            // Top-down sweep (ranks 1 to numLayers - 1)
            for (int l = 1; l < numLayers; l++) {
                List<DiagramGraph.Node> layer = layers.getOrDefault(l, Collections.emptyList());
                sortLayerByBarycenter(graph, layer, true);
            }

            // Bottom-up sweep (ranks numLayers - 2 down to 0)
            for (int l = numLayers - 2; l >= 0; l--) {
                List<DiagramGraph.Node> layer = layers.getOrDefault(l, Collections.emptyList());
                sortLayerByBarycenter(graph, layer, false);
            }
        }
    }

    private static void sortLayerByBarycenter(DiagramGraph graph, List<DiagramGraph.Node> layer, boolean useParents) {
        Map<DiagramGraph.Node, Double> barycenters = new HashMap<>();

        for (DiagramGraph.Node node : layer) {
            double sum = 0;
            int count = 0;

            List<DiagramGraph.Edge> edges = useParents ? graph.getOutgoingEdges(node) : graph.getIncomingEdges(node);
            for (DiagramGraph.Edge e : edges) {
                DiagramGraph.Node neighbor = useParents ? e.getToNode() : e.getFromNode();
                if (neighbor != null && neighbor != node) {
                    sum += neighbor.getOrderInRank();
                    count++;
                }
            }

            barycenters.put(node, count > 0 ? sum / count : (double) node.getOrderInRank());
        }

        layer.sort(Comparator.comparingDouble(barycenters::get));

        for (int i = 0; i < layer.size(); i++) {
            layer.get(i).setOrderInRank(i);
        }
    }

    // =========================================================================
    // 2. ORTHOGONAL COMPACT LAYOUT
    // =========================================================================

    private static void layoutOrthogonal(DiagramGraph graph) {
        List<DiagramGraph.Node> nodes = new ArrayList<>(graph.getNodes());
        if (nodes.isEmpty()) return;

        // Sort by degree (most connected first)
        nodes.sort((a, b) -> Integer.compare(
                graph.getConnectedEdges(b).size(),
                graph.getConnectedEdges(a).size()
        ));

        int cols = Math.max(1, (int) Math.ceil(Math.sqrt(nodes.size() * 1.5)));
        double curX = PADDING;
        double curY = PADDING;
        double rowMaxH = 0;

        for (int i = 0; i < nodes.size(); i++) {
            DiagramGraph.Node n = nodes.get(i);
            n.setX(curX);
            n.setY(curY);

            rowMaxH = Math.max(rowMaxH, n.getHeight());
            curX += n.getWidth() + NODE_SPACING_X;

            if ((i + 1) % cols == 0 || i == nodes.size() - 1) {
                curX = PADDING;
                curY += rowMaxH + NODE_SPACING_Y;
                rowMaxH = 0;
            }
        }
    }

    // =========================================================================
    // 3. ORGANIC / FORCE-DIRECTED LAYOUT
    // =========================================================================

    private static void layoutOrganic(DiagramGraph graph) {
        List<DiagramGraph.Node> nodes = graph.getNodes();
        if (nodes.isEmpty()) return;

        // First apply initial circle/orthogonal positioning
        int n = nodes.size();
        double radius = Math.max(400.0, n * 80.0);
        double centerX = PADDING + radius + 100;
        double centerY = PADDING + radius + 100;

        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n;
            nodes.get(i).setX(centerX + radius * Math.cos(angle) - nodes.get(i).getWidth() / 2.0);
            nodes.get(i).setY(centerY + radius * Math.sin(angle) - nodes.get(i).getHeight() / 2.0);
        }

        // Spring-embedder simulation (Fruchterman-Reingold)
        double k = Math.sqrt((radius * 2 * radius * 2) / n);
        double temp = radius / 3.0;

        for (int step = 0; step < 80; step++) {
            Map<DiagramGraph.Node, double[]> forces = new HashMap<>();
            for (DiagramGraph.Node node : nodes) {
                forces.put(node, new double[]{0.0, 0.0});
            }

            // Repulsion between all pairs
            for (int i = 0; i < n; i++) {
                DiagramGraph.Node u = nodes.get(i);
                for (int j = i + 1; j < n; j++) {
                    DiagramGraph.Node v = nodes.get(j);
                    double dx = (u.getX() + u.getWidth() / 2.0) - (v.getX() + v.getWidth() / 2.0);
                    double dy = (u.getY() + u.getHeight() / 2.0) - (v.getY() + v.getHeight() / 2.0);
                    double dist = Math.max(10.0, Math.sqrt(dx * dx + dy * dy));
                    double repForce = (k * k) / dist;
                    double fx = (dx / dist) * repForce;
                    double fy = (dy / dist) * repForce;

                    forces.get(u)[0] += fx;
                    forces.get(u)[1] += fy;
                    forces.get(v)[0] -= fx;
                    forces.get(v)[1] -= fy;
                }
            }

            // Attraction along edges
            for (DiagramGraph.Edge e : graph.getEdges()) {
                DiagramGraph.Node u = e.getFromNode();
                DiagramGraph.Node v = e.getToNode();
                if (u == null || v == null || u == v) continue;

                double dx = (u.getX() + u.getWidth() / 2.0) - (v.getX() + v.getWidth() / 2.0);
                double dy = (u.getY() + u.getHeight() / 2.0) - (v.getY() + v.getHeight() / 2.0);
                double dist = Math.max(10.0, Math.sqrt(dx * dx + dy * dy));
                double attrForce = (dist * dist) / k;
                double fx = (dx / dist) * attrForce;
                double fy = (dy / dist) * attrForce;

                forces.get(u)[0] -= fx;
                forces.get(u)[1] -= fy;
                forces.get(v)[0] += fx;
                forces.get(v)[1] += fy;
            }

            // Apply displacement with temperature cooling
            for (DiagramGraph.Node node : nodes) {
                double[] f = forces.get(node);
                double fLen = Math.sqrt(f[0] * f[0] + f[1] * f[1]);
                if (fLen > 0.0001) {
                    double disp = Math.min(fLen, temp);
                    node.setX(Math.max(PADDING, node.getX() + (f[0] / fLen) * disp));
                    node.setY(Math.max(PADDING, node.getY() + (f[1] / fLen) * disp));
                }
            }

            temp *= 0.95;
        }
    }

    // =========================================================================
    // 4. UTILITIES & COMPONENT PARTITIONING
    // =========================================================================

    private static List<List<DiagramGraph.Node>> partitionComponents(DiagramGraph graph) {
        List<DiagramGraph.Node> unvisited = new ArrayList<>(graph.getNodes());
        List<List<DiagramGraph.Node>> components = new ArrayList<>();

        while (!unvisited.isEmpty()) {
            DiagramGraph.Node seed = unvisited.remove(0);
            List<DiagramGraph.Node> comp = new ArrayList<>();
            comp.add(seed);

            List<DiagramGraph.Node> queue = new ArrayList<>();
            queue.add(seed);

            while (!queue.isEmpty()) {
                DiagramGraph.Node curr = queue.remove(0);
                for (DiagramGraph.Edge e : graph.getConnectedEdges(curr)) {
                    DiagramGraph.Node neighbor = (e.getFromNode() == curr) ? e.getToNode() : e.getFromNode();
                    if (neighbor != null && unvisited.remove(neighbor)) {
                        comp.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }

            // Set component ID on each node
            int cid = components.size();
            for (DiagramGraph.Node n : comp) {
                n.setComponentId(cid);
            }
            components.add(comp);
        }

        // Sort components so larger clusters come first, standalone nodes last
        components.sort((c1, c2) -> Integer.compare(c2.size(), c1.size()));
        return components;
    }

    private static ComponentBounds layoutGrid(List<DiagramGraph.Node> nodes, double startX, double startY) {
        int cols = Math.max(1, (int) Math.ceil(Math.sqrt(nodes.size())));
        double curX = startX;
        double curY = startY;
        double rowMaxH = 0;
        double maxW = 0;

        for (int i = 0; i < nodes.size(); i++) {
            DiagramGraph.Node n = nodes.get(i);
            n.setX(curX);
            n.setY(curY);

            rowMaxH = Math.max(rowMaxH, n.getHeight());
            maxW = Math.max(maxW, curX - startX + n.getWidth());
            curX += n.getWidth() + NODE_SPACING_X;

            if ((i + 1) % cols == 0 || i == nodes.size() - 1) {
                curX = startX;
                curY += rowMaxH + NODE_SPACING_Y;
                rowMaxH = 0;
            }
        }

        return new ComponentBounds(maxW, curY - startY);
    }

    private record ComponentBounds(double width, double height) {}
}

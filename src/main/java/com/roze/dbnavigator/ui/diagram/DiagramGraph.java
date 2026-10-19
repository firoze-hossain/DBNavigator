package com.roze.dbnavigator.ui.diagram;

import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.DbObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Graph data structure representing tables (nodes) and foreign key relationships (edges)
 * for DataGrip-style ER diagram visualization and algorithmic layout.
 */
public class DiagramGraph {

    public static class Node {
        private final DbObject table;
        private final List<MetadataService.ColumnInfo> columns;
        private double x;
        private double y;
        private double width = 230.0;
        private double height;
        private int rank = 0;
        private int orderInRank = 0;
        private int componentId = 0;

        public Node(DbObject table, List<MetadataService.ColumnInfo> columns) {
            this.table = table;
            this.columns = columns != null ? columns : Collections.emptyList();
            this.height = Math.max(60.0, 36.0 + this.columns.size() * 22.0 + 8.0);
        }

        public DbObject getTable() {
            return table;
        }

        public String getTableName() {
            return table != null ? table.getName() : "";
        }

        public List<MetadataService.ColumnInfo> getColumns() {
            return columns;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public double getWidth() {
            return width;
        }

        public void setWidth(double width) {
            this.width = width;
        }

        public double getHeight() {
            return height;
        }

        public void setHeight(double height) {
            this.height = height;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public int getOrderInRank() {
            return orderInRank;
        }

        public void setOrderInRank(int orderInRank) {
            this.orderInRank = orderInRank;
        }

        public int getComponentId() {
            return componentId;
        }

        public void setComponentId(int componentId) {
            this.componentId = componentId;
        }

        /**
         * Calculates vertical offset inside this table box for the given column.
         * Useful for exact column-to-column port anchoring.
         */
        public double getColumnYOffset(String columnName) {
            if (columnName == null) return height / 2.0;
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).name().equalsIgnoreCase(columnName)) {
                    // Header is ~36px, each column row is 22px high
                    return 36.0 + i * 22.0 + 11.0;
                }
            }
            return height / 2.0;
        }

        @Override
        public String toString() {
            return getTableName() + " (rank=" + rank + ", pos=(" + x + "," + y + "))";
        }
    }

    public static class Edge {
        private final Node fromNode;
        private final String fromColumn;
        private final Node toNode;
        private final String toColumn;
        private final String constraintName;
        private final boolean virtual;
        private boolean reversedForCycle = false;

        public Edge(Node fromNode, String fromColumn, Node toNode, String toColumn, String constraintName) {
            this(fromNode, fromColumn, toNode, toColumn, constraintName,
                    constraintName != null && constraintName.startsWith("inferred:"));
        }

        public Edge(Node fromNode, String fromColumn, Node toNode, String toColumn, String constraintName, boolean virtual) {
            this.fromNode = fromNode;
            this.fromColumn = fromColumn;
            this.toNode = toNode;
            this.toColumn = toColumn;
            this.constraintName = constraintName;
            this.virtual = virtual;
        }

        public boolean isVirtual() {
            return virtual;
        }

        public Node getFromNode() {
            return fromNode;
        }

        public String getFromColumn() {
            return fromColumn;
        }

        public Node getToNode() {
            return toNode;
        }

        public String getToColumn() {
            return toColumn;
        }

        public String getConstraintName() {
            return constraintName;
        }

        public boolean isReversedForCycle() {
            return reversedForCycle;
        }

        public void setReversedForCycle(boolean reversedForCycle) {
            this.reversedForCycle = reversedForCycle;
        }

        public boolean isSelfReference() {
            return fromNode != null && toNode != null && fromNode == toNode;
        }

        @Override
        public String toString() {
            return (fromNode != null ? fromNode.getTableName() : "?") + "." + fromColumn
                    + " -> "
                    + (toNode != null ? toNode.getTableName() : "?") + "." + toColumn;
        }
    }

    private final Map<String, Node> nodeMap = new LinkedHashMap<>();
    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();

    public void addNode(DbObject table, List<MetadataService.ColumnInfo> columns) {
        if (table == null) return;
        String key = normalizeKey(table.getName());
        if (!nodeMap.containsKey(key)) {
            Node node = new Node(table, columns);
            nodeMap.put(key, node);
            nodes.add(node);
        }
    }

    public void addEdge(String fromTableName, String fromColumn,
                        String toTableName, String toColumn, String constraintName) {
        Node fromNode = getNode(fromTableName);
        Node toNode = getNode(toTableName);
        if (fromNode != null && toNode != null) {
            Edge edge = new Edge(fromNode, fromColumn, toNode, toColumn, constraintName);
            edges.add(edge);
        }
    }

    public Node getNode(String tableName) {
        if (tableName == null) return null;
        return nodeMap.get(normalizeKey(tableName));
    }

    public List<Node> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public List<Edge> getOutgoingEdges(Node node) {
        List<Edge> result = new ArrayList<>();
        for (Edge e : edges) {
            if (e.getFromNode() == node) {
                result.add(e);
            }
        }
        return result;
    }

    public List<Edge> getIncomingEdges(Node node) {
        List<Edge> result = new ArrayList<>();
        for (Edge e : edges) {
            if (e.getToNode() == node) {
                result.add(e);
            }
        }
        return result;
    }

    public List<Edge> getConnectedEdges(Node node) {
        List<Edge> result = new ArrayList<>();
        for (Edge e : edges) {
            if (e.getFromNode() == node || e.getToNode() == node) {
                result.add(e);
            }
        }
        return result;
    }

    private static String normalizeKey(String tableName) {
        if (tableName == null) return "";
        // Strip catalog or schema prefix if present: "public.users" -> "users"
        int dot = tableName.lastIndexOf('.');
        String pure = dot >= 0 ? tableName.substring(dot + 1) : tableName;
        return pure.toLowerCase(Locale.ROOT);
    }
}

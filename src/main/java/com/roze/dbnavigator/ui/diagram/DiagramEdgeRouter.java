package com.roze.dbnavigator.ui.diagram;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes orthogonal (Manhattan 90° bends) routes for foreign key relationships,
 * anchoring directly to specific FK and PK column rows on the table boxes.
 */
public class DiagramEdgeRouter {

    public static class EdgeView extends Group {
        private final DiagramGraph.Edge edge;
        private final Polyline polyline;
        private final Polygon arrowHead;
        private final Label label;
        private final List<Point> routePoints;

        public EdgeView(DiagramGraph.Edge edge, List<Point> routePoints, Polyline polyline, Polygon arrowHead, Label label) {
            this.edge = edge;
            this.routePoints = routePoints;
            this.polyline = polyline;
            this.arrowHead = arrowHead;
            this.label = label;

            getChildren().addAll(polyline, arrowHead);
            if (label != null) {
                getChildren().add(label);
            }
        }

        public DiagramGraph.Edge getEdge() {
            return edge;
        }

        public Polyline getPolyline() {
            return polyline;
        }

        public Polygon getArrowHead() {
            return arrowHead;
        }

        public Label getLabel() {
            return label;
        }

        public List<Point> getRoutePoints() {
            return routePoints;
        }

        public void setHighlighted(boolean highlighted) {
            if (highlighted) {
                polyline.getStyleClass().remove("diagram-connector");
                if (!polyline.getStyleClass().contains("diagram-connector-selected")) {
                    polyline.getStyleClass().add("diagram-connector-selected");
                }
                arrowHead.getStyleClass().remove("diagram-connector-arrow");
                if (!arrowHead.getStyleClass().contains("diagram-connector-arrow-selected")) {
                    arrowHead.getStyleClass().add("diagram-connector-arrow-selected");
                }
                if (label != null) {
                    label.getStyleClass().add("diagram-connector-label-selected");
                }
                toFront();
            } else {
                polyline.getStyleClass().remove("diagram-connector-selected");
                if (!polyline.getStyleClass().contains("diagram-connector")) {
                    polyline.getStyleClass().add("diagram-connector");
                }
                arrowHead.getStyleClass().remove("diagram-connector-arrow-selected");
                if (!arrowHead.getStyleClass().contains("diagram-connector-arrow")) {
                    arrowHead.getStyleClass().add("diagram-connector-arrow");
                }
                if (label != null) {
                    label.getStyleClass().remove("diagram-connector-label-selected");
                }
            }
        }
    }

    public record Point(double x, double y) {}

    /**
     * Builds an orthogonal edge between two tables anchoring directly to the given columns.
     */
    public static EdgeView routeEdge(DiagramGraph.Edge edge, boolean showLabel) {
        DiagramGraph.Node fromNode = edge.getFromNode();
        DiagramGraph.Node toNode = edge.getToNode();

        double y1 = fromNode.getY() + fromNode.getColumnYOffset(edge.getFromColumn());
        double y2 = toNode.getY() + toNode.getColumnYOffset(edge.getToColumn());

        double fromLeft = fromNode.getX();
        double fromRight = fromNode.getX() + fromNode.getWidth();
        double toLeft = toNode.getX();
        double toRight = toNode.getX() + toNode.getWidth();

        List<Point> points = new ArrayList<>();
        double arrowDirX; // direction into target: 1 = pointing right, -1 = pointing left

        if (edge.isSelfReference()) {
            // Self-referencing loop on right side
            double x1 = fromRight;
            double loopX = fromRight + 32.0;
            points.add(new Point(x1, y1));
            points.add(new Point(loopX, y1));
            points.add(new Point(loopX, y2));
            points.add(new Point(x1, y2));
            arrowDirX = -1.0;
        } else if (fromRight + 30.0 < toLeft) {
            // Source is cleanly to the left of Target
            double x1 = fromRight;
            double x2 = toLeft;
            double midX = (x1 + x2) / 2.0;

            points.add(new Point(x1, y1));
            points.add(new Point(midX, y1));
            points.add(new Point(midX, y2));
            points.add(new Point(x2, y2));
            arrowDirX = 1.0;
        } else if (toRight + 30.0 < fromLeft) {
            // Source is cleanly to the right of Target
            double x1 = fromLeft;
            double x2 = toRight;
            double midX = (x1 + x2) / 2.0;

            points.add(new Point(x1, y1));
            points.add(new Point(midX, y1));
            points.add(new Point(midX, y2));
            points.add(new Point(x2, y2));
            arrowDirX = -1.0;
        } else {
            // Overlapping horizontally or vertically aligned: route through side channel
            boolean routeOnRight = (fromRight >= toRight);
            if (routeOnRight) {
                double x1 = fromRight;
                double x2 = toRight;
                double chanX = Math.max(x1, x2) + 32.0;

                points.add(new Point(x1, y1));
                points.add(new Point(chanX, y1));
                points.add(new Point(chanX, y2));
                points.add(new Point(x2, y2));
                arrowDirX = -1.0;
            } else {
                double x1 = fromLeft;
                double x2 = toLeft;
                double chanX = Math.min(x1, x2) - 32.0;

                points.add(new Point(x1, y1));
                points.add(new Point(chanX, y1));
                points.add(new Point(chanX, y2));
                points.add(new Point(x2, y2));
                arrowDirX = 1.0;
            }
        }

        // Create Polyline
        Polyline polyline = new Polyline();
        polyline.getStyleClass().add("diagram-connector");
        if (edge.isVirtual()) {
            polyline.getStyleClass().add("diagram-connector-virtual");
        }
        for (Point p : points) {
            polyline.getPoints().addAll(p.x(), p.y());
        }

        // Create Arrow Head at destination (last point)
        Point dest = points.get(points.size() - 1);
        Polygon arrow = new Polygon();
        arrow.getStyleClass().add("diagram-connector-arrow");
        if (edge.isVirtual()) {
            arrow.getStyleClass().add("diagram-connector-arrow-virtual");
        }
        double arrowSize = 6.0;

        if (arrowDirX > 0) {
            // Pointing to the right into the target left border
            arrow.getPoints().addAll(
                    dest.x(), dest.y(),
                    dest.x() - arrowSize * 1.5, dest.y() - arrowSize,
                    dest.x() - arrowSize * 1.5, dest.y() + arrowSize
            );
        } else {
            // Pointing to the left into the target right border
            arrow.getPoints().addAll(
                    dest.x(), dest.y(),
                    dest.x() + arrowSize * 1.5, dest.y() - arrowSize,
                    dest.x() + arrowSize * 1.5, dest.y() + arrowSize
            );
        }

        // Pill Label: fromCol:toCol positioned at the middle vertical or horizontal channel
        Label label = null;
        if (showLabel && points.size() >= 3) {
            label = new Label(edge.getFromColumn() + ":" + edge.getToColumn());
            label.getStyleClass().add("diagram-connector-label");
            if (edge.isVirtual()) {
                label.getStyleClass().add("diagram-connector-label-virtual");
            }
            label.setAlignment(Pos.CENTER);

            // Find longest segment to host the label
            double maxSegLen = 0;
            Point bestMid = null;
            for (int i = 0; i < points.size() - 1; i++) {
                Point pA = points.get(i);
                Point pB = points.get(i + 1);
                double len = Math.hypot(pB.x() - pA.x(), pB.y() - pA.y());
                if (len > maxSegLen) {
                    maxSegLen = len;
                    bestMid = new Point((pA.x() + pB.x()) / 2.0, (pA.y() + pB.y()) / 2.0);
                }
            }

            if (bestMid != null) {
                // Approximate label center offset
                double labelW = Math.max(50.0, (edge.getFromColumn().length() + edge.getToColumn().length()) * 6.5);
                label.setLayoutX(bestMid.x() - labelW / 2.0);
                label.setLayoutY(bestMid.y() - 9.0);
            }

            String tooltipText = edge.getFromNode().getTableName() + "." + edge.getFromColumn()
                    + " \u2192 "
                    + edge.getToNode().getTableName() + "." + edge.getToColumn()
                    + (edge.isVirtual() ? " (Inferred by column name)" : (edge.getConstraintName() != null ? " (" + edge.getConstraintName() + ")" : ""));
            Tooltip.install(label, new Tooltip(tooltipText));
            Tooltip.install(polyline, new Tooltip(tooltipText));
        }

        return new EdgeView(edge, points, polyline, arrow, label);
    }
}

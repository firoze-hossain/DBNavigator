package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;

/**
 * Renders authentic, high-fidelity vector icons matching DataGrip for all database engines.
 * Each database has its distinct brand shape and official color palette:
 * - PostgreSQL: Blue Elephant
 * - MySQL: Blue/Orange Dolphin
 * - MariaDB: Sea Lion / Seal
 * - MongoDB: Two-tone Green Leaf
 * - SQLite: Blue Feather
 * - SQL Server: Steel Database Cylinder with Red Server Badge
 * - Oracle: Iconic Red Oval "O"
 * - StratosDB: Isometric Violet Cube
 */
public final class DatabaseIcon {

    private DatabaseIcon() {}

    public static Node create(DatabaseType type) {
        return create(type, 14, true);
    }

    public static Node create(DatabaseType type, int size) {
        return create(type, size, true);
    }

    public static Node create(DatabaseType type, int size, boolean connected) {
        if (type == null) {
            type = DatabaseType.POSTGRESQL;
        }

        Group group = new Group();

        switch (type) {
            case POSTGRESQL -> {
                // PostgreSQL: Elephant head silhouette
                SVGPath elephant = new SVGPath();
                elephant.setContent("M 7.5,1.5 C 5.0,1.5 3.0,2.9 2.1,5.0 C 1.5,6.4 1.5,8.0 1.9,9.5 C 2.3,10.8 3.0,11.8 3.8,12.6 C 3.4,13.2 2.9,13.8 2.2,14.2 C 2.8,14.4 3.7,14.4 4.4,14.0 C 5.1,13.5 5.6,12.9 6.0,12.3 C 6.5,12.5 7.0,12.6 7.5,12.6 C 7.8,12.6 8.1,12.6 8.4,12.5 C 8.4,13.1 8.7,13.8 9.2,14.2 C 9.7,14.6 10.4,14.8 11.0,14.6 C 11.7,14.3 12.1,13.6 12.2,12.8 C 12.3,11.5 11.6,10.4 10.6,9.9 C 10.9,9.1 11.1,8.3 11.1,7.4 C 11.1,5.8 10.3,4.4 9.1,3.4 C 10.6,3.6 12.0,4.5 12.7,5.8 C 13.3,6.8 13.5,8.0 13.4,9.2 C 13.9,8.7 14.3,8.0 14.4,7.2 C 14.6,4.2 12.0,1.5 9.0,1.5 C 8.5,1.5 8.0,1.5 7.5,1.5 Z M 4.8,5.8 C 5.3,5.8 5.6,6.1 5.6,6.5 C 5.6,6.9 5.3,7.2 4.8,7.2 C 4.3,7.2 4.0,6.9 4.0,6.5 C 4.0,6.1 4.3,5.8 4.8,5.8 Z");
                elephant.setFill(Color.web("#336791"));
                group.getChildren().add(elephant);
            }
            case MYSQL -> {
                // MySQL: Dolphin body + orange dorsal fin
                SVGPath body = new SVGPath();
                body.setContent("M 14.5,4.0 C 13.3,2.8 11.5,2.5 9.8,2.8 C 8.2,3.2 6.8,4.3 5.8,5.6 C 4.6,7.2 3.9,9.0 3.2,10.8 C 2.5,12.2 1.6,13.4 0.5,14.2 C 1.6,14.2 2.8,13.8 3.8,13.1 C 4.8,12.3 5.5,11.2 6.2,10.0 C 7.0,8.6 8.0,7.3 9.4,6.4 C 10.5,5.6 11.9,5.2 13.2,5.5 C 13.9,5.7 14.5,6.1 15.0,6.7 C 15.2,5.8 15.1,4.7 14.5,4.0 Z M 1.2,13.0 C 1.9,12.5 2.5,11.6 2.8,10.7 C 2.2,11.2 1.6,12.0 1.2,13.0 Z");
                body.setFill(Color.web("#00758F"));

                SVGPath fin = new SVGPath();
                fin.setContent("M 9.5,3.6 C 9.0,1.8 7.5,0.8 6.0,1.2 C 6.8,2.2 7.9,3.1 9.5,3.6 Z");
                fin.setFill(Color.web("#F29111"));

                group.getChildren().addAll(body, fin);
            }
            case MARIADB -> {
                // MariaDB: Sea lion / seal
                SVGPath body = new SVGPath();
                body.setContent("M 14.2,5.0 C 13.5,3.4 12.0,2.3 10.2,2.0 C 8.5,1.7 6.8,2.2 5.4,3.2 C 3.8,4.5 2.7,6.3 1.9,8.2 C 1.3,9.9 0.9,11.8 0.6,13.6 C 2.0,13.3 3.3,12.4 4.3,11.3 C 5.3,10.2 6.1,8.9 7.0,7.7 C 8.0,6.4 9.4,5.4 11.0,5.0 C 12.0,4.7 13.0,4.9 13.7,5.5 C 14.0,5.4 14.1,5.2 14.2,5.0 Z M 11.3,3.6 C 11.8,3.6 12.2,3.9 12.2,4.4 C 12.2,4.9 11.8,5.2 11.3,5.2 C 10.8,5.2 10.4,4.9 10.4,4.4 C 10.4,3.9 10.8,3.6 11.3,3.6 Z");
                body.setFill(Color.web("#1F8BB6"));

                SVGPath flipper = new SVGPath();
                flipper.setContent("M 6.8,9.2 C 5.8,10.5 4.5,12.2 3.2,13.5 C 4.5,13.5 6.0,12.5 7.2,11.0 C 7.5,10.5 7.2,9.8 6.8,9.2 Z");
                flipper.setFill(Color.web("#003545"));

                group.getChildren().addAll(body, flipper);
            }
            case MONGODB -> {
                // MongoDB: Two-tone green leaf
                SVGPath leftLeaf = new SVGPath();
                leftLeaf.setContent("M 8.0,1.2 C 7.8,2.6 6.7,4.3 5.6,6.0 C 4.5,7.7 3.8,9.6 4.0,11.5 C 4.2,13.2 5.3,14.6 6.8,15.2 C 7.3,15.4 7.7,15.5 8.0,15.5 L 8.0,1.2 Z");
                leftLeaf.setFill(Color.web("#13AA52"));

                SVGPath rightLeaf = new SVGPath();
                rightLeaf.setContent("M 8.0,1.2 L 8.0,15.5 C 8.3,15.5 8.7,15.4 9.2,15.2 C 10.7,14.6 11.8,13.2 12.0,11.5 C 12.2,9.6 11.5,7.7 10.4,6.0 C 9.3,4.3 8.2,2.6 8.0,1.2 Z");
                rightLeaf.setFill(Color.web("#00684A"));

                SVGPath spine = new SVGPath();
                spine.setContent("M 7.7,6.8 L 8.3,6.8 L 8.3,14.0 L 7.7,14.0 Z");
                spine.setFill(Color.web("#80E0A7"));

                group.getChildren().addAll(leftLeaf, rightLeaf, spine);
            }
            case SQLITE -> {
                // SQLite: Feather quill
                SVGPath feather = new SVGPath();
                feather.setContent("M 14.0,1.2 C 11.5,1.7 8.6,4.0 6.8,6.4 C 5.4,8.2 4.3,10.3 3.3,12.4 C 2.8,13.3 2.3,14.1 1.6,14.8 C 2.7,14.5 3.6,13.7 4.3,12.9 C 5.4,11.8 6.2,10.6 7.0,9.4 C 8.3,7.6 10.0,5.9 11.9,4.7 C 12.8,4.1 13.9,3.5 14.7,2.6 C 15.0,2.3 15.0,1.6 14.7,1.3 C 14.5,1.2 14.2,1.2 14.0,1.2 Z");
                feather.setFill(Color.web("#0F80C1"));

                SVGPath shaft = new SVGPath();
                shaft.setContent("M 5.0,11.5 L 9.0,7.5 L 8.3,6.8 L 4.3,10.8 Z");
                shaft.setFill(Color.web("#5BC0DE"));

                group.getChildren().addAll(feather, shaft);
            }
            case SQLSERVER -> {
                // Microsoft SQL Server: Database cylinder + Microsoft red/orange grid badge
                SVGPath cylTop = new SVGPath();
                cylTop.setContent("M 2.5,4.0 C 2.5,2.6 5.0,1.5 8.0,1.5 C 11.0,1.5 13.5,2.6 13.5,4.0 C 13.5,5.4 11.0,6.5 8.0,6.5 C 5.0,6.5 2.5,5.4 2.5,4.0 Z");
                cylTop.setFill(Color.web("#B0B5BA"));

                SVGPath cylBody = new SVGPath();
                cylBody.setContent("M 2.5,4.0 L 2.5,12.0 C 2.5,13.4 5.0,14.5 8.0,14.5 C 11.0,14.5 13.5,13.4 13.5,12.0 L 13.5,4.0 C 13.5,5.0 11.0,6.0 8.0,6.0 C 5.0,6.0 2.5,5.0 2.5,4.0 Z");
                cylBody.setFill(Color.web("#8A9096"));

                SVGPath badge = new SVGPath();
                badge.setContent("M 7.5,7.5 L 10.0,7.5 L 10.0,10.0 L 7.5,10.0 Z M 10.8,7.5 L 13.3,7.5 L 13.3,10.0 L 10.8,10.0 Z M 7.5,10.8 L 10.0,10.8 L 10.0,13.3 L 7.5,13.3 Z M 10.8,10.8 L 13.3,10.8 L 13.3,13.3 L 10.8,13.3 Z");
                badge.setFill(Color.web("#CC292B"));

                group.getChildren().addAll(cylBody, cylTop, badge);
            }
            case ORACLE -> {
                // Oracle: Iconic red horizontal oval "O"
                SVGPath oval = new SVGPath();
                oval.setContent("M 8.0,2.8 C 3.5,2.8 1.0,5.1 1.0,8.0 C 1.0,10.9 3.5,13.2 8.0,13.2 C 12.5,13.2 15.0,10.9 15.0,8.0 C 15.0,5.1 12.5,2.8 8.0,2.8 Z M 8.0,5.2 C 11.2,5.2 12.8,6.4 12.8,8.0 C 12.8,9.6 11.2,10.8 8.0,10.8 C 4.8,10.8 3.2,9.6 3.2,8.0 C 3.2,6.4 4.8,5.2 8.0,5.2 Z");
                oval.setFill(Color.web("#EA1B22"));
                oval.setFillRule(FillRule.EVEN_ODD);
                group.getChildren().add(oval);
            }
            case STRATOSDB -> {
                // StratosDB: Isometric violet database cube
                SVGPath topFacet = new SVGPath();
                topFacet.setContent("M 8.0,1.8 L 13.2,4.8 L 8.0,7.8 L 2.8,4.8 Z");
                topFacet.setFill(Color.web("#A29BFE"));

                SVGPath leftFacet = new SVGPath();
                leftFacet.setContent("M 2.8,4.8 L 8.0,7.8 L 8.0,13.8 L 2.8,10.8 Z");
                leftFacet.setFill(Color.web("#5A45D4"));

                SVGPath rightFacet = new SVGPath();
                rightFacet.setContent("M 8.0,7.8 L 13.2,4.8 L 13.2,10.8 L 8.0,13.8 Z");
                rightFacet.setFill(Color.web("#6C5CE7"));

                group.getChildren().addAll(topFacet, leftFacet, rightFacet);
            }
        }

        double scale = (double) size / 16.0;
        group.setScaleX(scale);
        group.setScaleY(scale);

        StackPane container = new StackPane(group);
        container.setPrefSize(size, size);
        container.setMinSize(size, size);
        container.setMaxSize(size, size);
        container.setAlignment(Pos.CENTER);

        if (!connected) {
            container.setOpacity(0.48);
        }

        return container;
    }
}

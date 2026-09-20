package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.util.QueryExecutionResolver.CandidateOption;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.List;
import java.util.function.Consumer;

/**
 * In-editor floating popup for DataGrip's "Ask what to execute" option.
 * Displays candidate execution scopes (Statement, Subquery, Whole script, etc.)
 * with numeric accelerators (1, 2, 3) and keyboard navigation.
 */
public class QueryExecutionChooserPopup extends Popup {

    public QueryExecutionChooserPopup(List<CandidateOption> candidates, Consumer<CandidateOption> onChosen) {
        setAutoHide(true);
        setHideOnEscape(true);

        VBox container = new VBox(4);
        container.setPadding(new Insets(6, 6, 8, 6));
        container.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #4e5157; "
                + "-fx-border-width: 1px; -fx-border-radius: 6px; -fx-background-radius: 6px; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 4);");

        Label header = new Label("What to execute?");
        header.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #9da0a8; -fx-padding: 2 6 4 6;");

        ListView<CandidateOption> listView = new ListView<>();
        listView.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
        listView.setPrefWidth(460);
        listView.setPrefHeight(Math.min(260, candidates.size() * 46 + 10));
        listView.getItems().setAll(candidates);

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CandidateOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    int index = getIndex() + 1;
                    Label badge = new Label(String.valueOf(index));
                    badge.setStyle("-fx-background-color: #3574f0; -fx-text-fill: white; "
                            + "-fx-font-size: 10px; -fx-font-weight: bold; -fx-min-width: 18px; "
                            + "-fx-alignment: center; -fx-background-radius: 3px; -fx-padding: 1 4 1 4;");

                    Label titleLabel = new Label(item.title());
                    titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #dfe1e5;");

                    Label subLabel = new Label(item.subtitle());
                    subLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #8c8f96;");

                    VBox textBox = new VBox(1, titleLabel, subLabel);
                    HBox.setHgrow(textBox, Priority.ALWAYS);

                    HBox row = new HBox(8, badge, textBox);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(3, 4, 3, 4));

                    setGraphic(row);
                    setText(null);
                }
            }
        });

        listView.getSelectionModel().select(0);

        Runnable chooseCurrent = () -> {
            CandidateOption selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                hide();
                onChosen.accept(selected);
            }
        };

        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() >= 1) {
                chooseCurrent.run();
            }
        });

        listView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                chooseCurrent.run();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hide();
                e.consume();
            } else {
                String text = e.getText();
                if (text != null && text.matches("[1-9]")) {
                    int idx = Integer.parseInt(text) - 1;
                    if (idx >= 0 && idx < candidates.size()) {
                        hide();
                        onChosen.accept(candidates.get(idx));
                        e.consume();
                    }
                }
            }
        });

        container.getChildren().addAll(header, listView);
        getContent().add(container);
    }

    public void showAtNode(Node anchorNode, double screenX, double screenY) {
        show(anchorNode, screenX, screenY);
        // Focus the list view for immediate keyboard navigation
        Node content = getContent().get(0);
        if (content instanceof VBox vbox && vbox.getChildren().size() > 1) {
            vbox.getChildren().get(1).requestFocus();
        }
    }
}

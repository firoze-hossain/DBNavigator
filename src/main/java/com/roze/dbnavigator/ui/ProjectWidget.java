package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ProjectStore;
import com.roze.dbnavigator.model.Project;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.List;

/**
 * DataGrip-aligned Project Selector Widget:
 * Shows a letter badge (e.g. [ D ] for default, [ T ] for test2), the project name,
 * and a dropdown arrow. Clicking opens the DataGrip project popup menu.
 */
public class ProjectWidget extends Button {

    private final MainWindow mainWindow;
    private final StackPane badgePane = new StackPane();
    private final Label badgeLetter = new Label("D");
    private final Label nameLabel = new Label("default");
    private final Rectangle badgeBg = new Rectangle(18, 18);

    public ProjectWidget(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        initWidget();
    }

    private void initWidget() {
        getStyleClass().add("project-selector-widget");
        setStyle("-fx-background-color: #2b2d30; -fx-background-radius: 4; -fx-padding: 3 8 3 6; -fx-cursor: hand;");

        badgeBg.setArcWidth(5);
        badgeBg.setArcHeight(5);
        badgeBg.setFill(Color.web("#2e7d32"));

        badgeLetter.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        badgePane.getChildren().addAll(badgeBg, badgeLetter);

        nameLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label chevron = new Label();
        chevron.setGraphic(Icons.of(FontAwesomeSolid.CHEVRON_DOWN, "#868a91", 9));

        HBox layout = new HBox(6, badgePane, nameLabel, chevron);
        layout.setAlignment(Pos.CENTER_LEFT);
        setGraphic(layout);

        setOnMouseEntered(e -> setStyle("-fx-background-color: #35373c; -fx-background-radius: 4; -fx-padding: 3 8 3 6; -fx-cursor: hand;"));
        setOnMouseExited(e -> setStyle("-fx-background-color: #2b2d30; -fx-background-radius: 4; -fx-padding: 3 8 3 6; -fx-cursor: hand;"));

        setOnAction(e -> showProjectMenu());

        Project initial = (mainWindow != null && mainWindow.getProject() != null)
                ? mainWindow.getProject()
                : ProjectStore.getCurrentProject();
        updateProject(initial);
    }

    public void updateProject(Project project) {
        if (project == null) return;
        nameLabel.setText(project.getName());
        badgeLetter.setText(project.getInitialLetter());

        // Dynamic badge color based on initial letter
        int hash = Math.abs(project.getName().hashCode());
        String[] colors = {"#2e7d32", "#00796b", "#1565c0", "#6a1b9a", "#ad1457", "#d84315", "#455a64"};
        badgeBg.setFill(Color.web(colors[hash % colors.length]));
    }

    private void showProjectMenu() {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("project-context-menu");

        // 1. + New Project...
        MenuItem newProjectItem = new MenuItem("New Project…");
        newProjectItem.setGraphic(Icons.of(FontAwesomeSolid.PLUS, "#4a88c7", 12));
        newProjectItem.setOnAction(e -> mainWindow.createNewProjectFlow());
        menu.getItems().add(newProjectItem);

        // 2. Open...
        MenuItem openItem = new MenuItem("Open…");
        openItem.setGraphic(Icons.of(FontAwesomeSolid.FOLDER_OPEN, "#e0a44c", 12));
        openItem.setOnAction(e -> mainWindow.openProjectFlow());
        menu.getItems().add(openItem);

        menu.getItems().add(new SeparatorMenuItem());

        // 3. Open Projects section
        Label openHeader = new Label("Open Projects");
        openHeader.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 10px; -fx-padding: 2 12;");
        CustomMenuItem openHeaderItem = new CustomMenuItem(openHeader, false);
        menu.getItems().add(openHeaderItem);

        Project current = (mainWindow != null && mainWindow.getProject() != null)
                ? mainWindow.getProject()
                : ProjectStore.getCurrentProject();
        if (current != null) {
            MenuItem currentItem = buildProjectMenuItem(current, true);
            menu.getItems().add(currentItem);
        }

        // 4. Recent Projects section
        List<Project> recents = ProjectStore.getRecentProjects(current);
        if (!recents.isEmpty()) {
            menu.getItems().add(new SeparatorMenuItem());
            Label recentHeader = new Label("Recent Projects");
            recentHeader.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 10px; -fx-padding: 2 12;");
            CustomMenuItem recentHeaderItem = new CustomMenuItem(recentHeader, false);
            menu.getItems().add(recentHeaderItem);

            for (Project p : recents) {
                MenuItem item = buildProjectMenuItem(p, false);
                item.setOnAction(e -> mainWindow.switchProjectFlow(p));
                menu.getItems().add(item);
            }
        }

        menu.show(this, Side.BOTTOM, 0, 4);
    }

    private MenuItem buildProjectMenuItem(Project p, boolean isCurrent) {
        Rectangle bg = new Rectangle(18, 18);
        bg.setArcWidth(4);
        bg.setArcHeight(4);
        int hash = Math.abs(p.getName().hashCode());
        String[] colors = {"#2e7d32", "#00796b", "#1565c0", "#6a1b9a", "#ad1457", "#d84315", "#455a64"};
        bg.setFill(Color.web(colors[hash % colors.length]));

        Label letter = new Label(p.getInitialLetter());
        letter.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        StackPane icon = new StackPane(bg, letter);

        Label title = new Label(p.getName());
        title.setStyle(isCurrent ? "-fx-font-weight: bold; -fx-text-fill: -text;" : "-fx-text-fill: -text;");

        Label pathLbl = new Label(p.getDisplayPath());
        pathLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-dim;");

        VBox text = new VBox(1, title, pathLbl);
        HBox row = new HBox(8, icon, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 4, 2, 4));

        CustomMenuItem item = new CustomMenuItem(row, true);
        return item;
    }
}

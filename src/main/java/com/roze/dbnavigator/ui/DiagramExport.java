package com.roze.dbnavigator.ui;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.image.BufferedImage;
import java.io.File;

/** Exports any JavaFX Node (used here for diagram panes) to a PNG file. */
public final class DiagramExport {

    private DiagramExport() {}

    public static void exportToPng(Window owner, Node node, String defaultFileName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Diagram as PNG");
        chooser.setInitialFileName(defaultFileName.endsWith(".png") ? defaultFileName : defaultFileName + ".png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG image", "*.png"));
        File file = chooser.showSaveDialog(owner);
        if (file == null) return;

        try {
            SnapshotParameters params = new SnapshotParameters();
            WritableImage image = node.snapshot(params, null);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
            javax.imageio.ImageIO.write(bufferedImage, "png", file);
        } catch (Exception ex) {
            DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                    "Could not export diagram: " + ex.getMessage())).showAndWait();
        }
    }
}

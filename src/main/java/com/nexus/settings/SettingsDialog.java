package com.nexus.settings;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.Objects;
import java.util.Optional;

public final class SettingsDialog {

    private SettingsDialog() {
    }

    public static Optional<UiSettings> show(Window owner, UiSettings currentSettings) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setResizable(false);

        Label titleLabel = new Label("Settings");
        titleLabel.getStyleClass().add("settings-title");

        Label badgeLabel = new Label("Appearance");
        badgeLabel.getStyleClass().addAll("nexus-dialog-badge", "nexus-dialog-badge-info");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, titleLabel, spacer, badgeLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> themeBox = new ComboBox<>();
        themeBox.getItems().addAll("Dark", "Light");
        themeBox.setValue(currentSettings.getTheme());
        themeBox.getStyleClass().add("settings-combo");

        ComboBox<String> fontFamilyBox = new ComboBox<>();
        fontFamilyBox.getItems().addAll("Segoe UI", "Inter", "Verdana", "Georgia");
        fontFamilyBox.setValue(currentSettings.getFontFamily());
        fontFamilyBox.getStyleClass().add("settings-combo");

        ComboBox<Integer> fontSizeBox = new ComboBox<>();
        fontSizeBox.getItems().addAll(12, 13, 14, 15, 16, 18, 20);
        fontSizeBox.setValue(currentSettings.getFontSize());
        fontSizeBox.getStyleClass().add("settings-combo");

        ComboBox<String> accentBox = new ComboBox<>();
        accentBox.getItems().addAll("Blue", "Graphite", "Teal", "Plum");
        accentBox.setValue(currentSettings.getAccentColor());
        accentBox.getStyleClass().add("settings-combo");

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(12);
        form.getStyleClass().add("settings-form");

        form.add(label("Theme"), 0, 0);
        form.add(themeBox, 1, 0);

        form.add(label("Font"), 0, 1);
        form.add(fontFamilyBox, 1, 1);

        form.add(label("Font size"), 0, 2);
        form.add(fontSizeBox, 1, 2);

        form.add(label("Accent"), 0, 3);
        form.add(accentBox, 1, 3);

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().addAll("subtle-button", "nexus-dialog-button");

        Button applyButton = new Button("Apply");
        applyButton.getStyleClass().addAll("nexus-dialog-button", "nexus-dialog-primary-button");
        applyButton.setDefaultButton(true);
        cancelButton.setCancelButton(true);

        final UiSettings[] result = new UiSettings[1];

        cancelButton.setOnAction(event -> dialog.close());
        applyButton.setOnAction(event -> {
            result[0] = new UiSettings(
                    themeBox.getValue(),
                    fontFamilyBox.getValue(),
                    fontSizeBox.getValue(),
                    accentBox.getValue()
            );
            dialog.close();
        });

        HBox actions = new HBox(10, cancelButton, applyButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(18, header, form, actions);
        card.getStyleClass().add("settings-card");
        card.setMaxWidth(440);
        card.setEffect(new DropShadow(30, Color.rgb(0, 0, 0, 0.35)));

        StackPane root = new StackPane(card);
        root.setPadding(new Insets(18));
        root.getStyleClass().add("settings-dialog-root");
        root.getStyleClass().add(currentSettings.themeStyleClass());
        root.setStyle(currentSettings.toInlineStyle());

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        SettingsDialog.class.getResource("/css/styles.css"),
                        "styles.css not found"
                ).toExternalForm()
        );

        dialog.setScene(scene);
        dialog.showAndWait();

        return Optional.ofNullable(result[0]);
    }

    private static Label label(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("settings-label");
        return label;
    }
}
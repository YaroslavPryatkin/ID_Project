package com.postgres_gui.ui.components;

import com.postgres_gui.ui.layout.FunctionScreenPalette;
import com.postgres_gui.ui.layout.UILayoutSettings;
import com.postgres_gui.util.ColorUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

public final class UiFactory {
    private UiFactory() {
    }

    public static void hideScrollBars(ScrollPane scrollPane) {
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-background: transparent;"
                        + "-fx-padding: 0;"
        );
        scrollPane.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                applyHiddenScrollBars(scrollPane);
            }
        });
    }

    public static void applyHiddenScrollBars(ScrollPane scrollPane) {
        scrollPane.lookupAll(".scroll-bar").forEach(node -> {
            node.setVisible(false);
            node.setManaged(false);
        });
    }

    public static String panelBorderStyle(String borderColor, boolean top, boolean right, boolean bottom, boolean left) {
        double w = UILayoutSettings.PANEL_BORDER_WIDTH;
        return String.format(
                "-fx-border-color: %s; -fx-border-width: %.0f %.0f %.0f %.0f;",
                borderColor,
                top ? w : 0,
                right ? w : 0,
                bottom ? w : 0,
                left ? w : 0
        );
    }

    public static Button createNavScreenButton(String text, String backgroundColor, String hoverColor) {
        Button button = createBarButton(
                text,
                backgroundColor,
                hoverColor,
                UILayoutSettings.NAV_BUTTON_TEXT_COLOR,
                UILayoutSettings.NAV_SCREEN_BUTTON_FONT,
                UILayoutSettings.NAV_BUTTON_CORNER_RADIUS,
                Pos.CENTER,
                true
        );
        attachHoverBackground(button, backgroundColor, hoverColor);
        return button;
    }

    public static Button createStartScreenListButton(String text, String backgroundColor) {
        String hover = FunctionScreenPalette.toHex(
                ColorUtils.darken(Color.web(backgroundColor), UILayoutSettings.FUNCTION_COLOR_LIST_BUTTON_HOVER_DARKEN));
        Button button = createRoundedButton(
                text,
                backgroundColor,
                hover,
                UILayoutSettings.NAV_BUTTON_TEXT_COLOR,
                UILayoutSettings.NAV_SCREEN_BUTTON_FONT,
                UILayoutSettings.START_SCREEN_LIST_BUTTON_CORNER_RADIUS,
                Pos.CENTER,
                true
        );
        attachHoverBackground(button, backgroundColor, hover);
        return button;
    }

    public static Button createFunctionListButton(String text, String normalColor, String hoverColor) {
        Button button = createRoundedButton(
                text,
                normalColor,
                hoverColor,
                UILayoutSettings.FUNCTION_LIST_BUTTON_TEXT_COLOR,
                UILayoutSettings.FUNCTION_LIST_BUTTON_FONT,
                UILayoutSettings.FUNCTION_LIST_BUTTON_CORNER_RADIUS,
                Pos.CENTER_LEFT,
                true
        );
        attachHoverBackground(button, normalColor, hoverColor);
        return button;
    }

    public static Button createToolbarButton(String text, String normalColor, String hoverColor) {
        Button button = createBarButton(
                text,
                normalColor,
                hoverColor,
                UILayoutSettings.TOOLBAR_BUTTON_TEXT_COLOR,
                UILayoutSettings.BOTTOM_TOOLBAR_BUTTON_FONT,
                UILayoutSettings.BOTTOM_TOOLBAR_BUTTON_CORNER_RADIUS,
                Pos.CENTER,
                false
        );
        attachHoverBackground(button, normalColor, hoverColor);
        return button;
    }

    public static Button createRoundedButton(
            String text,
            String normalBackground,
            String hoverBackground,
            String textColor,
            javafx.scene.text.Font font,
            double cornerRadius,
            Pos alignment,
            boolean wrapText
    ) {
        return createRoundedButton(
                text, normalBackground, hoverBackground, textColor, font, cornerRadius, alignment, wrapText,
                UILayoutSettings.LIST_BUTTON_MIN_HEIGHT,
                UILayoutSettings.LIST_BUTTON_PADDING_VERTICAL,
                UILayoutSettings.LIST_BUTTON_PADDING_HORIZONTAL
        );
    }

    public static Button createBarButton(
            String text,
            String normalBackground,
            String hoverBackground,
            String textColor,
            javafx.scene.text.Font font,
            double cornerRadius,
            Pos alignment,
            boolean wrapText
    ) {
        return createRoundedButton(
                text, normalBackground, hoverBackground, textColor, font, cornerRadius, alignment, wrapText,
                UILayoutSettings.BAR_BUTTON_MIN_HEIGHT,
                UILayoutSettings.BAR_BUTTON_PADDING_VERTICAL,
                UILayoutSettings.BAR_BUTTON_PADDING_HORIZONTAL
        );
    }

    private static Button createRoundedButton(
            String text,
            String normalBackground,
            String hoverBackground,
            String textColor,
            javafx.scene.text.Font font,
            double cornerRadius,
            Pos alignment,
            boolean wrapText,
            double minHeight,
            double paddingVertical,
            double paddingHorizontal
    ) {
        Button button = new Button(text);
        button.setMnemonicParsing(false);
        button.setFont(font);
        button.setWrapText(wrapText);
        button.setTextAlignment(alignment == Pos.CENTER ? TextAlignment.CENTER : TextAlignment.LEFT);
        button.setAlignment(alignment);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(minHeight);
        button.setPrefHeight(Region.USE_COMPUTED_SIZE);
        button.setStyle(buttonStyle(
                normalBackground, textColor, cornerRadius, alignment, paddingVertical, paddingHorizontal));
        return button;
    }

    public static void attachHoverBackground(Button button, String normalBackground, String hoverBackground) {
        String textColor = extractTextFill(button.getStyle());
        double radius = extractRadius(button.getStyle());
        Pos alignment = button.getAlignment();
        String normalStyle = button.getStyle();
        double padV = extractPaddingVertical(normalStyle);
        double padH = extractPaddingHorizontal(normalStyle);
        String hoverStyle = buttonStyle(hoverBackground, textColor, radius, alignment, padV, padH);
        button.setOnMouseEntered(e -> {
            if (!button.isDisabled()) {
                button.setStyle(hoverStyle);
            }
        });
        button.setOnMouseExited(e -> button.setStyle(normalStyle));
    }

    public static String buttonStyle(
            String background,
            String textColor,
            double cornerRadius,
            Pos alignment,
            double paddingVertical,
            double paddingHorizontal
    ) {
        String alignStyle = alignment == Pos.CENTER ? "-fx-alignment: CENTER;" : "-fx-alignment: CENTER-LEFT;";
        return String.format(
                "%s -fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: %.0f; "
                        + "-fx-border-color: %s; -fx-border-width: %.0f; -fx-border-radius: %.0f; "
                        + "-fx-padding: %.0f %.0f; -fx-cursor: hand;",
                alignStyle,
                background,
                textColor,
                cornerRadius,
                UILayoutSettings.BUTTON_BORDER_COLOR,
                UILayoutSettings.BUTTON_BORDER_WIDTH,
                cornerRadius,
                paddingVertical,
                paddingHorizontal
        );
    }

    private static double extractPaddingVertical(String style) {
        return extractPaddingAxis(style, 0, UILayoutSettings.LIST_BUTTON_PADDING_VERTICAL);
    }

    private static double extractPaddingHorizontal(String style) {
        return extractPaddingAxis(style, 1, UILayoutSettings.LIST_BUTTON_PADDING_HORIZONTAL);
    }

    /**
     * CSS {@code -fx-padding}: 1 value — all sides; 2 — vertical horizontal; 4 — top right bottom left.
     */
    private static double extractPaddingAxis(String style, int axisIndex, double fallback) {
        int idx = style.indexOf("-fx-padding:");
        if (idx < 0) {
            return fallback;
        }
        int end = style.indexOf(';', idx);
        String[] parts = style.substring(idx + 12, end).trim().split("\\s+");
        if (parts.length == 0) {
            return fallback;
        }
        if (parts.length == 1) {
            return Double.parseDouble(parts[0]);
        }
        int safeIndex = Math.min(axisIndex, parts.length - 1);
        return Double.parseDouble(parts[safeIndex]);
    }

    private static String extractTextFill(String style) {
        int idx = style.indexOf("-fx-text-fill:");
        if (idx < 0) {
            return UILayoutSettings.FONT_COLOR_PRIMARY;
        }
        int end = style.indexOf(';', idx);
        return style.substring(idx + 15, end).trim();
    }

    private static double extractRadius(String style) {
        int idx = style.indexOf("-fx-background-radius:");
        if (idx < 0) {
            return 14;
        }
        int end = style.indexOf(';', idx);
        return Double.parseDouble(style.substring(idx + 22, end).trim());
    }

    public static VBox createLabeledInput(String labelText, TextField field, boolean invalid) {
        return createLabeledInput(labelText, field, invalid, UILayoutSettings.START_SCREEN_FORM_WIDTH);
    }

    public static VBox createLabeledInput(String labelText, TextField field, boolean invalid, double width) {
        Label plate = createInputLabelPlate(labelText);
        styleInputField(field, invalid, width);
        VBox block = new VBox(0, plate, field);
        if (width > 0 && width < Double.MAX_VALUE) {
            block.setMaxWidth(width);
            block.setPrefWidth(width);
        } else {
            block.setMaxWidth(Double.MAX_VALUE);
        }
        return block;
    }

    public static VBox createLabeledInputWithDroplist(String labelText, TextField textField, ScrollPane droplist, double width) {
        VBox block = createLabeledInput(labelText, textField, false, width);
        if (droplist != null) {
            droplist.minWidthProperty().bind(textField.widthProperty());
            droplist.prefWidthProperty().bind(textField.widthProperty());
            droplist.maxWidthProperty().bind(textField.widthProperty());

            block.getChildren().add(droplist);
        }
        return block;
    }

    public static Label createInputLabelPlate(String labelText) {
        Label plate = new Label(labelText);
        plate.setFont(UILayoutSettings.INPUT_LABEL_FONT);
        plate.setMaxWidth(Double.MAX_VALUE);
        plate.setAlignment(Pos.CENTER);
        plate.setStyle(String.format(
                "-fx-text-fill: %s; -fx-background-color: %s; -fx-background-radius: %.0f; "
                        + "-fx-padding: 4 8; -fx-alignment: center; "
                        + "-fx-border-color: %s; -fx-border-width: %.0f;",
                UILayoutSettings.INPUT_LABEL_TEXT_COLOR,
                UILayoutSettings.INPUT_LABEL_PLATE_BACKGROUND,
                UILayoutSettings.INPUT_LABEL_PLATE_CORNER_RADIUS,
                UILayoutSettings.BUTTON_BORDER_COLOR,
                UILayoutSettings.BUTTON_BORDER_WIDTH
        ));
        return plate;
    }

    public static TextField createCenteredInputField() {
        return createCenteredInputField(UILayoutSettings.START_SCREEN_FORM_WIDTH);
    }

    public static TextField createCenteredInputField(double width) {
        TextField field = new TextField();
        styleInputField(field, false, width);
        return field;
    }

    public static PasswordField createCenteredPasswordField() {
        return createCenteredPasswordField(UILayoutSettings.START_SCREEN_FORM_WIDTH);
    }

    public static PasswordField createCenteredPasswordField(double width) {
        PasswordField field = new PasswordField();
        stylePasswordField(field, false, width);
        return field;
    }

    public static TextField createFullWidthInputField() {
        TextField field = new TextField();
        styleInputField(field, false, -1);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    public static void styleInputField(TextField field, boolean invalid) {
        styleInputField(field, invalid, UILayoutSettings.START_SCREEN_FORM_WIDTH);
    }

    public static void styleInputField(TextField field, boolean invalid, double width) {
        field.setFont(UILayoutSettings.INPUT_FIELD_FONT);
        field.setAlignment(Pos.CENTER);
        field.setPrefHeight(UILayoutSettings.INPUT_FIELD_HEIGHT);
        field.setMinHeight(UILayoutSettings.INPUT_FIELD_HEIGHT);
        if (width > 0) {
            field.setMaxWidth(width);
            field.setPrefWidth(width);
        } else {
            field.setMaxWidth(Double.MAX_VALUE);
            field.setPrefWidth(Region.USE_COMPUTED_SIZE);
        }
        String border = invalid
                ? UILayoutSettings.INPUT_FIELD_ERROR_BORDER_COLOR
                : UILayoutSettings.INPUT_FIELD_BORDER_COLOR;
        field.setStyle(String.format(
                "-fx-text-fill: %s; -fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.0f; "
                        + "-fx-background-radius: %.0f; -fx-border-radius: %.0f;",
                UILayoutSettings.INPUT_FIELD_TEXT_COLOR,
                UILayoutSettings.INPUT_FIELD_BACKGROUND,
                border,
                UILayoutSettings.BUTTON_BORDER_WIDTH,
                UILayoutSettings.INPUT_FIELD_CORNER_RADIUS,
                UILayoutSettings.INPUT_FIELD_CORNER_RADIUS
        ));
    }

    public static void stylePasswordField(PasswordField field, boolean invalid) {
        stylePasswordField(field, invalid, UILayoutSettings.START_SCREEN_FORM_WIDTH);
    }

    public static void stylePasswordField(PasswordField field, boolean invalid, double width) {
        field.setFont(UILayoutSettings.INPUT_FIELD_FONT);
        field.setAlignment(Pos.CENTER);
        field.setPrefHeight(UILayoutSettings.INPUT_FIELD_HEIGHT);
        field.setMinHeight(UILayoutSettings.INPUT_FIELD_HEIGHT);
        if (width > 0) {
            field.setMaxWidth(width);
            field.setPrefWidth(width);
        } else {
            field.setMaxWidth(Double.MAX_VALUE);
            field.setPrefWidth(Region.USE_COMPUTED_SIZE);
        }
        String border = invalid
                ? UILayoutSettings.INPUT_FIELD_ERROR_BORDER_COLOR
                : UILayoutSettings.INPUT_FIELD_BORDER_COLOR;
        field.setStyle(String.format(
                "-fx-text-fill: %s; -fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.0f; "
                        + "-fx-background-radius: %.0f; -fx-border-radius: %.0f;",
                UILayoutSettings.INPUT_FIELD_TEXT_COLOR,
                UILayoutSettings.INPUT_FIELD_BACKGROUND,
                border,
                UILayoutSettings.BUTTON_BORDER_WIDTH,
                UILayoutSettings.INPUT_FIELD_CORNER_RADIUS,
                UILayoutSettings.INPUT_FIELD_CORNER_RADIUS
        ));
    }

    public static Label createErrorPlate(String message) {
        Label label = new Label(message);
        label.setFont(UILayoutSettings.ERROR_MESSAGE_FONT);
        label.setMaxWidth(UILayoutSettings.START_SCREEN_FORM_WIDTH);
        label.setAlignment(Pos.CENTER);
        label.setWrapText(true);
        label.setStyle(String.format(
                "-fx-text-fill: %s; -fx-background-color: %s; -fx-background-radius: %.0f; -fx-padding: 6 12;",
                UILayoutSettings.ERROR_MESSAGE_TEXT_COLOR,
                UILayoutSettings.ERROR_MESSAGE_PLATE_BACKGROUND,
                UILayoutSettings.ERROR_MESSAGE_PLATE_CORNER_RADIUS
        ));
        return label;
    }

    public static Label createZeroArgumentPlate(String message) {
        Label label = new Label(message);
        label.setFont(UILayoutSettings.ZERO_ARGUMENT_FONT);
        label.setAlignment(Pos.CENTER);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle(String.format(
                "-fx-text-fill: %s; -fx-background-color: %s; -fx-background-radius: %.0f; "
                        + "-fx-padding: %.0f; -fx-alignment: center; "
                        + "-fx-border-color: %s; -fx-border-width: %.0f;",
                UILayoutSettings.ZERO_ARGUMENT_TEXT_COLOR,
                UILayoutSettings.ZERO_ARGUMENT_PLATE_BACKGROUND,
                UILayoutSettings.ZERO_ARGUMENT_PLATE_CORNER_RADIUS,
                UILayoutSettings.ZERO_ARGUMENT_PLATE_PADDING,
                UILayoutSettings.BUTTON_BORDER_COLOR,
                UILayoutSettings.BUTTON_BORDER_WIDTH
        ));
        return label;
    }

    public static Label createStartScreenTitle(String title) {
        Label label = new Label(title);
        label.setFont(UILayoutSettings.START_SCREEN_TITLE_FONT);
        label.setAlignment(Pos.CENTER);
        label.setWrapText(true);
        label.setMaxWidth(UILayoutSettings.START_SCREEN_FORM_WIDTH);
        label.setStyle("-fx-text-fill: " + UILayoutSettings.START_SCREEN_TITLE_TEXT_COLOR + ";");
        return label;
    }
}

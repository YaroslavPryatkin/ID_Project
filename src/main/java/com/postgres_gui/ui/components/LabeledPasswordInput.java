package com.postgres_gui.ui.components;

import com.postgres_gui.ui.layout.UILayoutSettings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Поле пароля с плашкой-заголовком и переключателем «Show password» внутри блока.
 */
public final class LabeledPasswordInput {
    private final PasswordField passwordField;
    private final TextField visibleField;
    private final CheckBox showPasswordCheck;
    private final VBox root;
    private final SimpleStringProperty text = new SimpleStringProperty("");
    private boolean syncing;

    public LabeledPasswordInput(String labelText, boolean invalid) {
        this(labelText, invalid, UILayoutSettings.START_SCREEN_FORM_WIDTH);
    }

    public LabeledPasswordInput(String labelText, boolean invalid, double width) {
        passwordField = UiFactory.createCenteredPasswordField(width);
        visibleField = UiFactory.createCenteredInputField(width);
        visibleField.setVisible(false);
        visibleField.setManaged(false);

        showPasswordCheck = new CheckBox(UILayoutSettings.PASSWORD_SHOW_CHECKBOX_LABEL);
        showPasswordCheck.setFont(UILayoutSettings.CHECKBOX_FONT);
        showPasswordCheck.setStyle("-fx-text-fill: " + UILayoutSettings.CHECKBOX_TEXT_COLOR + ";");
        showPasswordCheck.setPadding(new Insets(UILayoutSettings.PASSWORD_SHOW_CHECKBOX_TOP_PADDING, 0, 0, 0));

        StackPane fieldStack = new StackPane(passwordField, visibleField);
        fieldStack.setMaxWidth(width);
        fieldStack.setPrefWidth(width);

        var plate = UiFactory.createInputLabelPlate(labelText);
        root = new VBox(0, plate, fieldStack, showPasswordCheck);
        root.setMaxWidth(width);
        root.setPrefWidth(width);

        wireSync();
        showPasswordCheck.selectedProperty().addListener((obs, was, show) -> toggleVisibility(show));
        setInvalid(invalid);
    }

    private void wireSync() {
        passwordField.textProperty().addListener((obs, o, n) -> {
            if (!syncing) {
                syncing = true;
                text.set(n);
                if (visibleField.isVisible()) {
                    visibleField.setText(n);
                }
                syncing = false;
            }
        });
        visibleField.textProperty().addListener((obs, o, n) -> {
            if (!syncing && visibleField.isVisible()) {
                syncing = true;
                text.set(n);
                passwordField.setText(n);
                syncing = false;
            }
        });
    }

    private void toggleVisibility(boolean show) {
        visibleField.setText(passwordField.getText());
        passwordField.setVisible(!show);
        passwordField.setManaged(!show);
        visibleField.setVisible(show);
        visibleField.setManaged(show);
        if (show) {
            visibleField.requestFocus();
            visibleField.positionCaret(visibleField.getText().length());
        } else {
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    public VBox getRoot() {
        return root;
    }

    public StringProperty textProperty() {
        return text;
    }

    public String getText() {
        return showPasswordCheck.isSelected() ? visibleField.getText() : passwordField.getText();
    }

    public void setText(String value) {
        syncing = true;
        passwordField.setText(value);
        visibleField.setText(value);
        text.set(value);
        syncing = false;
    }

    public void setInvalid(boolean invalid) {
        UiFactory.stylePasswordField(passwordField, invalid, UILayoutSettings.START_SCREEN_FORM_WIDTH);
        UiFactory.styleInputField(visibleField, invalid, UILayoutSettings.START_SCREEN_FORM_WIDTH);
    }
}

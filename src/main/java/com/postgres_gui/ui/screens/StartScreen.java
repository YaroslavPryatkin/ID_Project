package com.postgres_gui.ui.screens;

import com.postgres_gui.config.Config;
import com.postgres_gui.config.ScreenConfig;
import com.postgres_gui.ui.AppWindow;
import com.postgres_gui.ui.components.LabeledPasswordInput;
import com.postgres_gui.ui.components.UiFactory;
import com.postgres_gui.ui.layout.UILayoutSettings;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class StartScreen extends Screen {
    private static final String TITLE = "PostgreSQL Function GUI";
    private static final String SUBTITLE = "Created by Longseax";
    private static final String INVALID_MSG = "Invalid connection data";

    private final TextField urlField = UiFactory.createCenteredInputField();
    private final TextField userField = UiFactory.createCenteredInputField();
    private final TextField databaseField = UiFactory.createCenteredInputField();
    private final LabeledPasswordInput passwordInput = new LabeledPasswordInput("Password", false);

    private final VBox urlBlock = UiFactory.createLabeledInput("URL", urlField, false);
    private final VBox userBlock = UiFactory.createLabeledInput("User", userField, false);
    private final VBox databaseBlock = UiFactory.createLabeledInput("Database", databaseField, false);

    private final Button connectButton = new Button("Connect");
    private final Button resetButton = new Button("Reset");
    private final Label errorPlate = UiFactory.createErrorPlate(INVALID_MSG);
    private final VBox screenButtonsBox = new VBox(UILayoutSettings.START_SCREEN_LIST_BUTTON_SPACING);
    private final ScrollPane screenListScroll = new ScrollPane();

    private String savedUrl;
    private String savedUser;
    private String savedDatabase;
    private String savedPassword;
    private boolean credentialsInvalid;

    public StartScreen(AppWindow app, Image icon) {
        super(app);
        buildUi(icon);
        setupBindings();
        loadFieldsFromConfig();
    }

    private void buildUi(Image icon) {
        setStyle("-fx-background-color: " + ScreenConfig.backgroundColor + ";");

        if (icon != null) {
            ImageView background = new ImageView(icon);
            background.setOpacity(UILayoutSettings.BACKGROUND_ICON_OPACITY);
            background.setPreserveRatio(true);
            double factor = UILayoutSettings.BACKGROUND_ICON_SIZE_FACTOR;
            background.fitWidthProperty().bind(widthProperty().multiply(factor));
            background.fitHeightProperty().bind(heightProperty().multiply(factor));
            StackPane.setAlignment(background, Pos.CENTER);
            getChildren().add(background);
        }

        Label title = UiFactory.createStartScreenTitle(TITLE);

        Label subtitle = new Label(SUBTITLE);
        subtitle.setFont(UILayoutSettings.START_SCREEN_SUBTITLE_FONT);
        subtitle.setStyle("-fx-text-fill: " + UILayoutSettings.START_SCREEN_SUBTITLE_TEXT_COLOR + ";");
        subtitle.setMaxWidth(UILayoutSettings.START_SCREEN_FORM_WIDTH);
        subtitle.setAlignment(Pos.CENTER);

        errorPlate.setVisible(false);
        errorPlate.setManaged(false);

        HBox actionRow = new HBox(10, connectButton, resetButton);
        actionRow.setAlignment(Pos.CENTER);

        screenButtonsBox.setFillWidth(true);
        screenButtonsBox.setMaxWidth(UILayoutSettings.START_SCREEN_FORM_WIDTH);
        screenListScroll.setContent(screenButtonsBox);
        screenListScroll.setFitToWidth(true);
        screenListScroll.setMaxWidth(UILayoutSettings.START_SCREEN_FORM_WIDTH);
        screenListScroll.setPrefWidth(UILayoutSettings.START_SCREEN_FORM_WIDTH);
        UiFactory.hideScrollBars(screenListScroll);

        screenListScroll.setVisible(false);
        screenListScroll.setManaged(false);

        connectButton.setOnAction(e -> applyCredentials());
        resetButton.setOnAction(e -> resetToSaved());

        VBox formBlock = new VBox(UILayoutSettings.INPUT_BLOCK_VERTICAL_SPACING);
        formBlock.setAlignment(Pos.TOP_CENTER);
        formBlock.setMaxWidth(UILayoutSettings.START_SCREEN_FORM_WIDTH);
        formBlock.getChildren().addAll(
                errorPlate,
                urlBlock, userBlock, databaseBlock, passwordInput.getRoot(),
                actionRow
        );

        VBox headerBlock = new VBox(
                UILayoutSettings.START_SCREEN_TITLE_TO_SUBTITLE_SPACING,
                title, subtitle
        );
        headerBlock.setAlignment(Pos.CENTER);

        Region topSpacer = new Region();
        topSpacer.minHeightProperty().bind(
                heightProperty().multiply(UILayoutSettings.START_SCREEN_TITLE_Y_FRACTION)
        );

        VBox content = new VBox(
                UILayoutSettings.START_SCREEN_SUBTITLE_TO_FORM_BLOCK_SPACING,
                headerBlock, formBlock
        );
        content.setAlignment(Pos.TOP_CENTER);
        content.setMaxWidth(UILayoutSettings.START_SCREEN_FORM_WIDTH);

        VBox rootColumn = new VBox(topSpacer, content, screenListScroll);
        rootColumn.setAlignment(Pos.TOP_CENTER);
        rootColumn.setFillWidth(true);
        rootColumn.setPadding(new Insets(
                0, 24,
                UILayoutSettings.START_SCREEN_LIST_BOTTOM_PADDING, 24
        ));
        rootColumn.prefHeightProperty().bind(heightProperty());
        VBox.setVgrow(screenListScroll, Priority.ALWAYS);

        getChildren().add(rootColumn);
        StackPane.setAlignment(rootColumn, Pos.TOP_CENTER);
    }

    private void setupBindings() {
        var changed = Bindings.createBooleanBinding(
                this::isCredentialsChanged,
                urlField.textProperty(),
                userField.textProperty(),
                databaseField.textProperty(),
                passwordInput.textProperty()
        );

        connectButton.visibleProperty().bind(changed);
        connectButton.managedProperty().bind(changed);
        resetButton.visibleProperty().bind(changed);
        resetButton.managedProperty().bind(changed);

        changed.addListener((obs, wasChanged, nowChanged) -> {
            if (nowChanged && credentialsInvalid) {
                clearInvalidState();
            }
        });
    }

    private boolean isCredentialsChanged() {
        return !urlField.getText().equals(savedUrl)
                || !userField.getText().equals(savedUser)
                || !databaseField.getText().equals(savedDatabase)
                || !passwordInput.getText().equals(savedPassword);
    }

    private void loadFieldsFromConfig() {
        configToSaved();
        savedToCur();
    }

    // ИСПРАВЛЕНИЕ 1: Нормализация null значений. Заменяем null на "", чтобы биндинги работали корректно.
    private Config configToSaved() {
        Config config = app.getConfig();
        savedUrl = config.getUrl() != null ? config.getUrl() : "";
        savedUser = config.getUser() != null ? config.getUser() : "";
        savedDatabase = config.getDatabase() != null ? config.getDatabase() : "";
        savedPassword = config.getPassword() != null ? config.getPassword() : "";
        return config;
    }

    @Override
    public void onShow() {
        savedToCur();
        Platform.runLater(() -> {
            if (tryConnectWithCurrentCredentials()) {
                clearInvalidState();
                refreshScreenList();
            } else {
                setInvalidState();
                hideScreenList();
            }
        });
    }

    private void refreshScreenList() {
        screenButtonsBox.getChildren().clear();
        for (ScreenConfig screen : app.getConfig().getScreens()) {
            Button btn = UiFactory.createStartScreenListButton(screen.getDisplayName(), screen.getColor());
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> app.showFunctionScreen(screen.getPrefix()));
            screenButtonsBox.getChildren().add(btn);
        }
        screenListScroll.setVisible(true);
        screenListScroll.setManaged(true);
    }

    private void hideScreenList() {
        screenButtonsBox.getChildren().clear();
        screenListScroll.setVisible(false);
        screenListScroll.setManaged(false);
    }

    private void applyCredentials() {
        if (tryConnectWithCurrentCredentials()) {
            clearInvalidState();
            Config config = configToSaved();
            try {
                config.saveToFile();
            } catch (Exception e) {
                app.showError("Couldn't open config");
            }
            app.showSuccess("Success");
            refreshScreenList();
        } else {
            setInvalidState();
            app.showError("Error");
            hideScreenList();
        }
    }

    private void resetToSaved() {
        clearInvalidState();
        savedToCur();
        if (tryConnectWithCurrentCredentials()) {
            refreshScreenList();
            app.showSuccess("Success");
        } else {
            setInvalidState();
            app.showError("Error");
            hideScreenList();
        }
    }

    private void savedToCur() {
        urlField.setText(savedUrl);
        userField.setText(savedUser);
        databaseField.setText(savedDatabase);
        passwordInput.setText(savedPassword);
    }

    private boolean tryConnectWithCurrentCredentials() {
        app.updateConnectionSettings(
                urlField.getText(),
                userField.getText(),
                passwordInput.getText(),
                databaseField.getText()
        );
        return app.getDatabaseManager().connect();
    }

    private void setInvalidState() {
        credentialsInvalid = true;
        errorPlate.setVisible(true);
        errorPlate.setManaged(true);
        setFieldInvalid(true);
    }

    private void clearInvalidState() {
        credentialsInvalid = false;
        errorPlate.setVisible(false);
        errorPlate.setManaged(false);
        setFieldInvalid(false);
    }

    private void setFieldInvalid(boolean invalid) {
        UiFactory.styleInputField(urlField, invalid);
        UiFactory.styleInputField(userField, invalid);
        UiFactory.styleInputField(databaseField, invalid);
        passwordInput.setInvalid(invalid);
    }
}
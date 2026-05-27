package com.postgres_gui.ui.screens;

import com.postgres_gui.config.Config;
import com.postgres_gui.config.ScreenConfig;
import com.postgres_gui.ui.AppWindow;
import com.postgres_gui.ui.components.Button;
import com.postgres_gui.ui.components.ButtonList;
import com.postgres_gui.ui.components.TextInputField;
import com.postgres_gui.util.ColorUtils;
import com.postgres_gui.util.TextMetrics;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class StartScreen extends Screen {
    private static final String TITLE = "PostgreSQL Function GUI";
    private static final String SUBTITLE = "Created by Longseax";
    private static final String INVALID_MSG = "Invalid connection data";
    private static final double FIELD_WIDTH = 320;
    private static final double FIELD_HEIGHT = 32;
    private static final double LABEL_GAP = 8;
    private static final double ROW_GAP = 16;
    private static final Font TITLE_FONT = new Font("Segoe UI Bold", 22);
    private static final Font LABEL_FONT = new Font("Segoe UI", 12);
    private static final double ICON_ALPHA = 0.3;
    private static final double ICON_SIZE = 1;
    private static final double SCREEN_LIST_OPACITY = 0.8;


    private final TextInputField urlField;
    private final TextInputField userField;
    private final TextInputField databaseField;
    private final TextInputField passwordField;
    private Button applyButton;
    private Button resetButton;
    private ButtonList screenList;
    private Image icon;

    private String savedUrl;
    private String savedUser;
    private String savedDatabase;
    private String savedPassword;
    private boolean credentialsInvalid;
    private boolean credentialsChanged;
    private double width;
    private double height;

    public StartScreen(AppWindow app, Image icon) {
        super(app);
        urlField = new TextInputField(0, 0, FIELD_WIDTH, FIELD_HEIGHT, "URL");
        userField = new TextInputField(0, 0, FIELD_WIDTH, FIELD_HEIGHT, "User");
        databaseField = new TextInputField(0, 0, FIELD_WIDTH, FIELD_HEIGHT, "DataBase");
        passwordField = new TextInputField(0, 0, FIELD_WIDTH, FIELD_HEIGHT, "Password");
        this.icon = icon;
        loadFieldsFromConfig();
    }

    private void loadFieldsFromConfig() {
        configToSaved();
        savedToCur();
    }

    private void rebuildScreenButtons() {
        screenList = null;
        if (!app.getDatabaseManager().isConnected() || width <= 0 || height <= 0) {
            return;
        }


        double listY = loginBlockBottomY() + ROW_GAP;
        double listW = FIELD_WIDTH;
        double listH =  height - listY - 40;
        double listX = (width - listW) / 2;

        screenList = new ButtonList(listX, listY, listW, listH,
                Color.web("#E8E8E8", SCREEN_LIST_OPACITY) , ButtonList.ListOrientation.VERTICAL,
                ButtonList.VerticalButtonWidth.FILL_WIDTH, false);
        for (ScreenConfig screen : app.getConfig().getScreens()) {
            Color screenColor = Color.web(screen.getColor());
            screenList.addButton(new Button(screen.getDisplayName(), screenColor, () ->
                    app.showFunctionScreen(screen.getPrefix()), Color.WHITE));
        }
    }

    /** Нижняя граница блока логина (поля + apply), для размещения списка экранов ниже. */
    private double loginBlockBottomY() {
        double titleY = Math.max(60, height * 0.22);
        double urlY = titleY + 44;
        double userY = urlY + urlField.getHeight() + ROW_GAP;
        double dbY = userY + userField.getHeight() + ROW_GAP;
        double passY = dbY + databaseField.getHeight() + ROW_GAP;
        double bottom = passY + passwordField.getHeight();
        return bottom;
    }

    @Override
    public void onShow() {
        savedToCur();
        if (tryConnectWithCurrentCredentials()) {
            clearInvalidState();
            rebuildScreenButtons();
        } else {
            setInvalidState();
            screenList = null;
        }
        app.redraw();
    }

    @Override
    public void onResize(double width, double height) {
        this.width = width;
        this.height = height;
        if (app.getDatabaseManager().isConnected()) {
            rebuildScreenButtons();
        }
    }

    @Override
    public void draw(GraphicsContext gc, double width, double height) {
        this.width = width;
        this.height = height;

        gc.setFill(Color.web(ScreenConfig.backgroundColor));
        gc.fillRect(0, 0, width, height);

        if (icon != null) {
            gc.save();

            gc.setGlobalAlpha(ICON_ALPHA);

            double iconSize = Math.min(width, height) * ICON_SIZE;

            double iconX = (width - iconSize) / 2;
            double iconY = (height - iconSize) / 2;

            gc.drawImage(icon, iconX, iconY, iconSize, iconSize);

            gc.restore();
        }


        double blockX = (width - FIELD_WIDTH) / 2;
        double titleY = Math.max(60, height * 0.18);

        gc.setFont(TITLE_FONT);
        gc.setFill(Color.BLACK);
        double titleW = TextMetrics.width(TITLE_FONT, TITLE);
        gc.fillText(TITLE, (width - titleW) / 2, titleY);

        gc.setFont(LABEL_FONT);
        double subtitleW = TextMetrics.width(LABEL_FONT, SUBTITLE);
        gc.setFill(Color.web("#555555"));
        gc.fillText(SUBTITLE, (width - subtitleW) / 2, titleY + 14);

        Color bg = Color.web(ScreenConfig.backgroundColor);
        Color header = ColorUtils.darken(bg, 0.06);

        double urlY = titleY + 38;
        double userY = urlY + urlField.getHeight() + ROW_GAP;
        double dbY = userY + userField.getHeight() + ROW_GAP;
        double passY = dbY + databaseField.getHeight() + ROW_GAP;

        gc.setFont(LABEL_FONT);
        if (credentialsInvalid) {
            gc.setFill(Color.web("#D32F2F"));
            double errW = TextMetrics.width(LABEL_FONT, INVALID_MSG);
            gc.fillText(INVALID_MSG, (width - errW) / 2, urlY - 6);
        }

        urlField.setHeaderColor(header);
        urlField.setPosition(blockX, urlY);
        urlField.setSize(FIELD_WIDTH, FIELD_HEIGHT);
        urlField.setError(credentialsInvalid, null);
        urlField.draw(gc);

        userField.setHeaderColor(header);
        userField.setPosition(blockX, userY);
        userField.setSize(FIELD_WIDTH, FIELD_HEIGHT);
        userField.setError(credentialsInvalid, null);
        userField.draw(gc);

        databaseField.setHeaderColor(header);
        databaseField.setPosition(blockX, dbY);
        databaseField.setSize(FIELD_WIDTH, FIELD_HEIGHT);
        databaseField.setError(credentialsInvalid, null);
        databaseField.draw(gc);

        passwordField.setHeaderColor(header);
        passwordField.setPosition(blockX, passY);
        passwordField.setSize(FIELD_WIDTH, FIELD_HEIGHT);
        passwordField.setError(credentialsInvalid, null);
        passwordField.draw(gc);

        if (credentialsChanged) {
            if (applyButton == null) {
                applyButton = new Button("Connect", Color.web("#4CAF50"), this::applyCredentials, false, true, Color.WHITE);
            }
            applyButton.setPosition(blockX + FIELD_WIDTH + 12, passY + 18);
            applyButton.draw(gc);

            if (resetButton == null) {
                resetButton = new Button("Reset", Color.web("#B0B0B0"), this::resetToSaved, false, true, Color.WHITE);
            }
            double resetX = applyButton.getX() + applyButton.getWidth() + 10;
            resetButton.setPosition(resetX, passY + 18);
            resetButton.draw(gc);
        }

        if (screenList != null) {
            screenList.draw(gc);
        }
    }

    private void applyCredentials() {
        if (tryConnectWithCurrentCredentials()) {
            clearInvalidState();
            credentialsChanged = false;
            //config == cur, updating saved
            Config config = configToSaved();
            try {
                config.saveToFile();
            } catch (Exception e) {
                app.showError("Couldn't open config");
            }
            app.showSuccess("Success");
        } else {
            //don't change saved to be able to go back
            setInvalidState();
            app.showError("Error");
        }
        rebuildScreenButtons();
        app.redraw();
    }

    private void resetToSaved() {
        credentialsChanged = false;
        clearInvalidState();
        savedToCur();
        if (tryConnectWithCurrentCredentials()) {
            rebuildScreenButtons();
            app.showSuccess("Success");
        } else {
            setInvalidState();
            app.showError("Error");
        }

        app.redraw();
    }

    private Config configToSaved(){
        Config config = app.getConfig();
        savedUrl = config.getUrl();
        savedUser = config.getUser();
        savedDatabase = config.getDatabase();
        savedPassword = config.getPassword();
        return config;
    }

    private Config savedToConfig(){
        Config config = app.getConfig();
        config.setUrl(urlField.getText());
        config.setUser(userField.getText());
        config.setDatabase(databaseField.getText());
        config.setPassword(passwordField.getText());
        return config;
    }

    private void savedToCur(){
        urlField.setText(savedUrl);
        userField.setText(savedUser);
        databaseField.setText(savedDatabase);
        passwordField.setText(savedPassword);
    }

    /**
     * Changes config
     */
    private boolean tryConnectWithCurrentCredentials() {
        app.updateConnectionSettings(
                urlField.getText(),
                userField.getText(),
                passwordField.getText(),
                databaseField.getText()
        );
        return app.getDatabaseManager().connect();
    }

    private void setInvalidState() {
        credentialsInvalid = true;
        userField.setError(true, null);
        databaseField.setError(true, null);
        passwordField.setError(true, null);
    }

    private void clearInvalidState() {
        credentialsInvalid = false;
        userField.setError(false, null);
        databaseField.setError(false, null);
        passwordField.setError(false, null);
    }

    private void markCredentialsChanged() {
        credentialsChanged = !urlField.getText().equals(savedUrl)
                || !userField.getText().equals(savedUser)
                || !databaseField.getText().equals(savedDatabase)
                || !passwordField.getText().equals(savedPassword);
        if (credentialsChanged && credentialsInvalid) {
            clearInvalidState();
        }
    }

    @Override
    public void handleMousePressed(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();

        userField.handleMouseClick(x, y, event.isShiftDown());
        databaseField.handleMouseClick(x, y, event.isShiftDown());
        passwordField.handleMouseClick(x, y, event.isShiftDown());
        urlField.handleMouseClick(x, y, event.isShiftDown());
        markCredentialsChanged();

        if (applyButton != null && applyButton.contains(x, y)) {
            applyButton.click();
        }
        if (resetButton != null && resetButton.contains(x, y)) {
            resetButton.click();
        }
        if (screenList != null) {
            screenList.handleMouseClick(x, y);
        }
    }

    @Override
    public void handleMouseMoved(MouseEvent event) {
        if (applyButton != null) {
            applyButton.setHovered(applyButton.contains(event.getX(), event.getY()));
        }
        if (resetButton != null) {
            resetButton.setHovered(resetButton.contains(event.getX(), event.getY()));
        }
        if (screenList != null) {
            screenList.handleMouseMove(event.getX(), event.getY());
        }
    }

    @Override
    public void handleScroll(ScrollEvent event) {
        if (screenList != null) {
            screenList.handleScroll(event.getDeltaY());
        }
    }

    @Override
    public void handleKeyPressed(KeyEvent event) {
        boolean control = event.isControlDown();

        if (urlField.handleKeyPress(event.getCode(), null, control)
                || userField.handleKeyPress(event.getCode(), null, control)
                || databaseField.handleKeyPress(event.getCode(), null, control)
                || passwordField.handleKeyPress(event.getCode(), null, control)) {
            markCredentialsChanged();
        }
    }


    public void handleKeyTyped(KeyEvent event) {
        if (urlField.handleKeyPress(KeyCode.UNDEFINED, event.getCharacter(), false)
                || userField.handleKeyPress(KeyCode.UNDEFINED, event.getCharacter(), false)
                || databaseField.handleKeyPress(KeyCode.UNDEFINED, event.getCharacter(), false)
                || passwordField.handleKeyPress(KeyCode.UNDEFINED, event.getCharacter(), false)) {
            markCredentialsChanged();
        }
    }
}
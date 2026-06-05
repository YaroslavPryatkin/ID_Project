package com.postgres_gui.ui;

import com.postgres_gui.config.Config;
import com.postgres_gui.config.ScreenConfig;
import com.postgres_gui.database.DatabaseManager;
import com.postgres_gui.ui.components.NotificationSystem;
import com.postgres_gui.ui.screens.FunctionScreen;
import com.postgres_gui.ui.screens.Screen;
import com.postgres_gui.ui.screens.StartScreen;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;

public class AppWindow {
    private final Config config;
    private final DatabaseManager databaseManager;
    private final NotificationSystem notificationSystem;
    private final Map<String, FunctionScreen> functionScreens = new HashMap<>();
    private final StackPane root = new StackPane();
    private final StackPane screenHost = new StackPane();
    private final SimpleDoubleProperty rootWidth = new SimpleDoubleProperty(900);
    private final SimpleDoubleProperty rootHeight = new SimpleDoubleProperty(700);

    private Screen currentScreen;
    private final StartScreen startScreen;

    public AppWindow(Config config, Image icon) {
        this.config = config;
        this.databaseManager = new DatabaseManager(
                config.getUrl(), config.getDatabase(), config.getUser(), config.getPassword());
        this.notificationSystem = new NotificationSystem();
        this.startScreen = new StartScreen(this, icon);
        this.currentScreen = startScreen;

        root.setStyle("-fx-background-color: " + ScreenConfig.backgroundColor + ";");
        screenHost.prefWidthProperty().bind(root.widthProperty());
        screenHost.prefHeightProperty().bind(root.heightProperty());
        screenHost.getChildren().add(startScreen);
        root.getChildren().addAll(screenHost, notificationSystem.getView());

        root.widthProperty().addListener((obs, o, n) -> rootWidth.set(n.doubleValue()));
        root.heightProperty().addListener((obs, o, n) -> rootHeight.set(n.doubleValue()));
    }

    public StackPane getRoot() {
        return root;
    }

    public ReadOnlyDoubleProperty rootWidthProperty() {
        return rootWidth;
    }

    public ReadOnlyDoubleProperty rootHeightProperty() {
        return rootHeight;
    }

    public void bindRootSize(double width, double height) {
        root.setPrefSize(width, height);
        rootWidth.set(width);
        rootHeight.set(height);
    }

    private void showScreen(Screen screen) {
        currentScreen = screen;
        screenHost.getChildren().setAll(screen);
        screen.onShow();
    }

    public void showStartScreen() {
        showScreen(startScreen);
    }

    public void showFunctionScreen(String prefix) {
        if (!databaseManager.isConnected() && !databaseManager.connect()) {
            showError("Not connected to database");
            showStartScreen();
            return;
        }
        ScreenConfig screenConfig = config.getScreenByPrefix(prefix);
        if (screenConfig == null) {
            showError("Unknown screen: " + prefix);
            return;
        }
        FunctionScreen screen = functionScreens.computeIfAbsent(prefix, p -> new FunctionScreen(this, screenConfig));
        showScreen(screen);
    }

    public void updateConnectionSettings(String url, String user, String password, String database) {
        config.setUrl(url);
        config.setUser(user);
        config.setPassword(password);
        config.setDatabase(database);
        databaseManager.setCredentials(url, database, user, password);
    }

    public void showSuccess(String message) {
        notificationSystem.showSuccess(message);
    }

    public void showError(String message) {
        notificationSystem.showError(message);
    }

    public void setNotificationReservedBottom(double reservedBottom) {
        notificationSystem.setReservedBottom(reservedBottom);
    }

    public void setNotificationReservedRight(double reservedRight) {
        notificationSystem.setReservedRight(reservedRight);
    }

    public Config getConfig() {
        return config;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}

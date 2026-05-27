package com.postgres_gui.ui;

import com.postgres_gui.config.Config;
import com.postgres_gui.config.ScreenConfig;
import com.postgres_gui.database.DatabaseManager;
import com.postgres_gui.ui.components.NotificationSystem;
import com.postgres_gui.ui.screens.FunctionScreen;
import com.postgres_gui.ui.screens.Screen;
import com.postgres_gui.ui.screens.StartScreen;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.text.TextAlignment;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import java.util.HashMap;
import java.util.Map;

public class AppWindow {
    private final Config config;
    private final DatabaseManager databaseManager;
    private final NotificationSystem notificationSystem;
    private final Map<String, FunctionScreen> functionScreens = new HashMap<>();

    private Canvas canvas;
    private Screen currentScreen;
    private StartScreen startScreen;

    public AppWindow(Config config, Image image) {
        this.config = config;
        this.databaseManager = new DatabaseManager(
                config.getUrl(), config.getDatabase(), config.getUser(), config.getPassword());
        this.notificationSystem = new NotificationSystem(800, 600);
        this.startScreen = new StartScreen(this, image);
        this.currentScreen = startScreen;
    }

    public void attachCanvas(Canvas canvas) {
        this.canvas = canvas;
        canvas.setFocusTraversable(true);

        canvas.setOnMousePressed(this::onMousePressed);
        canvas.setOnMouseMoved(this::onMouseMoved);
        canvas.setOnScroll(this::onScroll);
        canvas.setOnKeyPressed(this::onKeyPressed);
        canvas.setOnKeyTyped(this::onKeyTyped);

        canvas.widthProperty().addListener((obs, oldVal, newVal) -> resize(newVal.doubleValue(), canvas.getHeight()));
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> resize(canvas.getWidth(), newVal.doubleValue()));
        resize(canvas.getWidth(), canvas.getHeight());

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                redraw();
            }
        }.start();
    }

    private void resize(double width, double height) {
        notificationSystem.setScreenSize(width, height);
        currentScreen.onResize(width, height);
        redraw();
    }

    public void redraw() {
        if (canvas == null) {
            return;
        }
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        gc.clearRect(0, 0, w, h);
        gc.setGlobalAlpha(1.0);
        gc.setTextAlign(TextAlignment.LEFT);
        currentScreen.draw(gc, w, h);
        notificationSystem.draw(gc);
    }

    private void onMousePressed(MouseEvent event) {
        canvas.requestFocus();
        currentScreen.handleMousePressed(event);
        redraw();
    }

    private void onMouseMoved(MouseEvent event) {
        currentScreen.handleMouseMoved(event);
        redraw();
    }

    private void onScroll(ScrollEvent event) {
        currentScreen.handleScroll(event);
        redraw();
    }

    private void onKeyPressed(KeyEvent event) {
        currentScreen.handleKeyPressed(event);
        redraw();
    }

    private void onKeyTyped(KeyEvent event) {
        currentScreen.handleKeyTyped(event);
        redraw();
    }


    public void showStartScreen() {
        currentScreen = startScreen;
        if (canvas != null) {
            startScreen.onResize(canvas.getWidth(), canvas.getHeight());
        }
        startScreen.onShow();
        redraw();
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
        currentScreen = screen;
        screen.onResize(canvas.getWidth(), canvas.getHeight());
        redraw();
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

    public Canvas getCanvas() {
        return canvas;
    }
}

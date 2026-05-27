package com.postgres_gui.ui.screens;

import com.postgres_gui.ui.AppWindow;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

public abstract class Screen {
    protected final AppWindow app;

    protected Screen(AppWindow app) {
        this.app = app;
    }

    public abstract void draw(GraphicsContext gc, double width, double height);

    public void onShow() {
    }

    public void onResize(double width, double height) {
    }

    public void handleMousePressed(MouseEvent event) {
    }

    public void handleMouseMoved(MouseEvent event) {
    }

    public void handleScroll(ScrollEvent event) {
    }

    public void handleKeyPressed(KeyEvent event) {
    }
    public void handleKeyTyped(KeyEvent event) {
    }
}

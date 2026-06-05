package com.postgres_gui.ui.screens;

import com.postgres_gui.ui.AppWindow;
import javafx.scene.layout.StackPane;

public abstract class Screen extends StackPane {
    protected final AppWindow app;

    protected Screen(AppWindow app) {
        this.app = app;
        prefWidthProperty().bind(app.rootWidthProperty());
        prefHeightProperty().bind(app.rootHeightProperty());
    }

    public void onShow() {
    }
}

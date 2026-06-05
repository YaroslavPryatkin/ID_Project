package com.postgres_gui.ui.components;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;

public final class AutoHeightScrollPanel {
    private AutoHeightScrollPanel() {
    }

    public static ScrollPane wrap(Region content, double maxHeight) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        UiFactory.hideScrollBars(scroll);

        Runnable updateHeightTask = () -> updateScrollHeight(scroll, content, maxHeight);

        content.heightProperty().addListener((obs, o, n) -> updateHeightTask.run());
        content.widthProperty().addListener((obs, o, n) -> updateHeightTask.run());
        scroll.sceneProperty().addListener((obs, o, scene) -> {
            if (scene != null) {
                updateHeightTask.run();
            }
        });
        scroll.maxHeightProperty().addListener((obs, o, n) -> updateHeightTask.run());

        updateHeightTask.run();
        return scroll;
    }

    public static void updateScrollHeight(ScrollPane scroll, Region content, double maxHeight) {
        content.applyCss();
        content.layout();
        double contentHeight = content.prefHeight(-1);
        if (contentHeight <= 0) {
            contentHeight = content.minHeight(-1);
        }



        double targetHeight = Math.min(contentHeight, maxHeight);
        scroll.setPrefViewportHeight(targetHeight);
        scroll.setMaxHeight(targetHeight);

        if (contentHeight > maxHeight) {
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        } else {
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        }
        UiFactory.applyHiddenScrollBars(scroll);
    }
}
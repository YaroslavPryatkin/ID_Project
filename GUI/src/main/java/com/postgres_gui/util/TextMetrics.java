package com.postgres_gui.util;

import javafx.scene.text.Font;
import javafx.scene.text.Text;

public final class TextMetrics {
    private TextMetrics() {
    }

    public static double width(Font font, String text) {
        Text node = new Text(text == null ? "" : text);
        node.setFont(font);
        return node.getLayoutBounds().getWidth();
    }
}

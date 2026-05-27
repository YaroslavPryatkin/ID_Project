package com.postgres_gui.util;

import javafx.scene.paint.Color;

public final class ColorUtils {
    private ColorUtils() {
    }

    public static Color darken(Color color, double factor) {
        if (color == null) {
            return Color.web("#CCCCCC");
        }
        return new Color(
                clamp(color.getRed() * (1 - factor)),
                clamp(color.getGreen() * (1 - factor)),
                clamp(color.getBlue() * (1 - factor)),
                color.getOpacity()
        );
    }

    public static Color lighten(Color color, double factor) {
        if (color == null) {
            return Color.web("#CCCCCC");
        }
        return new Color(
                clamp(color.getRed() + factor),
                clamp(color.getGreen() + factor),
                clamp(color.getBlue() + factor),
                color.getOpacity()
        );
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}

package com.postgres_gui.ui.components;

import com.postgres_gui.ui.screens.FunctionScreen;
import com.postgres_gui.util.ColorUtils;
import com.postgres_gui.util.TextMetrics;
import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

import static javafx.scene.text.TextAlignment.CENTER;
import static javafx.scene.text.TextAlignment.LEFT;

public class Button {
    private String label;
    private final Color textColor;
    private final Color normalColor;
    private final Color hoverColor;
    private final Color selectedColor;
    private Runnable onClickAction;

    private double x;
    private double y;
    private double width;
    private double height;
    private double maxWrapWidth = Double.MAX_VALUE;
    private boolean hovered;
    private boolean selected;
    private boolean active = true;
    private boolean roundedCorners = true;
    private boolean centerText = true;

    private static final Font BUTTON_FONT = new Font("Segoe UI", 12);
    private final Font font;
    private static final double BUTTON_PADDING = 12;
    private static final double CORNER_RADIUS = 8;
    private static final double LINE_HEIGHT = 16;

    public Button(String label, Color color, Runnable onClickAction, Color textColor) {
        this.label = label;
        Color safeColor = color != null ? color : Color.web("#CCCCCC");
        this.normalColor = safeColor;
        this.hoverColor = ColorUtils.darken(safeColor, 0.15);
        this.selectedColor = ColorUtils.darken(safeColor, 0.35);
        this.roundedCorners = true;
        this.centerText = true;
        this.onClickAction = onClickAction;
        this.font = FunctionScreen.SCREEN_TITLE_FONT;
        this.textColor = textColor;
        calculateSize();
    }

    public Button(String label, Color color, Runnable onClickAction, boolean roundedCorners, boolean centerText, Color textColor) {
        this.label = label;
        Color safeColor = color != null ? color : Color.web("#CCCCCC");
        this.normalColor = safeColor;
        this.hoverColor = ColorUtils.darken(safeColor, 0.15);
        this.selectedColor = ColorUtils.darken(safeColor, 0.35);
        this.roundedCorners = roundedCorners;
        this.centerText = centerText;
        this.onClickAction = onClickAction;
        this.textColor = textColor;
        this.font= FunctionScreen.SCREEN_TITLE_FONT;
        calculateSize();
    }

    private void calculateSize() {
        if (maxWrapWidth < Double.MAX_VALUE) {
            List<String> lines = wrapLines(label, maxWrapWidth - BUTTON_PADDING * 2);
            width = maxWrapWidth; // растягиваем кнопку на ширину списка
            height = Math.max(LINE_HEIGHT + BUTTON_PADDING, lines.size() * LINE_HEIGHT + BUTTON_PADDING);
        } else {
            Text textNode = new Text(label);
            textNode.setFont(font);
            Bounds bounds = textNode.getBoundsInLocal();
            width = bounds.getWidth() + BUTTON_PADDING * 2;
            height = bounds.getHeight() + BUTTON_PADDING;
        }
    }

    private Color currentDrawColor() {
        if (selected) {
            return selectedColor;
        }
        if (hovered && active) {
            return hoverColor;
        }
        return normalColor;
    }

    public void draw(GraphicsContext gc) {
        gc.save();
        if (!active) {
            gc.setGlobalAlpha(0.5);
        }

        Color drawColor = currentDrawColor();
        gc.setFill(drawColor);
        if (roundedCorners) {
            gc.fillRoundRect(x, y, width, height, CORNER_RADIUS, CORNER_RADIUS);
        } else {
            gc.fillRect(x, y, width, height);
        }

        gc.setStroke(ColorUtils.darken(drawColor, 0.3));
        gc.setLineWidth(1);
        if (roundedCorners) {
            gc.strokeRoundRect(x, y, width, height, CORNER_RADIUS, CORNER_RADIUS);
        } else {
            gc.strokeRect(x, y, width, height);
        }

        gc.setFill(textColor);
        gc.setFont(font);

        if (maxWrapWidth < Double.MAX_VALUE && width >= maxWrapWidth - 0.5) {
            drawWrappedText(gc, label, x + BUTTON_PADDING, y + BUTTON_PADDING / 2, width - BUTTON_PADDING * 2);
        } else {
            double textX = x + (centerText ? width / 2 : BUTTON_PADDING);
            double textY = y + height / 2 + 5;
            gc.setTextAlign(centerText ? CENTER : LEFT);
            gc.fillText(label, textX, textY);
        }

        if (!active) {
            gc.setGlobalAlpha(1.0);
        }
        gc.restore();
    }

    private void drawWrappedText(GraphicsContext gc, String text, double startX, double startY, double wrapWidth) {
        List<String> lines = wrapLines(text, wrapWidth);
        double currentY = startY + LINE_HEIGHT/2 + 5;
        double drawX = centerText ? (x + width / 2) : startX;
        for (String line : lines) {
            gc.setTextAlign(centerText ? CENTER : LEFT);
            gc.fillText(line, drawX, currentY);
            currentY += LINE_HEIGHT;
        }
    }

    private List<String> wrapLines(String text, double wrapWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (measureText(candidate) > wrapWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    private double measureText(String text) {
        return TextMetrics.width(font, text);
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered && active;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public void click() {
        if (active && onClickAction != null) {
            onClickAction.run();
        }
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setMaxWrapWidth(double maxWrapWidth) {
        this.maxWrapWidth = maxWrapWidth;
        calculateSize();
    }

    public void clearMaxWrapWidth() {
        this.maxWrapWidth = Double.MAX_VALUE;
        calculateSize();
    }

    public void setSize(double width, double height) {
        this.maxWrapWidth = Double.MAX_VALUE;
        this.width = width;
        this.height = height;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        calculateSize();
    }

    public Color getNormalColor() {
        return normalColor;
    }

    public void setCenterText(boolean centerText) {
        this.centerText = centerText;
        // maxWrapWidth определяет перенос, поэтому пересчитывать высоту не обязательно,
        // но пересчёт габаритов безопасен.
        calculateSize();
    }
}

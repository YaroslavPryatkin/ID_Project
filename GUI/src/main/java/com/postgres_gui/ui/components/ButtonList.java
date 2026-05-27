package com.postgres_gui.ui.components;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class ButtonList {
    private final List<Button> buttons = new ArrayList<>();
    private final Color backgroundColor;
    private final ListRenderer renderer;
    private final boolean selectionEnabled;

    private double x;
    private double y;
    private double width;
    private double height;
    private double scrollOffset;
    private Button selectedButton;

    private static final double BUTTON_MARGIN = 8;

    public enum ListOrientation {
        VERTICAL, HORIZONTAL
    }

    public enum VerticalButtonWidth {
        FIT_CONTENT,
        FILL_WIDTH,
        FILL_WIDTH_LEFT
    }

    public ButtonList(double x, double y, double width, double height,
                      Color backgroundColor, ListOrientation orientation,
                      VerticalButtonWidth verticalButtonWidth, boolean selectionEnabled) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.backgroundColor = backgroundColor;
        this.selectionEnabled = selectionEnabled;
        this.renderer = orientation == ListOrientation.VERTICAL
                ? new VerticalListRenderer(verticalButtonWidth)
                : new HorizontalListRenderer();
    }

    public void addButton(Button button) {
        buttons.add(button);

        if (selectionEnabled && selectedButton == null) {
            selectedButton = button;
            button.setSelected(true);
        }
    }

    public void clearButtons() {
        for (Button button : buttons) {
            button.setSelected(false);
        }
        buttons.clear();
        selectedButton = null;
    }

    public void layoutButtons() {
        renderer.layout();
    }

    public void draw(GraphicsContext gc) {
        layoutButtons();

        gc.setFill(backgroundColor);
        gc.fillRect(x, y, width, height);

        gc.setStroke(Color.web("#333333"));
        gc.setLineWidth(1);
        gc.strokeRect(x, y, width, height);

        gc.save();
        gc.beginPath();
        gc.rect(x, y, width, height);
        gc.clip();

        for (Button button : buttons) {
            button.draw(gc);
        }
        gc.restore();
    }

    public void selectButton(Button button) {

        if (!selectionEnabled || !buttons.contains(button)) {
            return;
        }
        if (selectedButton != null) {
            selectedButton.setSelected(false);
        }
        selectedButton = button;
        button.setSelected(true);
    }

    public Button getButtonAt(double px, double py) {
        layoutButtons();
        for (Button button : buttons) {
            if (button.contains(px, py)) {
                return button;
            }
        }
        return null;
    }

    public void handleMouseMove(double px, double py) {
        layoutButtons();
        for (Button button : buttons) {
            button.setHovered(button.contains(px, py));
        }
    }

    public void handleMouseClick(double px, double py) {
        Button button = getButtonAt(px, py);
        if (button != null) {
            if (selectionEnabled) {
                selectButton(button);
            }
            button.click();
        }
    }

    public void handleScroll(double deltaY) {
        renderer.handleScroll(deltaY);
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public Button getSelectedButton() {
        return selectedButton;
    }

    public List<Button> getButtons() {
        return buttons;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    private interface ListRenderer {
        void layout();
        void handleScroll(double deltaY);
    }

    private class VerticalListRenderer implements ListRenderer {
        private final VerticalButtonWidth widthMode;

        VerticalListRenderer(VerticalButtonWidth widthMode) {
            this.widthMode = widthMode;
        }

        @Override
        public void layout() {
            double availableWidth = width - BUTTON_MARGIN * 2;

            double totalContentHeight = BUTTON_MARGIN;
            for (Button button : buttons) {
                if (widthMode == VerticalButtonWidth.FILL_WIDTH_LEFT) {
                    button.setCenterText(false);
                    button.setMaxWrapWidth(availableWidth);
                } else if (widthMode == VerticalButtonWidth.FILL_WIDTH) {
                    button.setCenterText(true);
                    button.setMaxWrapWidth(availableWidth);
                } else {
                    button.setCenterText(false);
                    button.clearMaxWrapWidth();
                }
                totalContentHeight += button.getHeight() + BUTTON_MARGIN;
            }


            double maxScroll = Math.max(0, totalContentHeight - height);
            scrollOffset = Math.min(scrollOffset, maxScroll);
            scrollOffset = Math.max(0, scrollOffset);


            double currentY = y + BUTTON_MARGIN - scrollOffset;
            for (Button button : buttons) {
                double btnX = x + BUTTON_MARGIN;
                button.setPosition(btnX, currentY);
                currentY += button.getHeight() + BUTTON_MARGIN;
            }
        }

        @Override
        public void handleScroll(double deltaY) {
            scrollOffset = Math.max(0, scrollOffset - deltaY);
        }
    }

    private class HorizontalListRenderer implements ListRenderer {
        @Override
        public void layout() {

            double totalContentWidth = BUTTON_MARGIN;
            for (Button button : buttons) {
                button.clearMaxWrapWidth();
                totalContentWidth += button.getWidth() + BUTTON_MARGIN;
            }

            double maxScroll = Math.max(0, totalContentWidth - width);
            scrollOffset = Math.min(scrollOffset, maxScroll);
            scrollOffset = Math.max(0, scrollOffset);

            double currentX = x + BUTTON_MARGIN - scrollOffset;
            for (Button button : buttons) {
                button.setPosition(currentX, y + BUTTON_MARGIN);
                currentX += button.getWidth() + BUTTON_MARGIN;
            }
        }

        @Override
        public void handleScroll(double deltaY) {
            scrollOffset = Math.max(0, scrollOffset - deltaY);
        }
    }
}
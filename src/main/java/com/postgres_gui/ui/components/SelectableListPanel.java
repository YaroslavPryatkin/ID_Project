package com.postgres_gui.ui.components;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public final class SelectableListPanel {
    private final VBox itemsBox;
    private final ScrollPane scrollPane;
    private List<String> values;

    private int selectedIndex = -1;
    private Label currentlySelectedLabel = null;

    private Consumer<String> onSelectionCallback;

    // NEW: флаг для отключения интерактивности (для плашки "нет результатов")
    private boolean isInteractive = true;

    // NEW: текст для плашки "нет результатов"
    private String emptyMessage = "";

    private static final double MAX_HEIGHT = 200;
    private static final String SELECTED_STYLE = "-fx-background-color: #E3F2FD; -fx-padding: 8 12; -fx-text-fill: #000000;";
    private static final String NORMAL_STYLE = "-fx-padding: 8 12; -fx-text-fill: #000000; -fx-background-color: #FFFFFF;";
    private static final String EMPTY_MESSAGE_STYLE = "-fx-padding: 8 12; -fx-text-fill: #999999; -fx-background-color: #FFFFFF; -fx-alignment: center;";

    public SelectableListPanel() {
        this.values = List.of();
        this.itemsBox = new VBox();
        this.itemsBox.setStyle("-fx-spacing: 0; -fx-background-color: #FFFFFF;");
        this.itemsBox.setFocusTraversable(false);

        this.scrollPane = AutoHeightScrollPanel.wrap(itemsBox, MAX_HEIGHT);
        this.scrollPane.setMinHeight(0);
        this.scrollPane.setPrefHeight(Region.USE_COMPUTED_SIZE);

        this.scrollPane.setVisible(false);
        this.scrollPane.setManaged(false);

        this.scrollPane.setStyle("-fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-background-color: #FFFFFF;");

        itemsBox.setOnScroll(this::handleMouseScroll);
    }

    public ScrollPane getScrollPane() {
        return scrollPane;
    }

    private void handleMouseScroll(ScrollEvent e) {
        if (scrollPane.getVmax() > 0) {
            double currentVvalue = scrollPane.getVvalue();
            double scrollDelta = -e.getDeltaY() / 1000.0;
            scrollPane.setVvalue(Math.max(0, Math.min(1, currentVvalue + scrollDelta)));
        }
        e.consume();
    }

    public void updateValues(List<String> newValues) {
        this.values = newValues != null ? newValues : List.of();
        this.currentlySelectedLabel = null;
        this.selectedIndex = -1;
        itemsBox.getChildren().clear();

        for (int i = 0; i < values.size(); i++) {
            final int index = i;
            String value = values.get(i);
            Label label = createItemLabel(value, index);
            itemsBox.getChildren().add(label);
        }
        scrollPane.setVvalue(0);
        Platform.runLater(() -> {
            AutoHeightScrollPanel.updateScrollHeight(scrollPane, itemsBox, MAX_HEIGHT);
        });
    }

    private Label createItemLabel(String value, int index) {
        Label label = new Label(value);
        label.setStyle(NORMAL_STYLE);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setCursor(javafx.scene.Cursor.HAND);

        label.setOnMousePressed(e -> {
            // NEW: только если интерактивно
            if (isInteractive) {
                changeSelection(index);
                selectCurrent();
                e.consume();
            }
        });

        label.setOnMouseEntered(e -> {
            // NEW: только если интерактивно
            if (isInteractive) {
                changeSelection(index);
            }
        });

        return label;
    }

    public void show() {
        if (values.isEmpty()) {
            hide();
            return;
        }
        this.isInteractive = true;
        scrollPane.setVisible(true);
        scrollPane.setManaged(true);
    }

    // NEW: показать плашку "нет результатов"
    public void showEmptyMessage(String message) {
        this.emptyMessage = message;
        this.isInteractive = false;
        this.selectedIndex = -1;
        this.currentlySelectedLabel = null;

        itemsBox.getChildren().clear();
        Label emptyLabel = new Label(message);
        emptyLabel.setStyle(EMPTY_MESSAGE_STYLE);
        emptyLabel.setMaxWidth(Double.MAX_VALUE);
        emptyLabel.setWrapText(true);
        itemsBox.getChildren().add(emptyLabel);

        scrollPane.setVvalue(0);
        Platform.runLater(() -> {
            AutoHeightScrollPanel.updateScrollHeight(scrollPane, itemsBox, MAX_HEIGHT);
        });

        scrollPane.setVisible(true);
        scrollPane.setManaged(true);
    }

    public void hide() {
        scrollPane.setVisible(false);
        scrollPane.setManaged(false);
        changeSelection(-1);
        this.isInteractive = true; // сбрасываем флаг при скрытии
    }

    /**
     * Обрабатывает события клавиатуры для навигации по списку.
     *
     * @param e KeyEvent для обработки
     * @return true если событие было обработано, false в противном случае
     */
    public boolean handleKeyEvent(KeyEvent e) {
        if (!scrollPane.isVisible() || values.isEmpty()) {
            return false;
        }

        // NEW: если не интерактивно (плашка "нет результатов"), не обрабатываем события
        if (!isInteractive) {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                hide();
                e.consume();
                return true;
            }
            return false;
        }

        int newIndex = selectedIndex;
        switch (e.getCode()) {
            case UP -> {
                if (newIndex > 0) {
                    newIndex--;
                } else if (!values.isEmpty()) {
                    newIndex = values.size() - 1;
                }
                changeSelection(newIndex);
                scrollToSelected();
                e.consume();
                return true;
            }
            case DOWN -> {
                if (newIndex < values.size() - 1) {
                    newIndex++;
                } else if (!values.isEmpty()) {
                    newIndex = 0;
                }
                changeSelection(newIndex);
                scrollToSelected();
                e.consume();
                return true;
            }
            case TAB -> {
                if (selectedIndex >= 0) {
                    selectCurrent();
                } else if (!values.isEmpty()) {
                    changeSelection(0);
                    selectCurrent();
                }
                e.consume();
                return true;
            }
            case ESCAPE -> {
                hide();
                e.consume();
                return true;
            }
            case ENTER -> {
                if (selectedIndex >= 0) {
                    selectCurrent();
                    // FIXED: не consume() - пусть событие дойдет до TextField'а для обработки onAction
//                    e.consume();
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    private void changeSelection(int newIndex) {
        if (selectedIndex == newIndex) return;

        if (currentlySelectedLabel != null) {
            currentlySelectedLabel.setStyle(NORMAL_STYLE);
        }

        selectedIndex = newIndex;

        if (selectedIndex >= 0 && selectedIndex < itemsBox.getChildren().size()) {
            currentlySelectedLabel = (Label) itemsBox.getChildren().get(selectedIndex);
            currentlySelectedLabel.setStyle(SELECTED_STYLE);
        } else {
            currentlySelectedLabel = null;
        }
    }

    private void scrollToSelected() {
        if (selectedIndex < 0 || selectedIndex >= itemsBox.getChildren().size()) {
            return;
        }

        Platform.runLater(() -> {
            Label selectedLabel = (Label) itemsBox.getChildren().get(selectedIndex);
            Bounds bounds = selectedLabel.getBoundsInParent();

            double vboxHeight = itemsBox.getBoundsInLocal().getHeight();
            double viewportHeight = scrollPane.getViewportBounds().getHeight();

            if (vboxHeight > viewportHeight) {
                double contentHeight = vboxHeight - viewportHeight;
                double newVvalue = (bounds.getCenterY() - viewportHeight / 2) / contentHeight;
                scrollPane.setVvalue(Math.max(0, Math.min(1, newVvalue)));
            }
        });
    }

    private void selectCurrent() {
        if (selectedIndex >= 0 && selectedIndex < values.size()) {
            String selectedValue = values.get(selectedIndex);
            if (onSelectionCallback != null) {
                onSelectionCallback.accept(selectedValue);
            }
        }
    }

    public void setOnSelection(Consumer<String> callback) {
        this.onSelectionCallback = callback;
    }

    public int getItemCount() {
        return values.size();
    }

    public boolean isShowing() {
        return scrollPane.isVisible();
    }

    public boolean isShowingEmptyMessage() {
        return scrollPane.isVisible() && !isInteractive;
    }
}
package com.postgres_gui.ui.components;

import com.postgres_gui.util.TextMetrics;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

import static javafx.scene.text.TextAlignment.CENTER;

public class TextInputField {
    private final StringBuilder text = new StringBuilder();
    private String label;
    private Color headerColor = Color.web("#DADADA");
    private final Deque<String> undoStack = new ArrayDeque<>();

    private double x;
    private double y;
    private double width;
    private double inputHeight;
    private int cursorPosition;
    private boolean focused;
    private boolean hasError;

    private Consumer<TextInputField> onEnter;
    private Consumer<TextInputField> onBackspaceOnEmpty;

    private static final Font INPUT_FONT = new Font("Segoe UI", 12);
    private static final Font LABEL_FONT = new Font("Segoe UI", 11);
    private static final double LABEL_HEIGHT = 18;
    private static final double PADDING = 6;
    private static final double BORDER_WIDTH = 2;

    public TextInputField(double x, double y, double width, double inputHeight, String label) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.inputHeight = inputHeight;
        this.label = label;
    }

    public void draw(GraphicsContext gc) {
        gc.save();
        // Header
        gc.setFill(headerColor);
        gc.fillRect(x, y, width, LABEL_HEIGHT);
        gc.setFill(Color.web("#222222"));
        gc.setFont(LABEL_FONT);
        gc.setTextAlign(CENTER);
        gc.fillText(label == null ? "" : label, x + width / 2, y + LABEL_HEIGHT / 2 + 4);

        // Input
        double inputY = y + LABEL_HEIGHT;
        gc.setFill(Color.WHITE);
        gc.fillRect(x, inputY, width, inputHeight);

        Color border = hasError ? Color.web("#FF6B6B")
                : (focused ? Color.web("#0066CC") : Color.web("#CCCCCC"));
        gc.setStroke(border);
        gc.setLineWidth(BORDER_WIDTH);
        gc.strokeRect(x, inputY, width, inputHeight);

        gc.setFont(INPUT_FONT);
        String displayText = text.length() == 0 ? "" : text.toString();
        gc.setFill(text.length() == 0 ? Color.web("#999999") : Color.BLACK);

        double centerX = x + width / 2;
        double textY = inputY + inputHeight / 2 + 5;
        gc.setTextAlign(CENTER);
        gc.fillText(displayText, centerX, textY);

        if (focused && text.length() > 0) {
            String beforeCursor = text.substring(0, cursorPosition);
            double textWidth = TextMetrics.width(INPUT_FONT, text.toString());
            double beforeWidth = TextMetrics.width(INPUT_FONT, beforeCursor);
            double textStartX = x + (width - textWidth) / 2;
            double cursorX = textStartX + beforeWidth;
            gc.setStroke(Color.BLACK);
            gc.strokeLine(cursorX, inputY + PADDING, cursorX, inputY + inputHeight - PADDING);
        } else if (focused) {
            gc.setStroke(Color.BLACK);
            gc.strokeLine(centerX, inputY + PADDING, centerX, inputY + inputHeight - PADDING);
        }
        gc.restore();
    }

    public boolean handleKeyPress(KeyCode code, String character, boolean controlDown) {
        if (!focused) {
            return false;
        }

        // Обработка горячих клавиш буфера обмена
        if (controlDown) {
            if (code == KeyCode.C) {
                copyToClipboard();
                return true;
            }
            if (code == KeyCode.V) {
                pasteFromClipboard();
                return true;
            }
            if (code == KeyCode.X) {
                copyToClipboard();
                clearText();
                return true;
            }
            if (code == KeyCode.Z) {
                undo();
                return true;
            }
        }

        switch (code) {
            case BACK_SPACE -> {
                if (cursorPosition > 0) {
                    pushUndo();
                    text.deleteCharAt(cursorPosition - 1);
                    cursorPosition--;
                } else if (onBackspaceOnEmpty != null) {
                    onBackspaceOnEmpty.accept(this);
                }
                return true;
            }
            case DELETE -> {
                if (cursorPosition < text.length()) {
                    pushUndo();
                    text.deleteCharAt(cursorPosition);
                }
                return true;
            }
            case LEFT -> {
                if (cursorPosition > 0) {
                    cursorPosition--;
                }
                return true;
            }
            case RIGHT -> {
                if (cursorPosition < text.length()) {
                    cursorPosition++;
                }
                return true;
            }
            case HOME -> {
                cursorPosition = 0;
                return true;
            }
            case END -> {
                cursorPosition = text.length();
                return true;
            }
            case ENTER -> {
                if (onEnter != null) {
                    onEnter.accept(this);
                }
                return true;
            }
            default -> {
                // Если код клавиши несистемный (или UNDEFINED при KEY_TYPED), вставляем текст
                return insertCharacter(character);
            }
        }
    }

    private boolean insertCharacter(String character) {
        if (character != null && !character.isEmpty()) {
            // Удаляем невидимые системные символы (Backspace, Tab, Escape и т.д.)
            String filtered = character.replaceAll("\\p{C}", "");
            if (!filtered.isEmpty()) {
                pushUndo();
                text.insert(cursorPosition, filtered);
                cursorPosition += filtered.length();
                return true;
            }
        }
        return false;
    }

    public void handleMouseClick(double px, double py, boolean shiftDown) {
        if (contains(px, py)) {
            focused = true;
            cursorPosition = text.length();
        } else {
            focused = false;
        }
    }

    public boolean contains(double px, double py) {
        double inputY = y + LABEL_HEIGHT;
        return px >= x && px <= x + width && py >= inputY && py <= inputY + inputHeight;
    }

    private void pushUndo() {
        undoStack.push(text.toString());
        if (undoStack.size() > 50) {
            undoStack.removeLast();
        }
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            text.setLength(0);
            text.append(undoStack.pop());
            cursorPosition = text.length();
        }
    }

    private void copyToClipboard() {
        if (!text.isEmpty()) {
            Clipboard.getSystemClipboard().setContent(
                    new javafx.scene.input.ClipboardContent() {{
                        putString(text.toString());
                    }}
            );
        }
    }

    private void pasteFromClipboard() {
        if (Clipboard.getSystemClipboard().hasString()) {
            pushUndo();
            String pasted = Clipboard.getSystemClipboard().getString();
            // Используем тот же фильтр для очистки мусора при вставке
            String filtered = pasted.replaceAll("\\p{C}", "");
            text.insert(cursorPosition, filtered);
            cursorPosition += filtered.length();
        }
    }

    private void clearText() {
        pushUndo();
        text.setLength(0);
        cursorPosition = 0;
    }

    public void clear() {
        text.setLength(0);
        cursorPosition = 0;
    }

    public void setText(String value) {
        text.setLength(0);
        if (value != null) {
            text.append(value);
        }
        cursorPosition = text.length();
    }

    public String getText() {
        return text.toString();
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(double width, double height) {
        this.width = width;
        this.inputHeight = height;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setError(boolean hasError, String message) {
        this.hasError = hasError;
    }

    public void setHeaderColor(Color headerColor) {
        if (headerColor != null) {
            this.headerColor = headerColor;
        }
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setOnEnter(Consumer<TextInputField> onEnter) {
        this.onEnter = onEnter;
    }

    public void setOnBackspaceOnEmpty(Consumer<TextInputField> onBackspaceOnEmpty) {
        this.onBackspaceOnEmpty = onBackspaceOnEmpty;
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
        return inputHeight + LABEL_HEIGHT;
    }
}
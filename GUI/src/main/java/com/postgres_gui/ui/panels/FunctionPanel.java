package com.postgres_gui.ui.panels;

import com.postgres_gui.ui.AppWindow;
import com.postgres_gui.ui.components.Button;
import com.postgres_gui.ui.components.TableComponent;
import com.postgres_gui.ui.function.DatabaseFunction;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.nio.file.Path;

public class FunctionPanel {
    private final AppWindow app;
    private DatabaseFunction selectedFunction;

    private double contentX;
    private double contentY;
    private double contentWidth;
    private double contentHeight;
    private double bottomBarY;
    private double bottomBarHeight;
    private Color bottomBarColor;
    private Color buttonColor;

    private Button applyButton;
    private Button applyResetButton;
    private Button resetButton;
    private Button consoleButton;
    private Button clearButton;
    private Button doneButton;
    private Button exportButton;

    public FunctionPanel(AppWindow app) {
        this.app = app;
    }

    public void setLayout(double contentX, double contentY, double contentWidth, double contentHeight,
                          double bottomBarY, double bottomBarHeight, Color bottomBarColor, Color buttonColor) {
        this.contentX = contentX;
        this.contentY = contentY;
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;
        this.bottomBarY = bottomBarY;
        this.bottomBarHeight = bottomBarHeight;
        this.bottomBarColor = bottomBarColor;
        this.buttonColor = buttonColor;
        rebuildButtons();
    }

    public void setSelectedFunction(DatabaseFunction function) {
        this.selectedFunction = function;
        rebuildButtons();
    }

    private void rebuildButtons() {
        boolean hasFunction = selectedFunction != null;
        boolean hasArgs = hasFunction && selectedFunction.hasArguments();
        boolean console = hasFunction && selectedFunction.isShowingConsole();
        boolean table = hasFunction && selectedFunction.isTableResult();

        applyButton = new Button("Apply", buttonColor, () -> runApply(), false, true, Color.BLACK);
        applyResetButton = new Button("Apply and Reset", buttonColor, () -> runApplyAndReset(), false, true, Color.BLACK);
        resetButton = new Button("Reset", buttonColor, () -> {
            if (selectedFunction != null) {
                selectedFunction.resetArguments();
            }
        }, false, true, Color.BLACK);
        consoleButton = new Button(console ? "Back" : "Console", buttonColor, () -> {
            if (selectedFunction != null) {
                selectedFunction.toggleConsole();
                rebuildButtons();
                app.redraw();
            }
        }, false, true, Color.BLACK);
        clearButton = new Button("Clear", buttonColor, () -> {
            if (selectedFunction != null) {
                selectedFunction.clearConsole();
            }
        }, false, true, Color.BLACK);
        doneButton = new Button("Done", buttonColor, () -> {
            if (selectedFunction != null) {
                selectedFunction.clearResult();
                rebuildButtons();
                app.redraw();
            }
        }, false, true, Color.BLACK);
        exportButton = new Button("Export to Excel", buttonColor, this::exportTable, false, true, Color.BLACK);

        applyButton.setActive(hasFunction && !console && !table);
        applyResetButton.setActive(hasFunction && hasArgs && !console && !table);
        resetButton.setActive(hasFunction && hasArgs && !console && !table);
        consoleButton.setActive(hasFunction);
        clearButton.setActive(hasFunction && console);
        doneButton.setActive(hasFunction && table);
        exportButton.setActive(hasFunction && table);
    }

    private void runApply() {
        if (selectedFunction == null) {
            return;
        }
        try {
            selectedFunction.execute();
            app.showSuccess("Success");
            rebuildButtons();
        } catch (Exception e) {
            selectedFunction.addConsoleMessage("ERROR: " + e.getMessage());
            app.showError("Error");
        }
    }

    private void runApplyAndReset() {
        if (selectedFunction == null) {
            return;
        }
        try {
            selectedFunction.execute();
            app.showSuccess("Success");
        } catch (Exception e) {
            // По ТЗ "Apply and Reset" сбрасывает поля даже если запрос упал
            selectedFunction.addConsoleMessage("ERROR: " + e.getMessage());
            app.showError("Error");
        } finally {
            selectedFunction.resetArguments();
            rebuildButtons();
        }
    }

    private void exportTable() {
        if (selectedFunction == null || selectedFunction.getTableComponent() == null) {
            return;
        }
        try {
            Path path = Path.of(System.getProperty("user.home"), "Downloads",
                    selectedFunction.getFunctionName() + "_export.csv");
            selectedFunction.getTableComponent().exportToCsv(path);
            app.showSuccess("Exported");
        } catch (Exception e) {
            app.showError("Export failed");
        }
    }

    public void draw(GraphicsContext gc) {
        if (selectedFunction != null) {
            selectedFunction.draw(gc, contentX, contentY, contentWidth, contentHeight - bottomBarHeight);
        } else {
            gc.setFill(Color.web("#FAFAFA"));
            gc.fillRect(contentX, contentY, contentWidth, contentHeight - bottomBarHeight);
        }

        drawBottomBar(gc);
    }

    private void drawBottomBar(GraphicsContext gc) {
        gc.setFill(bottomBarColor);
        gc.fillRect(contentX, bottomBarY, contentWidth, bottomBarHeight);

        if (selectedFunction == null) {
            layoutInactiveButtons(gc);
            return;
        }

        if (selectedFunction.isShowingConsole()) {
            double quarter = contentWidth / 4.0;
            clearButton.setSize(contentWidth - quarter - 20, 28);
            consoleButton.setSize(quarter - 20, 28);
            layoutSingle(gc, clearButton, contentX + 10, bottomBarY + 6);
            layoutSingle(gc, consoleButton, contentX + contentWidth - quarter + 10, bottomBarY + 6);
            return;
        }

        if (selectedFunction.isTableResult()) {
            double quarter = contentWidth / 4.0;
            double leftWidth = contentWidth - quarter;
            double btnWidth = (leftWidth - 30) / 2.0;
            doneButton.setSize(btnWidth, 28);
            exportButton.setSize(btnWidth, 28);
            consoleButton.setSize(quarter - 20, 28);

            layoutSingle(gc, doneButton, contentX + 10, bottomBarY + 6);
            layoutSingle(gc, exportButton, contentX + 20 + btnWidth, bottomBarY + 6);
            layoutSingle(gc, consoleButton, contentX + contentWidth - quarter + 10, bottomBarY + 6);
            return;
        }

        boolean hasArgs = selectedFunction.hasArguments();
        double quarter = contentWidth / 4.0;
        double leftWidth = contentWidth - quarter;

        if (!hasArgs) {
            applyButton.setSize(leftWidth - 20, 28);
            layoutSingle(gc, applyButton, contentX + 10, bottomBarY + 6);
        } else {
            double btnWidth = (leftWidth - 40) / 3.0;
            applyButton.setSize(btnWidth, 28);
            applyResetButton.setSize(btnWidth, 28);
            resetButton.setSize(btnWidth, 28);
            layoutSingle(gc, applyButton, contentX + 10, bottomBarY + 6);
            layoutSingle(gc, applyResetButton, contentX + 20 + btnWidth, bottomBarY + 6);
            layoutSingle(gc, resetButton, contentX + 30 + btnWidth * 2, bottomBarY + 6);
        }
        consoleButton.setSize(quarter - 20, 28);
        layoutSingle(gc, consoleButton, contentX + contentWidth - quarter + 10, bottomBarY + 6);
    }

    private void layoutInactiveButtons(GraphicsContext gc) {
        applyButton.setActive(false);
        applyResetButton.setActive(false);
        resetButton.setActive(false);
        consoleButton.setActive(false);
    }

    private void layoutSingle(GraphicsContext gc, Button button, double x, double y) {
        if (button.getWidth() <= 0) {
            button.clearMaxWrapWidth();
        }
        button.setPosition(x, y);
        button.draw(gc);
    }

    public boolean handleMouseClick(double x, double y) {
        Button clicked = firstHit(x, y);
        if (clicked != null) {
            clicked.click();
            return true;
        }
        return false;
    }

    public void handleMouseMove(double x, double y) {
        for (Button button : allButtons()) {
            button.setHovered(button.contains(x, y));
        }
    }

    private Button firstHit(double x, double y) {
        for (Button button : allButtons()) {
            if (button.contains(x, y)) {
                return button;
            }
        }
        return null;
    }

    private Button[] allButtons() {
        return new Button[]{applyButton, applyResetButton, resetButton, consoleButton,
                clearButton, doneButton, exportButton};
    }

    public DatabaseFunction getSelectedFunction() {
        return selectedFunction;
    }

    public boolean isInContentArea(double x, double y) {
        return x >= contentX && x <= contentX + contentWidth
                && y >= contentY && y <= contentY + contentHeight - bottomBarHeight;
    }

    public boolean isInBottomBar(double x, double y) {
        return x >= contentX && x <= contentX + contentWidth
                && y >= bottomBarY && y <= bottomBarY + bottomBarHeight;
    }
}

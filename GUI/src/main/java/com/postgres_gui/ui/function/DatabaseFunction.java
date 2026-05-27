package com.postgres_gui.ui.function;

import com.postgres_gui.database.DatabaseFunctionInfo;
import com.postgres_gui.database.DatabaseManager;
import com.postgres_gui.database.FunctionParameter;
import com.postgres_gui.ui.components.Button;
import com.postgres_gui.ui.components.ButtonList;
import com.postgres_gui.ui.components.TableComponent;
import com.postgres_gui.ui.components.TextInputField;
import com.postgres_gui.util.TextMetrics;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DatabaseFunction {
    private final DatabaseFunctionInfo functionInfo;
    private final List<FunctionParameter> parameters;
    private final DatabaseManager dbManager;
    private final String displayName;
    private final List<String> argAliases;
    private final boolean hasNoOutPrefix;

    private final List<String> argumentValues = new ArrayList<>();
    private final List<TextInputField> inputFields = new ArrayList<>();
    private final List<String> consoleMessages = new ArrayList<>();
    private ButtonList consoleList;

    private Object lastResult;
    private boolean showingResult;
    private boolean showingConsole;
    private TableComponent tableComponent;
    private int currentArgumentIndex;

    private static final Font TITLE_FONT = new Font("Segoe UI Bold", 16);
    private static final Font LABEL_FONT = new Font("Segoe UI", 11);
    private static final Font CONSOLE_FONT = new Font("Consolas", 10);

    public DatabaseFunction(DatabaseFunctionInfo functionInfo,
                            DatabaseManager dbManager,
                            String displayName,
                            List<String> argAliases) throws Exception {
        this.functionInfo = functionInfo;
        this.dbManager = dbManager;
        this.displayName = displayName;
        this.argAliases = argAliases == null ? List.of() : argAliases;
        this.hasNoOutPrefix = functionInfo.hasNoOutPrefix();
        this.parameters = dbManager.getFunctionParameters(functionInfo.getName());

        for (FunctionParameter parameter : parameters) {
            argumentValues.add("");
            inputFields.add(new TextInputField(0, 0, 300, 30, parameter.getName()));
        }
        addConsoleMessage("Loaded: " + functionInfo.getName());
    }

    private String getArgLabel(int index, FunctionParameter parameter) {
        if (index >= 0 && index < argAliases.size()) {
            String alias = argAliases.get(index);
            if (alias != null && !alias.isBlank()) {
                return alias.trim();
            }
        }
        return parameter.getName();
    }

    public void syncArgumentValuesFromFields() {
        for (int i = 0; i < inputFields.size(); i++) {
            argumentValues.set(i, inputFields.get(i).getText());
        }
    }

    public void syncFieldsFromArgumentValues() {
        for (int i = 0; i < inputFields.size(); i++) {
            inputFields.get(i).setText(argumentValues.get(i));
        }
    }

    public void execute() throws Exception {
        syncArgumentValuesFromFields();
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < argumentValues.size(); i++) {
            String value = argumentValues.get(i);
            if (value == null || value.isBlank()) {
                params.add(null);
            } else {
                params.add(convertValue(value.trim(), parameters.get(i).getType()));
            }
        }

        Object result = dbManager.executeFunction(functionInfo.getName(), params);

        boolean isEmptyResult = (result == null) || (result instanceof List<?> list && list.isEmpty());
        if (hasNoOutPrefix || isEmptyResult) {
            addConsoleMessage("Executed without output.");
            showingResult = false;
            lastResult = null;
            tableComponent = null;
        } else {
            lastResult = result;
            showingResult = true;
            showingConsole = false;
            tableComponent = null;
            if (result instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tableData = (List<Map<String, Object>>) result;
                lastResult = tableData;
            }
        }
    }

    private Object convertValue(String value, String type) {
        String normalized = type.toLowerCase();

        if (normalized.contains("int") && !normalized.contains("point") && !normalized.contains("interval")) {
            return Integer.parseInt(value);
        }
        if (normalized.contains("bigint")) {
            return Long.parseLong(value);
        }

        if (normalized.contains("numeric") || normalized.contains("decimal")) {
            return new java.math.BigDecimal(value);
        }


        if (normalized.contains("double") || normalized.contains("real") || normalized.contains("float")) {
            return Double.parseDouble(value);
        }


        if (normalized.contains("bool")) {
            return Boolean.parseBoolean(value);
        }


        if (normalized.contains("uuid")) {
            return java.util.UUID.fromString(value);
        }

        if (normalized.contains("timestamp")) {
            return java.sql.Timestamp.valueOf(value);
        }

        if (normalized.contains("date")) {
            return java.sql.Date.valueOf(value);
        }

        if (normalized.contains("time")) {
            return java.sql.Time.valueOf(value);
        }

        return value;
    }

    public void resetArguments() {
        for (int i = 0; i < argumentValues.size(); i++) {
            argumentValues.set(i, "");
            inputFields.get(i).clear();
        }
        currentArgumentIndex = 0;
        focusArgument(0);
        addConsoleMessage("Arguments reset.");
    }

    public void clearResult() {
        showingResult = false;
        lastResult = null;
        tableComponent = null;
    }

    public void toggleConsole() {
        showingConsole = !showingConsole;
        if (showingConsole) {
            showingResult = false;
        }
    }

    public void clearConsole() {
        consoleMessages.clear();
        if (consoleList != null) {
            consoleList.clearButtons();
        }
    }

    public void addConsoleMessage(String message) {
        String timestamp = java.time.LocalTime.now().withNano(0).toString();
        String fullMessage = "[" + timestamp + "] " + message;
        consoleMessages.add(fullMessage);

        if (consoleList != null) {
            addButtonToConsole(fullMessage);
        }
    }

    private void addButtonToConsole(String fullMessage) {
        String displayMessage = fullMessage.replace("\n", " ").replace("\r", "");

        Button messageButton = new Button(displayMessage, Color.web("#111111"), () -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(fullMessage);
            clipboard.setContent(content);
        }, false, false, Color.WHITE);

        consoleList.addButton(messageButton);
    }

    public void focusArgument(int index) {
        if (inputFields.isEmpty()) {
            return;
        }
        currentArgumentIndex = Math.max(0, Math.min(index, inputFields.size() - 1));
        for (int i = 0; i < inputFields.size(); i++) {
            inputFields.get(i).setFocused(i == currentArgumentIndex);
        }
    }

    public void moveToNextArgument(Runnable onLastEnter) {
        if (currentArgumentIndex < inputFields.size() - 1) {
            syncArgumentValuesFromFields();
            focusArgument(currentArgumentIndex + 1);
        } else if (onLastEnter != null) {
            onLastEnter.run();
        }
    }

    public void moveToPreviousArgument() {
        if (currentArgumentIndex > 0) {
            syncArgumentValuesFromFields();
            focusArgument(currentArgumentIndex - 1);
        }
    }

    public void draw(GraphicsContext gc, double x, double y, double width, double height) {
        gc.setFill(Color.web("#FAFAFA"));
        gc.fillRect(x, y, width, height);

        if (showingConsole) {
            drawConsole(gc, x + 10, y + 10, width - 20, height - 20);
            return;
        }

        if (showingResult) {
            drawResult(gc, x + 10, y + 10, width - 20, height - 20);
            return;
        }

        drawArguments(gc, x + 10, y + 10, width - 20, height - 20);
    }

    private void drawArguments(GraphicsContext gc, double x, double y, double width, double height) {
        gc.save();
        drawFunctionTitleBox(gc, x, y, width);
        gc.restore();

        if (parameters.isEmpty()) {
            gc.setFont(LABEL_FONT);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("function is zero-argument", x + width / 2, y + 50);
            return;
        }

        double fieldY = y + 45;
        double fieldHeight = 30;
        double labelHeight = 18;
        double gap = 12;

        for (int i = 0; i < parameters.size(); i++) {
            FunctionParameter param = parameters.get(i);
            TextInputField field = inputFields.get(i);
            field.setPosition(x, fieldY + labelHeight);
            field.setSize(width, fieldHeight);
            field.setLabel(getArgLabel(i, param) + " (" + param.getType() + ")");
            field.draw(gc);

            fieldY += labelHeight + fieldHeight + gap;
            if (fieldY > y + height) {
                break;
            }
        }
    }

    private void drawResult(GraphicsContext gc, double x, double y, double width, double height) {
        gc.save();
        drawFunctionTitleBox(gc, x, y, width);
        gc.restore();

        if (lastResult instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tableData = (List<Map<String, Object>>) lastResult;
            if (tableComponent == null) {
                tableComponent = new TableComponent(x, y + 50, width, height - 45, tableData);
            } else {
                tableComponent.setBounds(x, y + 50, width, height - 45);
            }
            tableComponent.draw(gc);
            return;
        }

        gc.setFill(Color.BLACK);
        gc.setFont(LABEL_FONT);
        String text = lastResult == null ? "null" : lastResult.toString();
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(text, x + width / 2, y + 60, width - 20);
    }

    private void drawFunctionTitleBox(GraphicsContext gc, double x, double y, double width) {
        double textW = TextMetrics.width(TITLE_FONT, displayName);
        double boxW = Math.min(width - 20, textW + 28);
        double boxX = x + (width - boxW) / 2;
        double boxY = y + 6;
        double boxH = 28;

        gc.setFill(Color.web("#DADADA"));
        gc.fillRoundRect(boxX, boxY, boxW, boxH, 8, 8);
        gc.setStroke(Color.web("#BDBDBD"));
        gc.strokeRoundRect(boxX, boxY, boxW, boxH, 8, 8);

        gc.setFill(Color.web("#111111"));
        gc.setFont(TITLE_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(displayName, boxX + boxW / 2, boxY + boxH / 2 + 6);
    }

    private void drawConsole(GraphicsContext gc, double x, double y, double width, double height) {
        if (consoleList == null) {

            consoleList = new ButtonList(x, y, width, height, Color.web("#111111"),
                    ButtonList.ListOrientation.VERTICAL, ButtonList.VerticalButtonWidth.FILL_WIDTH_LEFT, false);


            for (String msg : consoleMessages) {
                addButtonToConsole(msg);
            }

        } else {

            consoleList.setPosition(x, y);
            consoleList.setSize(width, height);
        }

        consoleList.draw(gc);
    }

    public void handleConsoleClick(double x, double y) {
        if (consoleList != null) {
            consoleList.handleMouseClick(x, y);
        }
    }

    public void handleConsoleMouseMove(double x, double y) {
        if (consoleList != null) {
            consoleList.handleMouseMove(x, y);
        }
    }

    public void handleConsoleScroll(double deltaY) {
        if (consoleList != null) {
            consoleList.handleScroll(deltaY);
        }
    }

    public boolean isShowingResult() {
        return showingResult;
    }

    public boolean isShowingConsole() {
        return showingConsole;
    }

    public boolean isTableResult() {
        return showingResult && lastResult instanceof List<?> list
                && !list.isEmpty() && list.get(0) instanceof Map<?, ?>;
    }

    public boolean hasArguments() {
        return !parameters.isEmpty();
    }

    public boolean hasNoOutput() {
        return hasNoOutPrefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFunctionName() {
        return functionInfo.getName();
    }

    public List<TextInputField> getInputFields() {
        return inputFields;
    }

    public TableComponent getTableComponent() {
        return tableComponent;
    }

    public void setTableComponent(TableComponent tableComponent) {
        this.tableComponent = tableComponent;
    }
}

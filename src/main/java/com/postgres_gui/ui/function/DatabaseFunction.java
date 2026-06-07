package com.postgres_gui.ui.function;

import com.postgres_gui.database.DatabaseFunctionInfo;
import com.postgres_gui.database.DatabaseManager;
import com.postgres_gui.database.FunctionParameter;
import com.postgres_gui.ui.AppWindow;
import com.postgres_gui.ui.components.DroplistAutocomplete;
import com.postgres_gui.ui.components.ParameterNameParser;
import com.postgres_gui.ui.components.UiFactory;
import com.postgres_gui.ui.layout.UILayoutSettings;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DatabaseFunction extends StackPane {
    private final DatabaseFunctionInfo functionInfo;
    private final List<FunctionParameter> parameters;
    private final DatabaseManager dbManager;
    private final String displayName;
    private final List<ParameterNameParser.ParseResult> parsedAliases;
    private final boolean hasNoOutPrefix;

    private final List<String> argumentValues = new ArrayList<>();
    private final List<TextField> inputFields = new ArrayList<>();
    private final List<DroplistAutocomplete.RefreshCallback> droplistRefreshCallbacks = new ArrayList<>();

    private final VBox argsLayer = new VBox(UILayoutSettings.INPUT_BLOCK_VERTICAL_SPACING);
    private final ScrollPane argsScroll;
    private final StackPane resultLayer = new StackPane();
    private final TableView<Map<String, Object>> tableView = new TableView<>();
    private final Label scalarResultLabel = new Label();
    private final TextArea consoleArea = new TextArea();

    private Object lastResult;
    private boolean showingResult;
    private boolean showingConsole;
    private Runnable stateChangeListener;
    private AppWindow app;


    private boolean cellSelectionMode = false;
    private int selectedCellRow = -1;
    private int selectedCellColumn = -1;
    private int startCellRow = -1;
    private int startCellColumn = -1;
    private int endCellRow = -1;
    private int endCellColumn = -1;
    private int savedRowIndex = -1; // Сохраняем выбранную строку при входе в режим ячеек

    public DatabaseFunction(AppWindow app,
                            DatabaseFunctionInfo functionInfo,
                            DatabaseManager dbManager,
                            String displayName,
                            List<ParameterNameParser.ParseResult> parsedAliases) throws Exception {
        this.app = app;
        this.functionInfo = functionInfo;
        this.dbManager = dbManager;
        this.displayName = displayName;
        this.parsedAliases = parsedAliases == null ? List.of() : parsedAliases;
        this.hasNoOutPrefix = functionInfo.hasNoOutPrefix();
        this.parameters = dbManager.getFunctionParameters(functionInfo.getName());

        for (FunctionParameter parameter : parameters) {
            argumentValues.add("");
            TextField field = UiFactory.createFullWidthInputField();
            inputFields.add(field);
        }

        argsScroll = new ScrollPane(argsLayer);
        argsScroll.setFitToWidth(true);
        UiFactory.hideScrollBars(argsScroll);

        consoleArea.setEditable(false);
        consoleArea.setWrapText(true);
        consoleArea.setStyle(
                "-fx-control-inner-background: " + UILayoutSettings.CONSOLE_BACKGROUND + ";" +
                        "-fx-background-color: " + UILayoutSettings.CONSOLE_BACKGROUND + ";" +
                        "-fx-text-fill: " + UILayoutSettings.CONSOLE_MESSAGE_COLOR + ";" +
                        "-fx-focus-color: transparent;" +
                        "-fx-faint-focus-color: transparent;" +
                        "-fx-background-insets: 0;" +
                        "-fx-padding: 8;"
        );

        buildUi();
        rebuildArgumentForm();
        addConsoleMessage("Loaded: " + functionInfo.getName());
        showArgsView();
    }

    public void setOnStateChange(Runnable listener) {
        this.stateChangeListener = listener;
    }

    private void fireStateChange() {
        if (stateChangeListener != null) {
            stateChangeListener.run();
        }
    }

    private void buildUi() {
        setStyle("-fx-background-color: " + UILayoutSettings.FUNCTION_CENTER_BACKGROUND + ";");

        Label title = new Label(displayName);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 8 16; "
                + "-fx-background-color: #DADADA; -fx-background-radius: 8; -fx-border-color: #BDBDBD; -fx-border-radius: 8;");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        argsLayer.setPadding(new Insets(12));
        VBox argsRoot = new VBox(12, title, argsScroll);
        VBox.setVgrow(argsScroll, Priority.ALWAYS);
        argsRoot.setPadding(new Insets(12));

        scalarResultLabel.setWrapText(true);
        scalarResultLabel.setAlignment(Pos.CENTER);
        scalarResultLabel.setFont(UILayoutSettings.INPUT_FIELD_FONT);
        resultLayer.getChildren().addAll(tableView, scalarResultLabel);
        StackPane.setAlignment(scalarResultLabel, Pos.CENTER);
        tableView.setStyle("-fx-background-color: transparent;");
        tableView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.addEventFilter(KeyEvent.KEY_PRESSED, this::handleTableKeyCopy);

        VBox consoleRoot = new VBox(consoleArea);
        VBox.setVgrow(consoleArea, Priority.ALWAYS);
        consoleRoot.setPadding(new Insets(12));
        consoleRoot.setStyle("-fx-background-color: " + UILayoutSettings.CONSOLE_BACKGROUND + ";");

        getChildren().addAll(argsRoot, resultLayer, consoleRoot);

        argsRoot.prefWidthProperty().bind(widthProperty());
        argsRoot.prefHeightProperty().bind(heightProperty());
        resultLayer.prefWidthProperty().bind(widthProperty());
        resultLayer.prefHeightProperty().bind(heightProperty());
        consoleRoot.prefWidthProperty().bind(widthProperty());
        consoleRoot.prefHeightProperty().bind(heightProperty());
    }

    private void rebuildArgumentForm() {
        argsLayer.getChildren().clear();
        droplistRefreshCallbacks.clear();

        if (parameters.isEmpty()) {
            Label hint = UiFactory.createZeroArgumentPlate(UILayoutSettings.ZERO_ARGUMENT_MESSAGE);
            VBox centered = new VBox(hint);
            centered.setAlignment(Pos.TOP_CENTER);
            centered.setPadding(new Insets(UILayoutSettings.INPUT_BLOCK_VERTICAL_SPACING, 12, 0, 12));
            argsLayer.getChildren().add(centered);
            return;
        }

        for (int i = 0; i < parameters.size(); i++) {
            FunctionParameter param = parameters.get(i);
            TextField field = inputFields.get(i);

            String plateText = param.getName();
            ParameterNameParser.DroplistSpec spec = new ParameterNameParser.DroplistSpec(ParameterNameParser.DroplistKind.NONE, null, null, null);

            if (i < parsedAliases.size()) {
                ParameterNameParser.ParseResult pr = parsedAliases.get(i);

                if (pr.displayAlias() != null && !pr.displayAlias().isBlank()) {
                    plateText = pr.displayAlias();
                }

                spec = pr.spec();

                if (pr.errorMessage() != null) {
                    addConsoleMessage("Error: " + pr.errorMessage());
                }
            }
            //addConsoleMessage("Loaded parameter " + plateText + " with sql name " + param.getName() + "of type" + param.getType());
            ScrollPane droplist = DroplistAutocomplete.attach(field, spec, dbManager, droplistRefreshCallbacks);

            final int index = i;
            field.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.UP) {
                    if (index > 0) {
                        TextField prevField = inputFields.get(index - 1);
                        prevField.requestFocus();
                        prevField.positionCaret(prevField.getText().length());
                        e.consume();
                    }
                } else if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    syncArgumentValuesFromFields();
                    if (index < inputFields.size() - 1) {
                        TextField nextField = inputFields.get(index + 1);
                        nextField.requestFocus();
                        nextField.positionCaret(nextField.getText().length());
                        e.consume();
                    }
                } else if (e.getCode() == javafx.scene.input.KeyCode.BACK_SPACE) {
                    if (field.getText().isEmpty() && index > 0) {
                        TextField prevField = inputFields.get(index - 1);
                        prevField.requestFocus();
                        prevField.positionCaret(prevField.getText().length());
                        e.consume();
                    }
                }
            });

            VBox block = UiFactory.createLabeledInputWithDroplist(plateText, field, droplist, -1);
            argsLayer.getChildren().add(block);
        }
    }


    public static Optional<String> resolveDisplayLabel(String parameterName, String aliasFromConfig) {
        if (aliasFromConfig != null && !aliasFromConfig.isBlank()) {
            return Optional.of(aliasFromConfig.trim());
        }
        return Optional.of(parameterName);
    }

    public void syncArgumentValuesFromFields() {
        for (int i = 0; i < inputFields.size(); i++) {
            argumentValues.set(i, inputFields.get(i).getText());
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
            addConsoleMessage("Successfully executed without output.");
            showingResult = false;
            lastResult = null;
            showArgsView();
        } else {
            addConsoleMessage("Successfully executed.");
            lastResult = result;
            showingResult = true;
            showingConsole = false;
            if (result instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tableData = (List<Map<String, Object>>) result;
                lastResult = tableData;
            }
            showResultView();
        }
        fireStateChange();
    }

    private Object convertValue(String value, String type) throws SQLException {
        if (dbManager.isPostgresEnumType(type)) {
            return dbManager.toPgEnum(type, value);
        }

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
        if (normalized.contains("date") && !normalized.contains("timestamp")) {
            return java.sql.Date.valueOf(value);
        }
        if (normalized.contains("time") && !normalized.contains("timestamp")) {
            return java.sql.Time.valueOf(value);
        }
        return value;
    }

    public void resetArguments() {
        for (int i = 0; i < argumentValues.size(); i++) {
            argumentValues.set(i, "");
            inputFields.get(i).clear();
        }
        //addConsoleMessage("Arguments reset.");
        if (!inputFields.isEmpty()) {
            inputFields.getFirst().requestFocus();
        }
        refreshDroplistValues();
    }

    public void clearResult() {
        showingResult = false;
        lastResult = null;
        showArgsView();
        fireStateChange();
    }

    public void toggleConsole() {
        showingConsole = !showingConsole;
        if (showingConsole) {
            showingResult = false;
            showConsoleView();
        } else if (showingResult) {
            showResultView();
        } else {
            showArgsView();
        }
        fireStateChange();
    }

    public void clearConsole() {
        consoleArea.clear();
    }

    public void addConsoleMessage(String message) {
        String timestamp = java.time.LocalTime.now().withNano(0).toString();
        String formattedMessage = "[" + timestamp + "] " + message;

        if (!consoleArea.getText().isEmpty()) {
            consoleArea.appendText("\n\n");
        }

        consoleArea.appendText(formattedMessage);

        Platform.runLater(() -> consoleArea.positionCaret(consoleArea.getText().length()));
    }

    private void refreshDroplistValuesInternal() {
        for (DroplistAutocomplete.RefreshCallback callback : droplistRefreshCallbacks) {
            callback.refresh();
        }
    }

    public void refreshDroplistValues() {
        refreshDroplistValuesInternal();
    }

    private void showArgsView() {
        showingConsole = false;
        if (!showingResult) {
            setLayerVisible(0, true);
            setLayerVisible(1, false);
            setLayerVisible(2, false);
            refreshDroplistValuesInternal();
        }
    }

    private void showResultView() {
        setLayerVisible(0, false);
        setLayerVisible(1, true);
        setLayerVisible(2, false);
        populateResult();
    }

    private void showConsoleView() {
        setLayerVisible(0, false);
        setLayerVisible(1, false);
        setLayerVisible(2, true);
    }

    private void setLayerVisible(int index, boolean visible) {
        var node = getChildren().get(index);
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void populateResult() {
        if (lastResult instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tableData = (List<Map<String, Object>>) lastResult;
            tableView.setVisible(true);
            tableView.setManaged(true);
            scalarResultLabel.setVisible(false);
            scalarResultLabel.setManaged(false);
            bindTable(tableData);
            return;
        }

        // Скалярный результат - отображаем в мини-таблице вместо Label
        tableView.setVisible(true);
        tableView.setManaged(true);
        scalarResultLabel.setVisible(false);
        scalarResultLabel.setManaged(false);

        // Создаем однострочную таблицу для скалярного результата
        List<Map<String, Object>> scalarData = new ArrayList<>();
        Map<String, Object> resultRow = new java.util.HashMap<>();
        resultRow.put("Result", lastResult == null ? "null" : lastResult.toString());
        scalarData.add(resultRow);
        bindTable(scalarData);
    }

    private void bindTable(List<Map<String, Object>> tableData) {
        tableView.getColumns().clear();
        if (tableData.isEmpty()) {
            tableView.setItems(FXCollections.observableArrayList());
            return;
        }

        Map<String, Object> firstRow = tableData.getFirst();
        List<String> columns = new ArrayList<>(firstRow.keySet());
        ObservableList<Map<String, Object>> items = FXCollections.observableArrayList(tableData);

        for (String column : columns) {
            TableColumn<Map<String, Object>, String> col = new TableColumn<>();
            Label headerLabel = new Label(column);
            headerLabel.setAlignment(Pos.CENTER);
            headerLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(headerLabel, Priority.ALWAYS);

            Region sortArrowReserve = new Region();
            sortArrowReserve.setMinWidth(14);
            sortArrowReserve.setPrefWidth(14);

            HBox headerBox = new HBox(headerLabel, sortArrowReserve);
            headerBox.setAlignment(Pos.CENTER);
            headerBox.setFillHeight(true);
            HBox.setHgrow(headerLabel, Priority.ALWAYS);

            col.setGraphic(headerBox);
            col.setText(null);
            col.setSortable(true);
            col.setCellValueFactory(data -> {
                Object value = data.getValue().get(column);
                return new javafx.beans.property.SimpleStringProperty(value == null ? "" : value.toString());
            });

            // Подсвечиваем выделенные ячейки
            col.setCellFactory(colFactory -> new TableCell<Map<String, Object>, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || getTableRow() == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }

                    setText(item);
                    int row = getTableRow().getIndex();
                    int col = getTableView().getColumns().indexOf(getTableColumn());

                    // Подсвечиваем выделенную ячейку (синий цвет как в строках)
                    if (cellSelectionMode && isCellInSelection(row, col)) {
                        setStyle("-fx-background-color: #0093ff; -fx-text-fill: white;");
                    } else {
                        setStyle("");
                    }
                }
            });

            col.setPrefWidth(140);
            col.setStyle("-fx-alignment: CENTER;");
            tableView.getColumns().add(col);
        }
        tableView.setItems(items);
    }

    public void exportTableToCsv(Path path) throws Exception {
        if (!isTableResult()) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) lastResult;
        if (rows.isEmpty()) {
            return;
        }

        String delimiter = ";";
        List<String> columns = new ArrayList<>(rows.getFirst().keySet());
        StringBuilder sb = new StringBuilder();

        sb.append("sep=").append(delimiter).append('\n');

        sb.append(String.join(delimiter, columns)).append('\n');
        for (Map<String, Object> row : rows) {
            List<String> values = new ArrayList<>();
            for (String column : columns) {
                Object value = row.get(column);
                values.add(escapeCsv(value == null ? "" : value.toString()));
            }
            sb.append(String.join(delimiter, values)).append('\n');
        }
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = sb.toString().getBytes(StandardCharsets.UTF_8);

        OutputStream os = Files.newOutputStream(path);
        os.write(bom);
        os.write(content);
        os.close();
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void handleTableKeyCopy(KeyEvent event) {
        if (!showingResult) {
            return;
        }

        MultipleSelectionModel<Map<String, Object>> selectionModel = tableView.getSelectionModel();
        int currentRowIndex = tableView.getFocusModel().getFocusedIndex();
        List<String> columns = getTableColumns();
        int columnCount = columns.size();

        // Ctrl+C - копирование
        if (event.isControlDown() && event.getCode() == KeyCode.C) {
            if (cellSelectionMode && selectedCellRow >= 0 && selectedCellColumn >= 0) {
                copyCells(columns);
            } else {
                copyRows();
            }
            event.consume();
            return;
        }

        // UP/DOWN - навигация по строкам
        if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
            if (cellSelectionMode) {
                cellSelectionMode = false;
                selectedCellColumn = -1;
                startCellRow = -1;
                startCellColumn = -1;
                endCellRow = -1;
                endCellColumn = -1;
                if (selectedCellRow >= 0) {
                    currentRowIndex = selectedCellRow;
                }
                selectedCellRow = -1;

                tableView.refresh();
            }

            int newIndex = currentRowIndex;
            if (event.getCode() == KeyCode.UP && currentRowIndex > 0) {
                newIndex = currentRowIndex - 1;
            } else if (event.getCode() == KeyCode.DOWN && currentRowIndex < tableView.getItems().size() - 1) {
                newIndex = currentRowIndex + 1;
            }

            if (event.isShiftDown()) {
                // Shift - добавляем строку к выбранным (работает с Shift+мышь)
                if (!selectionModel.getSelectedIndices().contains(newIndex)) {
                    selectionModel.select(newIndex);
                }
            } else {
                // Без Shift - выбираем только эту строку
                selectionModel.clearSelection();
                selectionModel.select(newIndex);
                tableView.getFocusModel().focus(newIndex);
            }

            tableView.scrollTo(newIndex);
            event.consume();
            return;
        }

        // LEFT/RIGHT - навигация по ячейкам
        if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.RIGHT) {
            if (columnCount == 0) {
                return;
            }

            if (!cellSelectionMode) {
                // Входим в режим выбора ячеек - обе стрелки выбирают первую ячейку
                cellSelectionMode = true;
                selectionModel.clearSelection();

                selectedCellRow = currentRowIndex;
                selectedCellColumn = 0;
                startCellRow = currentRowIndex;
                startCellColumn = 0;
                endCellRow = currentRowIndex;
                endCellColumn = 0;
            } else {
                // Уже в режиме выбора ячеек - движемся влево/вправо
                if (event.getCode() == KeyCode.LEFT) {
                    if (selectedCellColumn > 0) {
                        selectedCellColumn--;
                    } else if (selectedCellRow > 0) {
                        selectedCellRow--;
                        selectedCellColumn = columnCount - 1;
                    }
                } else if (event.getCode() == KeyCode.RIGHT) {
                    if (selectedCellColumn < columnCount - 1) {
                        selectedCellColumn++;
                    } else if (selectedCellRow < tableView.getItems().size() - 1) {
                        selectedCellRow++;
                        selectedCellColumn = 0;
                    }
                }

                // Если Shift - расширяем выделение
                if (event.isShiftDown()) {
                    endCellRow = selectedCellRow;
                    endCellColumn = selectedCellColumn;
                } else {
                    // Без Shift - сбрасываем начало диапазона
                    startCellRow = selectedCellRow;
                    startCellColumn = selectedCellColumn;
                    endCellRow = selectedCellRow;
                    endCellColumn = selectedCellColumn;
                }
            }

            // Визуально обновляем подсвеченную ячейку
            updateCellHighlight();
            tableView.scrollTo(selectedCellRow);
            event.consume();
            return;
        }
    }

    /**
     * Обновляет визуальное отображение выделенной ячейки
     */
    private void updateCellHighlight() {
        if (!cellSelectionMode || selectedCellRow < 0 || selectedCellColumn < 0) {
            return;
        }

        List<String> columns = getTableColumns();
        if (selectedCellColumn >= columns.size() || selectedCellRow >= tableView.getItems().size()) {
            return;
        }

        // Перерисовываем таблицу для обновления стилей ячеек
        tableView.refresh();
    }

    /**
     * Проверяет, находится ли ячейка в диапазоне выделения
     */
    private boolean isCellInSelection(int row, int col) {
        if (startCellRow == -1 || startCellColumn == -1 || endCellRow == -1 || endCellColumn == -1) {
            return row == selectedCellRow && col == selectedCellColumn;
        }

        int minRow = Math.min(startCellRow, endCellRow);
        int maxRow = Math.max(startCellRow, endCellRow);
        int minCol = Math.min(startCellColumn, endCellColumn);
        int maxCol = Math.max(startCellColumn, endCellColumn);

        return row >= minRow && row <= maxRow && col >= minCol && col <= maxCol;
    }

    private List<String> getTableColumns() {
        if (tableView.getItems().isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(tableView.getItems().get(0).keySet());
    }

    private void copyCells(List<String> columns) {
        if (selectedCellRow < 0 || selectedCellColumn < 0 || selectedCellRow >= tableView.getItems().size()) {
            return;
        }

        StringBuilder clipboard = new StringBuilder();

        int minRow = Math.min(startCellRow, endCellRow);
        int maxRow = Math.max(startCellRow, endCellRow);
        int minCol = Math.min(startCellColumn, endCellColumn);
        int maxCol = Math.max(startCellColumn, endCellColumn);

        for (int row = minRow; row <= maxRow; row++) {
            List<String> rowValues = new ArrayList<>();
            for (int col = minCol; col <= maxCol; col++) {
                if (col < columns.size()) {
                    Map<String, Object> rowData = tableView.getItems().get(row);
                    Object cellValue = rowData.get(columns.get(col));
                    rowValues.add(cellValue == null ? "" : cellValue.toString());
                }
            }
            clipboard.append(String.join("; ", rowValues)).append(" \n");
        }

        Clipboard systemClipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(clipboard.toString());
        systemClipboard.setContent(content);

        int cellCount = (maxRow - minRow + 1) * (maxCol - minCol + 1);
        app.showSuccess("Copied " + cellCount + " cells to clipboard.");
    }

    private void copyRows() {
        ObservableList<Map<String, Object>> selectedRows = tableView.getSelectionModel().getSelectedItems();

        if (selectedRows == null || selectedRows.isEmpty()) {
            return;
        }

        List<String> columns = getTableColumns();
        List<String> rowStrings = new ArrayList<>();

        for (Map<String, Object> row : selectedRows) {
            List<String> values = new ArrayList<>();
            for (String column : columns) {
                Object value = row.get(column);
                values.add(value == null ? "" : value.toString());
            }
            rowStrings.add(String.join("; ", values));
        }

        String clipboardContent = String.join(" \n", rowStrings);

        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(clipboardContent);
        clipboard.setContent(content);

        app.showSuccess("Copied " + selectedRows.size() + " rows to clipboard.");
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

    public String getDisplayName() {
        return displayName;
    }

    public String getFunctionName() {
        return functionInfo.getName();
    }

    public List<TextField> getInputFields() {
        return inputFields;
    }

    public void executeFromLastField(Runnable onSuccess, Runnable onError) {
        syncArgumentValuesFromFields();
        try {
            execute();
            resetArguments();
            onSuccess.run();
        } catch (Exception ex) {
            addConsoleMessage("ERROR: " + ex.getMessage());
            onError.run();
        }
    }
}
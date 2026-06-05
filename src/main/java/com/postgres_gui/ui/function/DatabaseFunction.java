package com.postgres_gui.ui.function;

import com.postgres_gui.database.DatabaseFunctionInfo;
import com.postgres_gui.database.DatabaseManager;
import com.postgres_gui.database.FunctionParameter;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

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

    public DatabaseFunction(DatabaseFunctionInfo functionInfo,
                            DatabaseManager dbManager,
                            String displayName,
                            List<ParameterNameParser.ParseResult> parsedAliases) throws Exception {
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
            field.setOnAction(e -> {
                syncArgumentValuesFromFields();
                if (index < inputFields.size() - 1) {
                    inputFields.get(index + 1).requestFocus();
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

        tableView.setVisible(false);
        tableView.setManaged(false);
        scalarResultLabel.setVisible(true);
        scalarResultLabel.setManaged(true);
        scalarResultLabel.setText(lastResult == null ? "null" : lastResult.toString());
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
        List<String> columns = new ArrayList<>(rows.getFirst().keySet());
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", columns)).append('\n');
        for (Map<String, Object> row : rows) {
            List<String> values = new ArrayList<>();
            for (String column : columns) {
                Object value = row.get(column);
                values.add(escapeCsv(value == null ? "" : value.toString()));
            }
            sb.append(String.join(",", values)).append('\n');
        }
        java.nio.file.Files.writeString(path, sb.toString());
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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
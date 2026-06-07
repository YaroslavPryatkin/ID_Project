package com.postgres_gui.ui.screens;

import com.postgres_gui.config.ScreenConfig;
import com.postgres_gui.database.DatabaseFunctionInfo;
import com.postgres_gui.ui.AppWindow;
import com.postgres_gui.ui.components.UiFactory;
import com.postgres_gui.ui.function.DatabaseFunction;
import com.postgres_gui.ui.layout.FunctionScreenPalette;
import com.postgres_gui.ui.layout.UILayoutSettings;
import com.postgres_gui.util.ColorUtils;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FunctionScreen extends Screen {
    private final ScreenConfig screenConfig;
    private final Map<String, DatabaseFunction> functions = new LinkedHashMap<>();
    private final BorderPane layout = new BorderPane();

    private final double leftPanelWidth;
    private final Label screenTitlePlate;
    private final HBox topBar = new HBox(UILayoutSettings.TOP_BAR_BUTTON_GAP);
    private final VBox functionButtonsBox = new VBox(UILayoutSettings.FUNCTION_LIST_BUTTON_SPACING);
    private final StackPane centerHost = new StackPane();
    private final HBox bottomBar = new HBox();
    private final HBox actionButtonsHost = new HBox(UILayoutSettings.BOTTOM_TOOLBAR_BUTTON_GAP);

    private final Button applyButton;
    private final Button applyResetButton;
    private final Button resetButton;
    private final Button consoleButton;
    private final Button clearButton;
    private final Button doneButton;
    private final Button exportButton;

    private final List<Button> actionButtons = new ArrayList<>();

    private FunctionScreenPalette palette;
    private DatabaseFunction selectedFunction;
    private Button selectedListButton;

    public FunctionScreen(AppWindow app, ScreenConfig screenConfig) {
        super(app);
        this.screenConfig = screenConfig;
        this.palette = FunctionScreenPalette.fromBase(Color.web(screenConfig.getColor()));
        this.leftPanelWidth = Math.max(
                UILayoutSettings.FUNCTION_LIST_PANEL_MIN_WIDTH,
                screenConfig.getLeftPanelWidth() * UILayoutSettings.FUNCTION_LIST_PANEL_WIDTH_FACTOR
        );

        applyButton = UiFactory.createToolbarButton("Apply", palette.toolbarButtonHex(), palette.toolbarButtonHoverHex());
        applyResetButton = UiFactory.createToolbarButton("Apply and Reset", palette.toolbarButtonHex(), palette.toolbarButtonHoverHex());
        resetButton = UiFactory.createToolbarButton("Reset", palette.toolbarButtonHex(), palette.toolbarButtonHoverHex());
        consoleButton = UiFactory.createToolbarButton("Console", palette.toolbarButtonHex(), palette.toolbarButtonHoverHex());
        clearButton = UiFactory.createToolbarButton("Clear", palette.toolbarButtonHex(), palette.toolbarButtonHoverHex());
        doneButton = UiFactory.createToolbarButton("Done", palette.toolbarButtonHex(), palette.toolbarButtonHoverHex());
        exportButton = UiFactory.createToolbarButton("Export to Excel", palette.toolbarButtonHex(), palette.toolbarButtonHoverHex());

        actionButtons.addAll(List.of(applyButton, applyResetButton, resetButton, doneButton, exportButton, clearButton));

        screenTitlePlate = new Label(screenConfig.getDisplayName());
        buildLayout();
        loadFunctions();
        getChildren().add(layout);

        this.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.isAltDown()) {
                if (e.getCode() == javafx.scene.input.KeyCode.UP) {
                    switchFunction(-1);
                } else if (e.getCode() == javafx.scene.input.KeyCode.DOWN) {
                    switchFunction(1);
                } else if (e.getCode() == javafx.scene.input.KeyCode.LEFT) {
                    switchScreen(-1);
                } else if (e.getCode() == javafx.scene.input.KeyCode.RIGHT) {
                    switchScreen(1);
                } else if (e.getCode() == KeyCode.Q) {
                    if(!selectedFunction.isShowingConsole() && !selectedFunction.isShowingResult())
                        runApply();
                    else if(selectedFunction.isShowingConsole())
                        runClearConsoleButton();
                    else if(selectedFunction.isShowingResult())
                        runDoneButton();
                }
                else if (e.getCode() == KeyCode.W) {
                    if(!selectedFunction.isShowingConsole() && !selectedFunction.isShowingResult())
                        runApplyAndReset();
                    else if(selectedFunction.isShowingResult())
                        exportTable();
                } else if (e.getCode() == KeyCode.E && !selectedFunction.isShowingConsole() && !selectedFunction.isShowingResult()) {
                    runReset();
                } else if (e.getCode() == KeyCode.R) {
                    runConsoleButton();
                }
                e.consume();
            } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                if (selectedFunction != null) {
                    if (selectedFunction.isShowingConsole()) {
                        selectedFunction.toggleConsole();
                        e.consume();
                    } else if (selectedFunction.isShowingResult()) {
                        selectedFunction.clearResult();
                        e.consume();
                    }
                }
            } else if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                boolean isTextFieldFocused = getScene() != null &&
                        getScene().getFocusOwner() instanceof javafx.scene.control.TextInputControl;

                if (!isTextFieldFocused && selectedFunction != null) {
                    if (selectedFunction.isShowingConsole()) {
                        selectedFunction.toggleConsole();
                    } else if (selectedFunction.isShowingResult()) {
                        selectedFunction.clearResult();
                    } else {
                        if (selectedFunction.hasArguments()) {
                            List<TextField> fields = selectedFunction.getInputFields();
                            if (!fields.isEmpty()) {
                                TextField firstField = fields.getFirst();
                                firstField.requestFocus();
                                firstField.positionCaret(firstField.getText().length());
                            }
                        } else {
                            selectedFunction.executeFromLastField(
                                    () -> app.showSuccess("Success"),
                                    () -> app.showError("Error")
                            );
                        }
                    }
                    e.consume();
                }
            }
        });
    }

    private void buildLayout() {
        String border = palette.panelBorderHex();
        layout.prefWidthProperty().bind(widthProperty());
        layout.prefHeightProperty().bind(heightProperty());
        layout.setStyle("-fx-background-color: " + palette.baseHex() + ";");

        screenTitlePlate.setFont(UILayoutSettings.NAV_SCREEN_BUTTON_FONT);
        screenTitlePlate.setAlignment(Pos.CENTER);
        screenTitlePlate.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        screenTitlePlate.setWrapText(true);
        screenTitlePlate.setMinSize(leftPanelWidth, UILayoutSettings.TOP_BAR_HEIGHT);
        screenTitlePlate.setPrefSize(leftPanelWidth, UILayoutSettings.TOP_BAR_HEIGHT);
        screenTitlePlate.setMaxSize(leftPanelWidth, UILayoutSettings.TOP_BAR_HEIGHT);
        screenTitlePlate.setStyle(
                "-fx-background-color: " + palette.screenTitleHex() + ";"
                        + "-fx-text-fill: " + UILayoutSettings.NAV_BUTTON_TEXT_COLOR + ";"
                        + " -fx-font-weight: bold; -fx-padding: 4 8;"
        );

        String navHover = FunctionScreenPalette.toHex(
                ColorUtils.darken(Color.web("#9E9E9E"), UILayoutSettings.FUNCTION_COLOR_NAV_BUTTON_HOVER_DARKEN));
        Button backButton = UiFactory.createNavScreenButton("Return to main menu", "#9E9E9E", navHover);
        backButton.setOnAction(e -> app.showStartScreen());

        topBar.setPadding(new Insets(0, UILayoutSettings.TOP_BAR_HORIZONTAL_PADDING, 0, 0));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setMinHeight(UILayoutSettings.TOP_BAR_HEIGHT);
        topBar.setPrefHeight(UILayoutSettings.TOP_BAR_HEIGHT);
        topBar.setMaxHeight(UILayoutSettings.TOP_BAR_HEIGHT);
        topBar.setStyle(
                "-fx-background-color: " + palette.topBarHex() + ";"
                        + UiFactory.panelBorderStyle(border, false, false, true, false)
        );
        topBar.getChildren().add(screenTitlePlate);
        topBar.getChildren().add(backButton);

        for (ScreenConfig other : app.getConfig().getScreens()) {
            if (other.getPrefix().equals(screenConfig.getPrefix())) {
                Button currentScreenButton = UiFactory.createStaticNavbarButton(
                        other.getDisplayName(), palette.screenButtonSelectedHex());
                topBar.getChildren().add(currentScreenButton);
                continue;
            }
            String otherHover = FunctionScreenPalette.toHex(
                    ColorUtils.darken(Color.web(other.getColor()), UILayoutSettings.FUNCTION_COLOR_NAV_BUTTON_HOVER_DARKEN));
            Button nav = UiFactory.createNavScreenButton(other.getDisplayName(), other.getColor(), otherHover);
            nav.setOnAction(e -> app.showFunctionScreen(other.getPrefix()));
            topBar.getChildren().add(nav);
        }

        functionButtonsBox.setFillWidth(true);
        functionButtonsBox.setMaxWidth(leftPanelWidth);
        functionButtonsBox.setPadding(new Insets(
                UILayoutSettings.FUNCTION_LIST_VERTICAL_PADDING, UILayoutSettings.FUNCTION_LIST_HORIZONTAL_PADDING,
                UILayoutSettings.FUNCTION_LIST_VERTICAL_PADDING, UILayoutSettings.FUNCTION_LIST_HORIZONTAL_PADDING));
        javafx.scene.control.ScrollPane functionScroll = new javafx.scene.control.ScrollPane(functionButtonsBox);
        functionScroll.setFitToWidth(true);
        UiFactory.hideScrollBars(functionScroll);
        VBox.setVgrow(functionScroll, Priority.ALWAYS);

        VBox leftPanel = new VBox(functionScroll);
        leftPanel.setMinWidth(leftPanelWidth);
        leftPanel.setPrefWidth(leftPanelWidth);
        leftPanel.setMaxWidth(leftPanelWidth);
        leftPanel.setStyle(
                "-fx-background-color: " + palette.listPanelHex() + ";"
                        + UiFactory.panelBorderStyle(border, false, true, false, false)
        );

        centerHost.setStyle("-fx-background-color: " + UILayoutSettings.FUNCTION_CENTER_BACKGROUND + ";");

        actionButtonsHost.setAlignment(Pos.CENTER_LEFT);
        actionButtonsHost.prefWidthProperty().bind(
                Bindings.multiply(
                        bottomBar.widthProperty(),
                        1.0 - UILayoutSettings.BOTTOM_TOOLBAR_CONSOLE_WIDTH_FRACTION
                ).subtract(UILayoutSettings.BOTTOM_TOOLBAR_CONSOLE_GAP)
        );
        consoleButton.prefWidthProperty().bind(
                Bindings.multiply(bottomBar.widthProperty(), UILayoutSettings.BOTTOM_TOOLBAR_CONSOLE_WIDTH_FRACTION)
        );
        HBox.setMargin(consoleButton, new Insets(0, 0, 0, UILayoutSettings.BOTTOM_TOOLBAR_CONSOLE_GAP));

        bottomBar.setSpacing(0);
        bottomBar.setMinHeight(UILayoutSettings.BOTTOM_TOOLBAR_HEIGHT);
        bottomBar.setPrefHeight(UILayoutSettings.BOTTOM_TOOLBAR_HEIGHT);
        bottomBar.setMaxHeight(UILayoutSettings.BOTTOM_TOOLBAR_HEIGHT);
        bottomBar.setPadding(new Insets(
                UILayoutSettings.BOTTOM_TOOLBAR_INNER_PADDING,
                UILayoutSettings.TOP_BAR_HORIZONTAL_PADDING,
                UILayoutSettings.BOTTOM_TOOLBAR_INNER_PADDING,
                UILayoutSettings.TOP_BAR_HORIZONTAL_PADDING
        ));
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setStyle(
                "-fx-background-color: " + palette.bottomBarHex() + ";"
                        + UiFactory.panelBorderStyle(border, true, false, false, false)
        );
        bottomBar.getChildren().addAll(actionButtonsHost, consoleButton);

        applyButton.setOnAction(e -> runApply());
        applyResetButton.setOnAction(e -> runApplyAndReset());
        resetButton.setOnAction(e -> runReset());
        consoleButton.setOnAction(e -> runConsoleButton());
        clearButton.setOnAction(e -> runClearConsoleButton());
        doneButton.setOnAction(e -> runDoneButton());
        exportButton.setOnAction(e -> exportTable());

        BorderPane centerArea = new BorderPane();
        centerArea.setCenter(centerHost);
        centerArea.setBottom(bottomBar);

        layout.setTop(topBar);
        layout.setLeft(leftPanel);
        layout.setCenter(centerArea);

        app.setNotificationReservedBottom(UILayoutSettings.BOTTOM_TOOLBAR_RESERVED_FOR_NOTIFICATIONS);
        app.setNotificationReservedRight(0);
    }

    private void loadFunctions() {
        functions.clear();
        functionButtonsBox.getChildren().clear();
        selectedListButton = null;
        try {
            List<DatabaseFunctionInfo> infos = app.getDatabaseManager()
                    .getFunctionsByPrefix(screenConfig.getPrefix());
            //System.out.println("Adding functions in amount of " + infos.size());
            for (DatabaseFunctionInfo info : infos) {
                String display = screenConfig.getDisplayNameForFunction(info.getName());
                DatabaseFunction function = new DatabaseFunction(
                        app,
                        info,
                        app.getDatabaseManager(),
                        display,
                        screenConfig.getParsedArgAliasesForFunction(info.getName())
                );
                function.setOnStateChange(this::updateToolbar);
                wireFunctionFields(function);
                //System.out.println("Adding new function: " + info.getName());
                functions.put(info.getName(), function);

                Button listButton = UiFactory.createFunctionListButton(
                        display, palette.listButtonHex(), palette.listButtonHoverHex());
                listButton.setMaxWidth(Double.MAX_VALUE);
                listButton.getProperties().put("displayText", display);
                String functionName = info.getName();
                listButton.setOnAction(e -> selectFunction(functionName, listButton));
                functionButtonsBox.getChildren().add(listButton);
            }
        } catch (Exception e) {
            app.showError("Failed to load functions");
        }
        if (!functions.isEmpty() && functionButtonsBox.getChildren().getFirst() instanceof Button first) {
            selectFunction(functions.keySet().iterator().next(), first);
        } else {
            centerHost.getChildren().clear();
            selectedFunction = null;
            updateToolbar();
        }
    }

    private void wireFunctionFields(DatabaseFunction function) {
        List<TextField> fields = function.getInputFields();
        if (fields.isEmpty()) {
            return;
        }
        TextField lastField = fields.getLast();
        lastField.setOnAction(e -> function.executeFromLastField(
                () -> app.showSuccess("Success"),
                () -> app.showError("Error")
        ));
    }

    private void selectFunction(String functionName, Button listButton) {
        if (selectedListButton != null) {
            applyListButtonState(selectedListButton, false);
        }
        selectedListButton = listButton;
        applyListButtonState(selectedListButton, true);

        selectedFunction = functions.get(functionName);
        if (selectedFunction != null) {
            centerHost.getChildren().setAll(selectedFunction);
            if (!selectedFunction.getInputFields().isEmpty()) {
                selectedFunction.getInputFields().getFirst().requestFocus();
            }
            // Refresh droplist values when function is selected
            selectedFunction.refreshDroplistValues();
        } else {
            centerHost.getChildren().clear();
        }
        updateToolbar();
    }
    @Override
    public void onShow(){
        if(selectedFunction!=null){
            selectedFunction.refreshDroplistValues();
        }
    }

    private void applyListButtonState(Button button, boolean selected) {
        Object label = button.getProperties().get("displayText");
        if (label instanceof String displayText) {
            button.setText(displayText);
        }
        String normal = selected ? palette.listButtonSelectedHex() : palette.listButtonHex();
        button.setStyle(listButtonStyle(normal, selected));
        String hover = palette.listButtonHoverHex();
        button.setOnMouseEntered(e -> {
            if (!selected) {
                button.setStyle(listButtonStyle(hover, false));
                restoreListButtonText(button);
            }
        });
        button.setOnMouseExited(e -> {
            button.setStyle(listButtonStyle(normal, selected));
            restoreListButtonText(button);
        });
    }

    private void restoreListButtonText(Button button) {
        Object label = button.getProperties().get("displayText");
        if (label instanceof String displayText) {
            button.setText(displayText);
        }
    }

    private String listButtonStyle(String background, boolean selected) {
        return String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: %.0f; "
                        + "-fx-border-color: %s; -fx-border-width: %.0f; -fx-border-radius: %.0f; "
                        + "-fx-padding: %.0f %.0f; -fx-alignment: CENTER-LEFT; -fx-cursor: hand; "
                        + "-fx-font-size: %.0fpx; -fx-font-family: \"%s\";%s",
                background,
                UILayoutSettings.FUNCTION_LIST_BUTTON_TEXT_COLOR,
                UILayoutSettings.FUNCTION_LIST_BUTTON_CORNER_RADIUS,
                UILayoutSettings.BUTTON_BORDER_COLOR,
                UILayoutSettings.BUTTON_BORDER_WIDTH,
                UILayoutSettings.FUNCTION_LIST_BUTTON_CORNER_RADIUS,
                UILayoutSettings.LIST_BUTTON_PADDING_VERTICAL,
                UILayoutSettings.LIST_BUTTON_PADDING_HORIZONTAL,
                UILayoutSettings.FUNCTION_LIST_BUTTON_FONT.getSize(),
                UILayoutSettings.FUNCTION_LIST_BUTTON_FONT.getFamily(),
                selected ? " -fx-font-weight: bold;" : ""
        );
    }

    private void updateToolbar() {
        boolean hasFunction = selectedFunction != null;
        boolean hasArgs = hasFunction && selectedFunction.hasArguments();
        boolean console = hasFunction && selectedFunction.isShowingConsole();
        boolean table = hasFunction && selectedFunction.isTableResult();

        setButtonVisible(applyButton, hasFunction && !console && !table);
        setButtonVisible(applyResetButton, hasFunction && hasArgs && !console && !table);
        setButtonVisible(resetButton, hasFunction && hasArgs && !console && !table);
        setButtonVisible(consoleButton, hasFunction);
        setButtonVisible(clearButton, hasFunction && console);
        setButtonVisible(doneButton, hasFunction && table);
        setButtonVisible(exportButton, hasFunction && table);

        consoleButton.setText(console ? "Back" : "Console");

        actionButtonsHost.getChildren().clear();
        List<Button> visible = actionButtons.stream().filter(Button::isVisible).toList();
        for (Button button : visible) {
            HBox.setHgrow(button, Priority.ALWAYS);
            button.setMaxWidth(Double.MAX_VALUE);
            button.prefWidthProperty().unbind();
            actionButtonsHost.getChildren().add(button);
        }
    }

    private void setButtonVisible(Button button, boolean visible) {
        button.setVisible(visible);
        button.setManaged(visible);
    }

    private void runApply() {
        if (selectedFunction == null) {
            return;
        }
        try {
            selectedFunction.execute();
            app.showSuccess("Success");
        } catch (Exception e) {
            selectedFunction.addConsoleMessage("ERROR: " + e.getMessage());
            app.showError("Error");
        }
        updateToolbar();
    }

    private void runApplyAndReset() {
        if (selectedFunction == null) {
            return;
        }
        try {
            selectedFunction.execute();
            app.showSuccess("Success");
        } catch (Exception e) {
            selectedFunction.addConsoleMessage("ERROR: " + e.getMessage());
            app.showError("Error");
        } finally {
            selectedFunction.resetArguments();
        }
        updateToolbar();
    }

    private void runReset(){
        if (selectedFunction != null) {
            selectedFunction.resetArguments();
        }
    }

    private void runConsoleButton(){
        if (selectedFunction != null) {
            selectedFunction.toggleConsole();
            updateToolbar();
        }
    }

    private void runClearConsoleButton(){
        if (selectedFunction != null) {
            selectedFunction.clearConsole();
        }
    }

    private void runDoneButton(){
        if (selectedFunction != null) {
            selectedFunction.clearResult();
            updateToolbar();
        }
    }

    private void exportTable() {
        if (selectedFunction == null || !selectedFunction.isTableResult()) {
            return;
        }
        try {
            Path path = Path.of(System.getProperty("user.home"), "Downloads",
                    selectedFunction.getFunctionName() + "_export.csv");
            selectedFunction.exportTableToCsv(path);
            app.showSuccess("Exported");
        } catch (Exception e) {
            app.showError("Export failed");
        }
    }



    private void switchFunction(int offset) {
        if (functionButtonsBox.getChildren().isEmpty()) return;

        int currentIndex = functionButtonsBox.getChildren().indexOf(selectedListButton);
        if (currentIndex == -1) currentIndex = 0;

        int newIndex = (currentIndex + offset) % functionButtonsBox.getChildren().size();
        if (newIndex < 0) {
            newIndex += functionButtonsBox.getChildren().size();
        }

        if (functionButtonsBox.getChildren().get(newIndex) instanceof Button btn) {
            btn.fire();
        }
    }

    private void switchScreen(int offset) {
        List<ScreenConfig> screens = app.getConfig().getScreens();
        if (screens == null || screens.isEmpty()) return;

        int currentIndex = -1;
        for (int i = 0; i < screens.size(); i++) {
            if (screens.get(i).getPrefix().equals(this.screenConfig.getPrefix())) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex == -1) return;

        int newIndex = (currentIndex + offset) % screens.size();
        if (newIndex < 0) {
            newIndex += screens.size();
        }

        app.showFunctionScreen(screens.get(newIndex).getPrefix());
    }
}
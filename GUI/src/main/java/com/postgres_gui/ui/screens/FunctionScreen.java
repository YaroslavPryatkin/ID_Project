package com.postgres_gui.ui.screens;

import com.postgres_gui.config.ScreenConfig;
import com.postgres_gui.database.DatabaseFunctionInfo;
import com.postgres_gui.ui.AppWindow;
import com.postgres_gui.ui.components.Button;
import com.postgres_gui.ui.components.ButtonList;
import com.postgres_gui.ui.components.TableComponent;
import com.postgres_gui.ui.components.TextInputField;
import com.postgres_gui.ui.function.DatabaseFunction;
import com.postgres_gui.ui.panels.FunctionPanel;
import com.postgres_gui.util.ColorUtils;
import com.postgres_gui.util.TextMetrics;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FunctionScreen extends Screen {
    private static final double TOP_BAR_HEIGHT = 44;
    private static final double BOTTOM_BAR_HEIGHT = 40;
    public static final Font SCREEN_TITLE_FONT = new Font("Segoe UI Bold", 14);
    private static final double TOP_PADDING = 8;
    private static final double BUTTON_GAP = 10;
    private static final double SCREEN_TITLE_BOX_H = 28;
    private static final double SCREEN_TITLE_BOX_PAD_TOP = 8;

    private final ScreenConfig screenConfig;
    private final Map<String, DatabaseFunction> functions = new LinkedHashMap<>();
    private final FunctionPanel functionPanel;

    private ButtonList functionList;
    private ButtonList topNavigation;
    private Button backButton;

    private Color baseColor;
    private Color listBgColor;
    private Color listButtonColor;
    private Color topBarColor;
    private Color bottomBarColor;

    private double leftPanelWidth;
    private double width;
    private double height;
    private String selectedFunctionName;

    public FunctionScreen(AppWindow app, ScreenConfig screenConfig) {
        super(app);
        this.screenConfig = screenConfig;
        this.functionPanel = new FunctionPanel(app);
        this.baseColor = Color.web(screenConfig.getColor());
        recalculateColors();
        loadFunctions();
    }

    private void recalculateColors() {
        listBgColor = ColorUtils.lighten(baseColor, 0.25);
        listButtonColor = ColorUtils.lighten(listBgColor, 0.12);
        topBarColor = listButtonColor;
        bottomBarColor = ColorUtils.lighten(baseColor, 0.18);
    }

    private void loadFunctions() {
        functions.clear();
        try {
            List<DatabaseFunctionInfo> infos = app.getDatabaseManager()
                    .getFunctionsByPrefix(screenConfig.getPrefix());
            for (DatabaseFunctionInfo info : infos) {
                String display = screenConfig.getDisplayNameForFunction(info.getName());
                DatabaseFunction function = new DatabaseFunction(
                        info,
                        app.getDatabaseManager(),
                        display,
                        screenConfig.getArgAliasesForFunction(info.getName())
                );
                wireFunctionFields(function);
                functions.put(info.getName(), function);
            }
        } catch (Exception e) {
            app.showError("Failed to load functions");
        }
        if (!functions.isEmpty()) {
            selectFunction(functions.keySet().iterator().next());
        }
    }

    private void wireFunctionFields(DatabaseFunction function) {
        List<TextInputField> fields = function.getInputFields();
        for (TextInputField field : fields) {
            field.setOnEnter(f -> function.moveToNextArgument(() -> {
                try {
                    function.syncArgumentValuesFromFields();
                    function.execute();
                    function.resetArguments();
                    app.showSuccess("Success");
                    functionPanel.setSelectedFunction(function);
                } catch (Exception ex) {
                    function.addConsoleMessage("ERROR: " + ex.getMessage());
                    app.showError("Error");
                }
            }));
            field.setOnBackspaceOnEmpty(f -> function.moveToPreviousArgument());
        }
        if (!fields.isEmpty()) {
            function.focusArgument(0);
        }
    }

    @Override
    public void onResize(double width, double height) {
        this.width = width;
        this.height = height;
        leftPanelWidth = Math.max(180, screenConfig.getLeftPanelWidth() * 8.5);
        rebuildLists();
        updatePanelLayout();
    }

    private void rebuildLists() {
        double topY = TOP_BAR_HEIGHT;
        double leftH = height - topY;

        functionList = new ButtonList(0, topY, leftPanelWidth, leftH,
                listBgColor, ButtonList.ListOrientation.VERTICAL, ButtonList.VerticalButtonWidth.FILL_WIDTH_LEFT, true);

        java.util.Map<String, Button> functionButtons = new java.util.HashMap<>();
        for (DatabaseFunction function : functions.values()) {
            Button btn = new Button(function.getDisplayName(), listButtonColor, () ->
                    selectFunction(function.getFunctionName()), Color.BLACK);
            functionButtons.put(function.getFunctionName(), btn);
            functionList.addButton(btn);
        }
        if (selectedFunctionName != null) {
            Button btn = functionButtons.get(selectedFunctionName);
            if (btn != null) {
                functionList.selectButton(btn);
            }
        }

        backButton = new Button("Return to main menu", Color.web("#9E9E9E"), app::showStartScreen, true, true, Color.WHITE);
        backButton.setPosition(leftPanelWidth + TOP_PADDING, TOP_PADDING);
        double navX = backButton.getX() + backButton.getWidth() + BUTTON_GAP;

        topNavigation = new ButtonList(navX, 0, width - navX, TOP_BAR_HEIGHT,
                topBarColor, ButtonList.ListOrientation.HORIZONTAL, ButtonList.VerticalButtonWidth.FIT_CONTENT,false);

        for (ScreenConfig other : app.getConfig().getScreens()) {
            if (other.getPrefix().equals(screenConfig.getPrefix())) {
                continue;
            }
            Color color = Color.web(other.getColor());
            topNavigation.addButton(new Button(other.getDisplayName(), color,
                    () -> app.showFunctionScreen(other.getPrefix()), Color.WHITE));
        }
    }

    private void updatePanelLayout() {
        double contentX = leftPanelWidth;
        double contentY = TOP_BAR_HEIGHT;
        double contentW = width - leftPanelWidth;
        double contentH = height - TOP_BAR_HEIGHT;
        functionPanel.setLayout(contentX, contentY, contentW, contentH,
                height - BOTTOM_BAR_HEIGHT, BOTTOM_BAR_HEIGHT, bottomBarColor, listButtonColor);
    }

    private void selectFunction(String functionName) {
        selectedFunctionName = functionName;
        DatabaseFunction function = functions.get(functionName);
        functionPanel.setSelectedFunction(function);
        if (function != null) {
            function.focusArgument(0);
        }
    }

    @Override
    public void draw(GraphicsContext gc, double width, double height) {
        this.width = width;
        this.height = height;
        app.setNotificationReservedBottom(BOTTOM_BAR_HEIGHT + 10);
        app.setNotificationReservedRight(0);

        gc.setFill(baseColor);
        gc.fillRect(0, 0, width, height);

        gc.setFill(listBgColor);
        gc.fillRect(0, TOP_BAR_HEIGHT, leftPanelWidth, height - TOP_BAR_HEIGHT);

        gc.setFill(topBarColor);
        gc.fillRect(0, 0, width, TOP_BAR_HEIGHT);

        // Подложка под название экрана (внутри левого списка)
        Color screenColor = Color.web(screenConfig.getColor());
        Color boxColor = ColorUtils.darken(screenColor, 0.4);
        double boxX = 10;
        double boxY = SCREEN_TITLE_BOX_PAD_TOP;
        double boxW = leftPanelWidth - 20;
        double boxH = SCREEN_TITLE_BOX_H;

        gc.setFill(boxColor);
        gc.fillRoundRect(boxX, boxY, boxW, boxH, 8, 8);
        gc.setStroke(Color.web("#BDBDBD"));
        gc.strokeRoundRect(boxX, boxY, boxW, boxH, 8, 8);

        gc.setFill(Color.WHITE);
        gc.setFont(SCREEN_TITLE_FONT);
        double titleW = TextMetrics.width(SCREEN_TITLE_FONT, screenConfig.getDisplayName());
        gc.fillText(screenConfig.getDisplayName(), boxX + (boxW - titleW) / 2, boxY + boxH / 2 + 6);

        backButton.draw(gc);

        if (topNavigation != null) {
            topNavigation.draw(gc);
        }
        if (functionList != null) {
            functionList.draw(gc);
        }

        functionPanel.draw(gc);
    }

    @Override
    public void handleMousePressed(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();

        if (backButton.contains(x, y)) {
            backButton.click();
            return;
        }

        if (functionList != null && x < leftPanelWidth && y > functionList.getY()) {
            functionList.handleMouseClick(x, y);
            return;
        }

        if (topNavigation != null && y < TOP_BAR_HEIGHT && x > topNavigation.getX()) {
            topNavigation.handleMouseClick(x, y);
            return;
        }

        DatabaseFunction selected = functionPanel.getSelectedFunction();
        if (selected == null) {
            return;
        }

        if (functionPanel.isInBottomBar(x, y)) {
            functionPanel.handleMouseClick(x, y);
            return;
        }

        if (!functionPanel.isInContentArea(x, y)) {
            return;
        }

        if (selected.isShowingConsole()) {
            selected.handleConsoleClick(x, y);
            return;
        }

        if (selected.isTableResult() && selected.getTableComponent() != null) {
            selected.getTableComponent().handleMouseClick(x, y);
        }

        for (TextInputField field : selected.getInputFields()) {
            field.handleMouseClick(x, y, event.isShiftDown());
        }
    }

    @Override
    public void handleMouseMoved(MouseEvent event) {
        backButton.setHovered(backButton.contains(event.getX(), event.getY()));
        if (functionList != null) {
            functionList.handleMouseMove(event.getX(), event.getY());
        }
        if (topNavigation != null) {
            topNavigation.handleMouseMove(event.getX(), event.getY());
        }
        functionPanel.handleMouseMove(event.getX(), event.getY());

        DatabaseFunction selected = functionPanel.getSelectedFunction();
        if (selected != null && selected.isShowingConsole()) {
            selected.handleConsoleMouseMove(event.getX(), event.getY());
        }
    }

    @Override
    public void handleScroll(ScrollEvent event) {
        double x = event.getX();
        double y = event.getY();

        if (functionList != null && x < leftPanelWidth && y > functionList.getY()) {
            functionList.handleScroll(event.getDeltaY());
            return;
        }
        if (topNavigation != null && y < TOP_BAR_HEIGHT && x > leftPanelWidth) {
            topNavigation.handleScroll(event.getDeltaY());
            return;
        }

        DatabaseFunction selected = functionPanel.getSelectedFunction();
        if (selected != null) {
            if (selected.isShowingConsole()) {
                selected.handleConsoleScroll(event.getDeltaY());
                return;
            }

            if (selected.isTableResult() && selected.getTableComponent() != null) {
                if (event.isControlDown()) {
                    selected.getTableComponent().handleZoom(event.getDeltaY() > 0 ? 0.1 : -0.1);
                } else {
                    selected.getTableComponent().handleScroll(-event.getDeltaX(), -event.getDeltaY());
                }
            }
        }
    }

    @Override
    public void handleKeyPressed(KeyEvent event) {
        DatabaseFunction selected = functionPanel.getSelectedFunction();
        if (selected == null) {
            return;
        }

        if (selected.isTableResult() && selected.getTableComponent() != null) {
            TableComponent table = selected.getTableComponent();
            if (event.isControlDown() && event.getCode() == KeyCode.C) {
                table.copySelectionToClipboard();
                return;
            }
            switch (event.getCode()) {
                case UP, W -> table.handleKeyNavigation(TableComponent.KeyDirection.UP);
                case DOWN, S -> table.handleKeyNavigation(TableComponent.KeyDirection.DOWN);
                case LEFT, A -> table.handleKeyNavigation(TableComponent.KeyDirection.LEFT);
                case RIGHT, D -> table.handleKeyNavigation(TableComponent.KeyDirection.RIGHT);
                default -> {
                }
            }
            return;
        }

        boolean control = event.isControlDown();
        for (TextInputField field : selected.getInputFields()) {
            if (field.isFocused()) {
                field.handleKeyPress(event.getCode(), null, control);
                selected.syncArgumentValuesFromFields();
                return;
            }
        }
    }

    public void handleKeyTyped(KeyEvent event) {
        DatabaseFunction selected = functionPanel.getSelectedFunction();
        if (selected == null || (selected.isTableResult() && selected.getTableComponent() != null)) {
            return;
        }

        for (TextInputField field : selected.getInputFields()) {
            if (field.isFocused()) {
                field.handleKeyPress(KeyCode.UNDEFINED, event.getCharacter(), false);
                selected.syncArgumentValuesFromFields();
                return;
            }
        }
    }
}

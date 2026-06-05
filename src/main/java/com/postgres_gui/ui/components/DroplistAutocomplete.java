package com.postgres_gui.ui.components;

import com.postgres_gui.database.DatabaseManager;
import com.postgres_gui.ui.function.DatabaseFunction;
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class DroplistAutocomplete {
    private DroplistAutocomplete() {
    }

    private static final int MAX_VISIBLE_ITEMS = 15;

    public interface RefreshCallback {
        void refresh();
    }

    private static class DroplistState {
        SelectableListPanel listPanel;
        List<String> cachedValues;
        String currentFilter = "";
        List<String> filteredValues = new ArrayList<>();
        ParameterNameParser.DroplistSpec spec;
        DatabaseManager dbManager;
        TextField field;
        boolean isUpdatingFromSelection = false;

        DroplistState(TextField field, ParameterNameParser.DroplistSpec spec, DatabaseManager dbManager) {
            this.listPanel = new SelectableListPanel();
            this.cachedValues = new ArrayList<>();
            this.spec = spec;
            this.dbManager = dbManager;
            this.field = field;
        }
    }

    public static ScrollPane attach(TextField field, ParameterNameParser.DroplistSpec spec, DatabaseManager dbManager, List<RefreshCallback> callbackList) {
        if (spec == null || spec.kind() == ParameterNameParser.DroplistKind.NONE) {
            return null;
        }

        DroplistState state = new DroplistState(field, spec, dbManager);

        loadDroplistValues(state);

        field.textProperty().addListener((obs, oldText, newText) -> {
            if (state.isUpdatingFromSelection) {
                return;
            }
            if (!field.isFocused()) return;
            state.currentFilter = newText == null ? "" : newText;
            updateFilteredValues(state);
            showFilteredMenu(state);
        });

        field.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (focused) {
                state.currentFilter = field.getText();
                updateFilteredValues(state);
                showFilteredMenu(state);
            } else {
                Platform.runLater(state.listPanel::hide);
            }
        });

        field.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (state.listPanel.isShowing()) {
                if (state.listPanel.handleKeyEvent(e)) {
                    return;
                }
            }

            if (!state.listPanel.isShowing()) {
                if (e.getCode() == KeyCode.DOWN && field.isFocused()) {
                    state.currentFilter = field.getText();
                    updateFilteredValues(state);
                    showFilteredMenu(state);
                    e.consume();
                }
            }
        });

        state.listPanel.setOnSelection(selectedValue -> {
            state.isUpdatingFromSelection = true;
            try {
                field.setText(selectedValue);
                field.positionCaret(selectedValue.length());
                field.requestFocus();
            } finally {
                state.isUpdatingFromSelection = false;
                Platform.runLater(state.listPanel::hide);
            }
        });

        if (callbackList != null) {
            callbackList.add(() -> loadDroplistValues(state));
        }

        return state.listPanel.getScrollPane();
    }

    private static void loadDroplistValues(DroplistState state) {
        CompletableFuture.supplyAsync(() -> fetchAllValues(state.spec, state.dbManager))
                .thenAccept(values -> Platform.runLater(() -> {
                    state.cachedValues.clear();
                    state.cachedValues.addAll(values);

                    if (state.field.isFocused()) {
                        state.currentFilter = state.field.getText();
                        updateFilteredValues(state);
                        showFilteredMenu(state);
                    }
                }));
    }

    private static void updateFilteredValues(DroplistState state) {
        String lowerInput = state.currentFilter.toLowerCase();
        state.filteredValues = state.cachedValues.stream()
                .filter(v -> v.toLowerCase().startsWith(lowerInput))
                .limit(MAX_VISIBLE_ITEMS)
                .toList();
    }

    private static void showFilteredMenu(DroplistState state) {
        //(state.filteredValues.size()== 1 && state.filteredValues.getFirst().equals(state.currentFilter))
        if (!state.field.isFocused()) {
            state.listPanel.hide();
            return;
        }

        if (state.filteredValues.isEmpty()) {
            String emptyMessage = generateEmptyMessage(state.spec);
            state.listPanel.showEmptyMessage(emptyMessage);
            return;
        }

        state.listPanel.updateValues(new ArrayList<>(state.filteredValues));
        state.listPanel.show();
    }

    private static String generateEmptyMessage(ParameterNameParser.DroplistSpec spec) {
        return switch (spec.kind()) {
            case TABLE_FIELD -> "No matching records in " + spec.table() + "." + spec.field();
            case ENUM -> "No matching values in enum " + spec.enumTypeName();
            case NONE -> "No values available";
        };
    }

    private static List<String> fetchAllValues(ParameterNameParser.DroplistSpec spec, DatabaseManager dbManager) {
        try {
            return switch (spec.kind()) {
                case TABLE_FIELD -> dbManager.queryDistinctColumnValues(spec.table(), spec.field(), "");
                case ENUM -> dbManager.queryEnumLabels(spec.enumTypeName());
                case NONE -> List.of();
            };
        } catch (Exception e) {
            return List.of();
        }
    }
}
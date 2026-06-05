package com.postgres_gui.ui.layout;

import javafx.scene.text.Font;

/**
 * Статические параметры вёрстки и оформления UI.
 */
public final class UILayoutSettings {
    private UILayoutSettings() {
    }

    // --- Размеры окна ---
    public static final double WINDOW_START_WIDTH = 900;
    public static final double WINDOW_START_HEIGHT = 700;
    public static final double WINDOW_MIN_WIDTH = 800;
    public static final double WINDOW_MIN_HEIGHT = 650;

    // --- Общие цвета текста ---
    public static final String FONT_COLOR_PRIMARY = "#000000";
    public static final String FONT_COLOR_SECONDARY = "#555555";
    public static final String FONT_COLOR_ON_DARK = "#FFFFFF";
    public static final String FONT_COLOR_INPUT = "#000000";

    // --- Контуры ---
    public static final double PANEL_BORDER_WIDTH = 1.0;
    public static final double BUTTON_BORDER_WIDTH = 1.0;
    public static final String PANEL_BORDER_COLOR = "#333333";
    public static final String BUTTON_BORDER_COLOR = "#333333";

    // --- Стартовый экран ---
    public static final double START_SCREEN_FORM_WIDTH = 320;
    public static final double START_SCREEN_TITLE_Y_FRACTION = 0.18;
    public static final double START_SCREEN_TITLE_TO_SUBTITLE_SPACING = 4;
    public static final double START_SCREEN_SUBTITLE_TO_FORM_BLOCK_SPACING = 24;
    public static final double START_SCREEN_LIST_BUTTON_SPACING = 10;
    public static final double START_SCREEN_LIST_BOTTOM_PADDING = 36;
    public static final double START_SCREEN_LIST_BUTTON_CORNER_RADIUS = 18;
    public static final String START_SCREEN_TITLE_TEXT_COLOR = FONT_COLOR_PRIMARY;
    public static final String START_SCREEN_SUBTITLE_TEXT_COLOR = FONT_COLOR_SECONDARY;

    // --- Экран функций: список функций ---
    public static final double FUNCTION_LIST_VERTICAL_PADDING = 8;
    public static final double FUNCTION_LIST_HORIZONTAL_PADDING = 6;
    public static final double FUNCTION_LIST_BUTTON_SPACING = 10;
    public static final double FUNCTION_LIST_BUTTON_CORNER_RADIUS = 18;
    public static final double FUNCTION_LIST_PANEL_MIN_WIDTH = 200;
    public static final double FUNCTION_LIST_PANEL_WIDTH_FACTOR = 8.5;

    // --- Экран функций: факторы цвета от базового (ColorUtils) ---
    public static final double FUNCTION_COLOR_LIST_PANEL_LIGHTEN = 0.25;
    public static final double FUNCTION_COLOR_LIST_BUTTON_LIGHTEN = 0.12;
    public static final double FUNCTION_COLOR_LIST_BUTTON_HOVER_DARKEN = 0.12;
    public static final double FUNCTION_COLOR_LIST_BUTTON_SELECTED_DARKEN = 0.18;
    public static final double FUNCTION_COLOR_TOP_BAR_LIGHTEN = 0.37;
    public static final double FUNCTION_COLOR_BOTTOM_BAR_LIGHTEN = 0.25;
    public static final double FUNCTION_COLOR_TOOLBAR_BUTTON_LIGHTEN = 0.12;
    public static final double FUNCTION_COLOR_TOOLBAR_BUTTON_HOVER_DARKEN = 0.10;
    public static final double FUNCTION_COLOR_SCREEN_TITLE_DARKEN = 0.40;
    public static final double FUNCTION_COLOR_PANEL_BORDER_DARKEN = 0.35;
    public static final double FUNCTION_COLOR_NAV_BUTTON_HOVER_DARKEN = 0.10;

    // --- Кнопки (общие размеры) ---
    public static final double BAR_BUTTON_MIN_HEIGHT = 30;
    public static final double BAR_BUTTON_PADDING_VERTICAL = 6;
    public static final double BAR_BUTTON_PADDING_HORIZONTAL = 12;
    public static final double LIST_BUTTON_MIN_HEIGHT = 32;
    public static final double LIST_BUTTON_PADDING_VERTICAL = 8;
    public static final double LIST_BUTTON_PADDING_HORIZONTAL = 12;

    // --- Верхний бар ---
    public static final double NAV_BUTTON_CORNER_RADIUS = 18;
    public static final double TOP_BAR_HEIGHT = 40;
    public static final double TOP_BAR_HORIZONTAL_PADDING = 10;
    public static final double TOP_BAR_BUTTON_GAP = 10;
    public static final String NAV_BUTTON_TEXT_COLOR = FONT_COLOR_ON_DARK;

    // --- Нижний тулбар ---
    public static final double BOTTOM_TOOLBAR_HEIGHT = 40;
    public static final double BOTTOM_TOOLBAR_CONSOLE_WIDTH_FRACTION = 0.25;
    public static final double BOTTOM_TOOLBAR_BUTTON_CORNER_RADIUS = 14;
    public static final double BOTTOM_TOOLBAR_BUTTON_GAP = 8;
    public static final double BOTTOM_TOOLBAR_CONSOLE_GAP = 10;
    public static final double BOTTOM_TOOLBAR_INNER_PADDING = 4;
    public static final double BOTTOM_TOOLBAR_RESERVED_FOR_NOTIFICATIONS = 50;
    public static final String TOOLBAR_BUTTON_TEXT_COLOR = FONT_COLOR_PRIMARY;

    // --- Поля ввода ---
    public static final double INPUT_FIELD_HEIGHT = 32;
    public static final double INPUT_LABEL_PLATE_HEIGHT = 22;
    public static final double INPUT_FIELD_CORNER_RADIUS = 0;
    public static final double INPUT_LABEL_PLATE_CORNER_RADIUS = 0;
    public static final double INPUT_BLOCK_VERTICAL_SPACING = 14;
    public static final String INPUT_LABEL_TEXT_COLOR = FONT_COLOR_PRIMARY;
    public static final String INPUT_FIELD_TEXT_COLOR = FONT_COLOR_INPUT;
    public static final String INPUT_LABEL_PLATE_BACKGROUND = "#E0E0E0";
    public static final String INPUT_FIELD_BACKGROUND = "#FFFFFF";
    public static final String INPUT_FIELD_BORDER_COLOR = "#BDBDBD";
    public static final String INPUT_FIELD_ERROR_BORDER_COLOR = "#D32F2F";
    public static final double DROPLIST_MAX_HEIGHT = 300;

    // --- Пароль: переключатель видимости ---
    public static final String PASSWORD_SHOW_CHECKBOX_LABEL = "Show password";
    public static final double PASSWORD_SHOW_CHECKBOX_TOP_PADDING = 4;
    public static final String CHECKBOX_TEXT_COLOR = FONT_COLOR_SECONDARY;

    // --- Плашка ошибки (стартовый экран) ---
    public static final double ERROR_MESSAGE_PLATE_CORNER_RADIUS = 8;
    public static final String ERROR_MESSAGE_PLATE_BACKGROUND = "#F5E0E0";
    public static final String ERROR_MESSAGE_TEXT_COLOR = "#D32F2F";

    //Notification system
    public static final double NOTIFICATION_MAX_WIDTH = 200;


    // --- Фоновая иконка ---
    public static final double BACKGROUND_ICON_OPACITY = 0.30;
    public static final double BACKGROUND_ICON_SIZE_FACTOR = 0.90;

    // --- Центральная область функции ---
    public static final String FUNCTION_CENTER_BACKGROUND = "#FAFAFA";
    public static final String ZERO_ARGUMENT_MESSAGE = "Function is zero-argument";
    public static final String ZERO_ARGUMENT_PLATE_BACKGROUND = "#E8E8E8";
    public static final double ZERO_ARGUMENT_PLATE_CORNER_RADIUS = 8;
    public static final double ZERO_ARGUMENT_PLATE_PADDING = 10;
    public static final Font ZERO_ARGUMENT_FONT = Font.font("Segoe UI", 12);
    public static final String ZERO_ARGUMENT_TEXT_COLOR = FONT_COLOR_SECONDARY;

    // --- Шрифты ---
    public static final Font NAV_SCREEN_BUTTON_FONT = Font.font("Segoe UI", 13);
    public static final Font FUNCTION_LIST_BUTTON_FONT = Font.font("Segoe UI", 13);
    public static final Font BOTTOM_TOOLBAR_BUTTON_FONT = Font.font("Segoe UI", 13);
    public static final Font INPUT_LABEL_FONT = Font.font("Segoe UI", 12);
    public static final Font INPUT_FIELD_FONT = Font.font("Segoe UI", 12);
    public static final Font START_SCREEN_TITLE_FONT = Font.font("Segoe UI Bold", 22);
    public static final Font START_SCREEN_SUBTITLE_FONT = Font.font("Segoe UI", 12);
    public static final Font ERROR_MESSAGE_FONT = Font.font("Segoe UI", 12);
    public static final Font CHECKBOX_FONT = Font.font("Segoe UI", 11);
    public static final Font CONSOLE_TIMESTAMP_FONT = Font.font("Consolas", 11);
    public static final Font CONSOLE_MESSAGE_FONT = Font.font("Consolas", 11);
    public static final String FUNCTION_LIST_BUTTON_TEXT_COLOR = FONT_COLOR_PRIMARY;
    public static final String CONSOLE_TIMESTAMP_COLOR = "#8FA8C8";
    public static final String CONSOLE_MESSAGE_COLOR = "#EEEEEE";
    public static final String CONSOLE_BACKGROUND = "#111111";



    // --- Префиксы droplist ---
    public static final String DROPLIST_TABLE_PREFIX = "droplisttable_";
    public static final String DROPLIST_FIELD_PREFIX = "droplistfield_";
    public static final String DROPLIST_ENUM_PREFIX = "droplistenum_";
}

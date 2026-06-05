package com.postgres_gui.ui.layout;

import com.postgres_gui.util.ColorUtils;
import javafx.scene.paint.Color;

/**
 * Производные цвета экрана функций от базового цвета экрана (из конфига).
 */
public record FunctionScreenPalette(
        Color base,
        Color listPanelBackground,
        Color listButton,
        Color listButtonHover,
        Color listButtonSelected,
        Color topBarBackground,
        Color bottomBarBackground,
        Color toolbarButton,
        Color toolbarButtonHover,
        Color screenTitlePlate,
        Color panelBorder
) {
    public static FunctionScreenPalette fromBase(Color baseColor) {
        Color listPanel = ColorUtils.lighten(baseColor, UILayoutSettings.FUNCTION_COLOR_LIST_PANEL_LIGHTEN);
        Color listBtn = ColorUtils.lighten(listPanel, UILayoutSettings.FUNCTION_COLOR_LIST_BUTTON_LIGHTEN);
        Color bottomBar = ColorUtils.lighten(baseColor, UILayoutSettings.FUNCTION_COLOR_BOTTOM_BAR_LIGHTEN);
        Color toolbarBtn = ColorUtils.lighten(listPanel, UILayoutSettings.FUNCTION_COLOR_TOOLBAR_BUTTON_LIGHTEN);
        return new FunctionScreenPalette(
                baseColor,
                listPanel,
                listBtn,
                ColorUtils.darken(listBtn, UILayoutSettings.FUNCTION_COLOR_LIST_BUTTON_HOVER_DARKEN),
                ColorUtils.darken(listBtn, UILayoutSettings.FUNCTION_COLOR_LIST_BUTTON_SELECTED_DARKEN),
                ColorUtils.lighten(baseColor, UILayoutSettings.FUNCTION_COLOR_TOP_BAR_LIGHTEN),
                bottomBar,
                toolbarBtn,
                ColorUtils.darken(toolbarBtn, UILayoutSettings.FUNCTION_COLOR_TOOLBAR_BUTTON_HOVER_DARKEN),
                ColorUtils.darken(baseColor, UILayoutSettings.FUNCTION_COLOR_SCREEN_TITLE_DARKEN),
                ColorUtils.darken(baseColor, UILayoutSettings.FUNCTION_COLOR_PANEL_BORDER_DARKEN)
        );
    }

    public String baseHex() {
        return toHex(base);
    }

    public String listPanelHex() {
        return toHex(listPanelBackground);
    }

    public String listButtonHex() {
        return toHex(listButton);
    }

    public String listButtonHoverHex() {
        return toHex(listButtonHover);
    }

    public String listButtonSelectedHex() {
        return toHex(listButtonSelected);
    }

    public String topBarHex() {
        return toHex(topBarBackground);
    }

    public String bottomBarHex() {
        return toHex(bottomBarBackground);
    }

    public String toolbarButtonHex() {
        return toHex(toolbarButton);
    }

    public String toolbarButtonHoverHex() {
        return toHex(toolbarButtonHover);
    }

    public String screenTitleHex() {
        return toHex(screenTitlePlate);
    }

    public String panelBorderHex() {
        return toHex(panelBorder);
    }

    public static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}

package com.postgres_gui.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScreenConfig {
    private final String prefix;
    private final String color;
    private final String displayName;
    private final Map<String, String> functionNameMappings = new HashMap<>();
    private final Map<String, List<String>> functionArgAliases = new HashMap<>();
    private static final int leftPanelWidth = 30;
    public final static String backgroundColor = "#F8F9FA";

    public ScreenConfig(String prefix, String color, String displayName) {
        this.prefix = prefix;
        this.color = color;
        this.displayName = displayName;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void addFunctionMapping(String sqlName, String guiName) {
        functionNameMappings.put(sqlName, guiName);
    }

    public void addFunctionMapping(String sqlName, String guiName, List<String> argAliases) {
        functionNameMappings.put(sqlName, guiName);
        if (argAliases != null && !argAliases.isEmpty()) {
            functionArgAliases.put(sqlName, argAliases);
        }
    }

    public String getDisplayNameForFunction(String sqlFunctionName) {
        String withoutScreenPrefix = sqlFunctionName;
        if (withoutScreenPrefix.startsWith(prefix + "_")) {
            withoutScreenPrefix = withoutScreenPrefix.substring(prefix.length() + 1);
        }

        String mapped = functionNameMappings.get(withoutScreenPrefix);
        if (mapped != null) {
            return mapped;
        }
        mapped = functionNameMappings.get("noout_" + withoutScreenPrefix);
        if (mapped != null) {
            return mapped;
        }

        if (withoutScreenPrefix.startsWith("noout_")) {
            withoutScreenPrefix = withoutScreenPrefix.substring("noout_".length());
        }
        return withoutScreenPrefix;
    }

    public List<String> getArgAliasesForFunction(String sqlFunctionName) {
        String withoutScreenPrefix = sqlFunctionName;
        if (withoutScreenPrefix.startsWith(prefix + "_")) {
            withoutScreenPrefix = withoutScreenPrefix.substring(prefix.length() + 1);
        }

        List<String> mapped = functionArgAliases.get(withoutScreenPrefix);
        if (mapped != null) {
            return mapped;
        }
        mapped = functionArgAliases.get("noout_" + withoutScreenPrefix);
        if (mapped != null) {
            return mapped;
        }
        return List.of();
    }

    public int getLeftPanelWidth() {
        return leftPanelWidth;
    }

    public Map<String, String> getFunctionNameMappings() {
        return functionNameMappings;
    }
}

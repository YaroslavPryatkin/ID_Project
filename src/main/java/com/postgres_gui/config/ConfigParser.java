package com.postgres_gui.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ConfigParser {

    public Config parse(String filename) throws IOException {
        Config config = new Config();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                parseLine(line, config, lineNum);
            }
        }

        return config;
    }

    private void parseLine(String line, Config config, int lineNum) {
        if (line.startsWith("url:")) {
            parseUrl(line.substring(4).trim(), config);
        } else if (line.startsWith("database:")) {
            config.setDatabase(line.substring(9).trim());
        } else if (line.startsWith("user:")) {
            config.setUser(line.substring(5).trim());
        } else if (line.startsWith("password:")) {
            config.setPassword(line.substring(9).trim());
        } else if (line.startsWith("screen:")) {
            parseScreen(line.substring(7).trim(), config);
        } else if (line.startsWith("fun:")) {
            parseFunctionMapping(line.substring(4).trim(), config);
        } else if (!line.isEmpty()) {
            System.err.println("Warning: Unknown config line " + lineNum + ": " + line);
        }
    }

    private void parseUrl(String value, Config config) {
        int slash = value.indexOf('/');
        if (slash >= 0) {
            config.setUrl(value.substring(0, slash));
            config.setDatabase(value.substring(slash + 1));
        } else {
            config.setUrl(value);
        }
    }

    private void parseScreen(String content, Config config) {
        String[] parts = content.split("\\s+", 3);

        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid screen format. Expected: prefix color [display name...]");
        }

        String prefix = parts[0];
        String color = parts[1];
        String displayName = parts.length > 2 ? parts[2] : prefix;

        ScreenConfig screenConfig = new ScreenConfig(prefix, color, displayName);
        config.addScreen(screenConfig);
    }

    private void parseFunctionMapping(String content, Config config) {
        if (content.isBlank() || config.getScreens().isEmpty()) {
            throw new IllegalArgumentException("Invalid function mapping format. Expected: sql_name [gui_name...]");
        }
        String trimmed = content.trim();

        String sqlName;
        String rest;
        int firstSpace = trimmed.indexOf(' ');
        if (firstSpace < 0) {
            sqlName = trimmed;
            rest = "";
        } else {
            sqlName = trimmed.substring(0, firstSpace).trim();
            rest = trimmed.substring(firstSpace + 1).trim();
        }

        String guiName = rest.isBlank() ? sqlName : rest;
        java.util.List<String> aliases = java.util.List.of();

        // Опционально: "... (alias1, alias2, ...)" в конце строки
        int open = rest.lastIndexOf('(');
        int close = rest.endsWith(")") ? rest.length() - 1 : -1;
        if (open >= 0 && close > open) {
            String before = rest.substring(0, open).trim();
            String inside = rest.substring(open + 1, close).trim();
            guiName = before.isBlank() ? sqlName : before;
            if (!inside.isBlank()) {
                java.util.List<String> list = new java.util.ArrayList<>();
                for (String part : inside.split(",")) {
                    String a = part.trim();
                    if (!a.isEmpty()) {
                        list.add(a);
                    }
                }
                aliases = java.util.List.copyOf(list);
            }
        }

        ScreenConfig lastScreen = config.getScreens().get(config.getScreens().size() - 1);
        lastScreen.addFunctionMapping(sqlName, guiName, aliases);
    }
}

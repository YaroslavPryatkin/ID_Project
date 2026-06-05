package com.postgres_gui.database;

public class DatabaseFunctionInfo {
    private final String name;
    private final boolean returnsSet;
    private final int argumentCount;

    public DatabaseFunctionInfo(String name, boolean returnsSet, int argumentCount) {
        this.name = name;
        this.returnsSet = returnsSet;
        this.argumentCount = argumentCount;
    }

    public String getName() {
        return name;
    }

    public boolean returnsSet() {
        return returnsSet;
    }

    public int getArgumentCount() {
        return argumentCount;
    }

    public boolean hasNoOutPrefix() {
        return name.contains("noout_");
    }

    public String getSqlNameWithoutPrefix(String screenPrefix) {
        String clean = name;
        if (clean.startsWith(screenPrefix + "_")) {
            clean = clean.substring(screenPrefix.length() + 1);
        }
        if (clean.startsWith("noout_")) {
            clean = clean.substring("noout_".length());
        }
        return clean;
    }
}

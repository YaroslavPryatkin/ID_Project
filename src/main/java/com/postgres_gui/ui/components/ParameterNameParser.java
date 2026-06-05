package com.postgres_gui.ui.components;

import com.postgres_gui.ui.layout.UILayoutSettings;

import java.util.ArrayList;
import java.util.List;

public final class ParameterNameParser {
    private ParameterNameParser() {
    }

    public enum DroplistKind {
        NONE, TABLE_FIELD, ENUM
    }

    public record DroplistSpec(DroplistKind kind, String table, String field, String enumTypeName) {
    }

    public record ParseResult(String displayAlias, DroplistSpec spec, String errorMessage) {
    }

    public static ParseResult parseAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return new ParseResult(null, new DroplistSpec(DroplistKind.NONE, null, null, null), null);
        }

        String[] words = alias.split("\\s+");
        List<String> cleanWords = new ArrayList<>();

        String table = null;
        String field = null;
        String enumType = null;

        int tableCount = 0;
        int fieldCount = 0;
        int enumCount = 0;

        for (String word : words) {
            if (word.startsWith(UILayoutSettings.DROPLIST_TABLE_PREFIX)) {
                table = word.substring(UILayoutSettings.DROPLIST_TABLE_PREFIX.length());
                tableCount++;
            } else if (word.startsWith(UILayoutSettings.DROPLIST_FIELD_PREFIX)) {
                field = word.substring(UILayoutSettings.DROPLIST_FIELD_PREFIX.length());
                fieldCount++;
            } else if (word.startsWith(UILayoutSettings.DROPLIST_ENUM_PREFIX)) {
                enumType = word.substring(UILayoutSettings.DROPLIST_ENUM_PREFIX.length());
                enumCount++;
            } else {
                // Все остальные слова сохраняются В ТОЧНОСТИ в том регистре, в котором они были в конфиге
                cleanWords.add(word);
            }
        }

        String displayAlias = cleanWords.isEmpty() ? null : String.join(" ", cleanWords);

        if (tableCount == 0 && fieldCount == 0 && enumCount == 0) {
            return new ParseResult(displayAlias, new DroplistSpec(DroplistKind.NONE, null, null, null), null);
        }

        if (enumCount == 1 && tableCount == 0 && fieldCount == 0) {
            return new ParseResult(displayAlias, new DroplistSpec(DroplistKind.ENUM, null, null, enumType), null);
        }

        if (enumCount == 0 && tableCount == 1 && fieldCount == 1) {
            return new ParseResult(displayAlias, new DroplistSpec(DroplistKind.TABLE_FIELD, table, field, null), null);
        }

        String errorMsg = "Wrong drop list config for '" + displayAlias + "'. " +
                "Alias should contain either droplistenum_ or droplisttable_ + droplistfield_ .";
        return new ParseResult(displayAlias, new DroplistSpec(DroplistKind.NONE, null, null, null), errorMsg);
    }
}



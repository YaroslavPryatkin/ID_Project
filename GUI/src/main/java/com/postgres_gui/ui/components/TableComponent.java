package com.postgres_gui.ui.components;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import com.postgres_gui.util.TextMetrics;

import java.util.*;

public class TableComponent {
    private final List<Map<String, Object>> data;
    private final List<String> columns;

    private double x;
    private double y;
    private double width;
    private double height;
    private double scrollX;
    private double scrollY;
    private double zoom = 1.0;

    private final double rowHeight = 25;
    private final double columnPadding = 10;
    private double[] columnWidths;
    private int selectedRow = -1;

    private static final Font HEADER_FONT = new Font("Segoe UI Bold", 11);
    private static final Font CELL_FONT = new Font("Segoe UI", 10);

    public TableComponent(double x, double y, double width, double height, List<Map<String, Object>> data) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.data = data == null ? List.of() : data;
        if (!this.data.isEmpty()) {
            this.columns = new ArrayList<>(this.data.get(0).keySet());
        } else {
            this.columns = new ArrayList<>();
        }
        calculateColumnWidths();
    }

    private void calculateColumnWidths() {
        columnWidths = new double[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            double max = measure(HEADER_FONT, columns.get(i)) + columnPadding * 2;
            for (Map<String, Object> row : data) {
                Object value = row.get(columns.get(i));
                String str = value == null ? "" : value.toString();
                max = Math.max(max, measure(CELL_FONT, str) + columnPadding * 2);
            }
            // Не делаем колонки длиннее чем нужно
            columnWidths[i] = Math.max(10, max);
        }
    }

    private double measure(Font font, String text) {
        return TextMetrics.width(font, text == null ? "" : text);
    }

    public void draw(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.fillRect(x, y, width, height);
        gc.setStroke(Color.web("#CCCCCC"));
        gc.strokeRect(x, y, width, height);

        gc.save();
        gc.beginPath();
        gc.rect(x, y, width, height);
        gc.clip();

        drawHeaders(gc);
        drawRows(gc);
        gc.restore();
    }

    private void drawHeaders(GraphicsContext gc) {
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(Color.web("#E8E8E8"));
        gc.fillRect(x, y, width, rowHeight * zoom);

        double currentX = x - scrollX;
        for (int i = 0; i < columns.size(); i++) {
            double colWidth = columnWidths[i] * zoom;
            gc.setStroke(Color.web("#CCCCCC"));
            gc.strokeLine(currentX + colWidth, y, currentX + colWidth, y + rowHeight * zoom);
            gc.setFill(Color.BLACK);
            gc.setFont(HEADER_FONT);
            gc.fillText(columns.get(i), currentX + columnPadding, y + rowHeight * zoom / 2 + 4);
            currentX += colWidth;
        }
    }

    private void drawRows(GraphicsContext gc) {
        gc.setTextAlign(TextAlignment.LEFT);
        double currentY = y + rowHeight * zoom - scrollY;
        for (int rowIdx = 0; rowIdx < data.size(); rowIdx++) {
            if (currentY > y + height) {
                break;
            }
            if (currentY + rowHeight * zoom < y + rowHeight * zoom) {
                currentY += rowHeight * zoom;
                continue;
            }

            Color bg = rowIdx == selectedRow
                    ? Color.web("#D6EAF8")
                    : (rowIdx % 2 == 0 ? Color.WHITE : Color.web("#F7F7F7"));
            gc.setFill(bg);
            gc.fillRect(x, currentY, width, rowHeight * zoom);

            double currentX = x - scrollX;
            Map<String, Object> row = data.get(rowIdx);
            for (int colIdx = 0; colIdx < columns.size(); colIdx++) {
                double colWidth = columnWidths[colIdx] * zoom;
                Object value = row.get(columns.get(colIdx));
                String text = value == null ? "null" : value.toString();
                gc.setStroke(Color.web("#DDDDDD"));
                gc.strokeLine(currentX + colWidth, currentY, currentX + colWidth, currentY + rowHeight * zoom);
                gc.setFill(Color.BLACK);
                gc.setFont(CELL_FONT);
                gc.fillText(text, currentX + columnPadding, currentY + rowHeight * zoom / 2 + 4);
                currentX += colWidth;
            }
            currentY += rowHeight * zoom;
        }
    }

    public void handleScroll(double deltaX, double deltaY) {
        scrollX += deltaX;
        scrollY += deltaY;
        clampScroll();
    }

    public void handleZoom(double delta) {
        zoom = Math.max(0.5, Math.min(2.5, zoom + delta));
        calculateColumnWidths();
        clampScroll();
    }

    private void clampScroll() {
        double totalWidth = Arrays.stream(columnWidths).sum() * zoom;
        double totalHeight = data.size() * rowHeight * zoom;
        scrollX = Math.max(0, Math.min(scrollX, Math.max(0, totalWidth - width + 20)));
        scrollY = Math.max(0, Math.min(scrollY, Math.max(0, totalHeight - height + rowHeight * zoom)));
    }

    public void handleKeyNavigation(KeyDirection direction) {
        switch (direction) {
            case UP -> scrollY = Math.max(0, scrollY - rowHeight * zoom * 2);
            case DOWN -> scrollY += rowHeight * zoom * 2;
            case LEFT -> scrollX = Math.max(0, scrollX - 40);
            case RIGHT -> scrollX += 40;
        }
        clampScroll();
    }

    public void handleMouseClick(double px, double py) {
        if (px < x || px > x + width || py < y + rowHeight * zoom || py > y + height) {
            return;
        }
        int row = (int) ((py - y - rowHeight * zoom + scrollY) / (rowHeight * zoom));
        if (row >= 0 && row < data.size()) {
            selectedRow = row;
        }
    }

    public void copySelectionToClipboard() {
        if (selectedRow < 0 || selectedRow >= data.size()) {
            return;
        }
        Map<String, Object> row = data.get(selectedRow);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String column : columns) {
            if (!first) {
                sb.append(' ');
            }
            Object value = row.get(column);
            sb.append(value == null ? "" : value);
            first = false;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(content);
    }

    public void exportToCsv(java.nio.file.Path path) throws java.io.IOException {

        try (var fos = new java.io.FileOutputStream(path.toFile());
             var writer = new java.io.OutputStreamWriter(fos, java.nio.charset.StandardCharsets.UTF_8)) {


            writer.write('\ufeff');

            String separator = ";";

            writer.write(String.join(separator, columns));
            writer.write("\r\n");

            for (Map<String, Object> row : data) {
                boolean first = true;
                for (String column : columns) {
                    if (!first) {
                        writer.write(separator);
                    }
                    Object value = row.get(column);
                    writer.write(escapeCsv(value == null ? "" : value.toString(), separator));
                    first = false;
                }
                writer.write("\r\n");
            }
        }
    }
    private String escapeCsv(String value, String separator) {
        if (value.contains(separator) || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public void setBounds(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        calculateColumnWidths();
        clampScroll();
    }

    public enum KeyDirection {
        UP, DOWN, LEFT, RIGHT
    }
}

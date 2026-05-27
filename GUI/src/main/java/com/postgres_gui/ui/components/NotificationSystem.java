package com.postgres_gui.ui.components;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationSystem {
    private static class Notification {
        final String message;
        final NotificationType type;
        final long createdAt;
        long displayTime = 3000;
        double opacity = 1.0;

        Notification(String message, NotificationType type, long displayTime) {
            this.message = message;
            this.type = type;
            this.createdAt = System.currentTimeMillis();
            this.displayTime = displayTime;
        }

        void updateOpacity() {
            long elapsed = System.currentTimeMillis() - createdAt;
            if (elapsed > displayTime) {
                opacity = Math.max(0, 1.0 - ((double) (elapsed - displayTime) / 500));
            }
        }

        boolean isExpired() {
            return opacity <= 0;
        }
    }

    public enum NotificationType {
        SUCCESS, ERROR
    }

    private final List<Notification> notifications = new ArrayList<>();
    private double screenWidth;
    private double screenHeight;
    private double reservedBottom = 0;
    private double reservedRight = 0;

    private static final Font NOTIFICATION_FONT = new Font("Segoe UI", 12);
    private static final double NOTIFICATION_HEIGHT = 40;
    private static final double NOTIFICATION_PADDING = 12;

    public NotificationSystem(double screenWidth, double screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void showSuccess(String message) {
        showNotification(message, NotificationType.SUCCESS);
    }

    public void showError(String message) {
        showNotification(message, NotificationType.ERROR);
    }

    public void showNotification(String message, NotificationType type) {
        long displayTime = notifications.size() >= 4 ? 1500 : 3000;
        notifications.add(new Notification(message, type, displayTime));
        if (notifications.size() > 8) {
            notifications.remove(0);
        }
    }

    public void draw(GraphicsContext gc) {
        gc.save();
        Iterator<Notification> iterator = notifications.iterator();
        while (iterator.hasNext()) {
            Notification notif = iterator.next();
            notif.updateOpacity();
            if (notif.isExpired()) {
                iterator.remove();
            }
        }

        double currentY = screenHeight - reservedBottom - NOTIFICATION_PADDING - NOTIFICATION_HEIGHT;

        for (Notification notif : notifications) {
            Color bg = notif.type == NotificationType.SUCCESS
                    ? Color.web("#6B8F6B", 0.85 * notif.opacity)
                    : Color.web("#9B6B6B", 0.85 * notif.opacity);

            gc.setGlobalAlpha(0.92 * notif.opacity);
            gc.setFill(bg);
            double notifWidth = textWidth(notif.message) + NOTIFICATION_PADDING * 2;
            double notifX = screenWidth - reservedRight - notifWidth - NOTIFICATION_PADDING;
            gc.fillRoundRect(notifX, currentY, notifWidth, NOTIFICATION_HEIGHT, 5, 5);
            gc.setFont(NOTIFICATION_FONT);
            gc.setFill(Color.web("#F3F3F3"));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(notif.message, notifX + notifWidth / 2, currentY + NOTIFICATION_HEIGHT / 2 + 5);
            gc.setGlobalAlpha(1.0);

            currentY -= NOTIFICATION_HEIGHT + NOTIFICATION_PADDING;
        }
        gc.restore();
    }

    public void setScreenSize(double width, double height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public void setReservedBottom(double reservedBottom) {
        this.reservedBottom = Math.max(0, reservedBottom);
    }

    public void setReservedRight(double reservedRight) {
        this.reservedRight = Math.max(0, reservedRight);
    }

    private double textWidth(String text) {
        return text.length() * NOTIFICATION_FONT.getSize() * 0.6;
    }
}

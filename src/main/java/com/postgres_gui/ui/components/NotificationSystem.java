package com.postgres_gui.ui.components;

import com.postgres_gui.ui.layout.UILayoutSettings;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationSystem {
    private static class Notification {
        final Label label;
        final long createdAt;
        final long displayTime;
        double opacity = 1.0;

        Notification(String message, NotificationType type, long displayTime) {
            this.displayTime = displayTime;
            this.createdAt = System.currentTimeMillis();
            this.label = new Label(message);
            this.label.setPadding(new Insets(10, 14, 10, 14));

            this.label.setMaxWidth(UILayoutSettings.NOTIFICATION_MAX_WIDTH);
            this.label.setWrapText(true);

            String bg = type == NotificationType.SUCCESS ? "#6B8F6B" : "#9B6B6B";
            label.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: #F3F3F3; -fx-background-radius: 5;");
        }

        void updateOpacity() {
            long elapsed = System.currentTimeMillis() - createdAt;
            if (elapsed > displayTime) {
                opacity = Math.max(0, 1.0 - ((double) (elapsed - displayTime) / 500));
            }
            label.setOpacity(opacity);
        }

        boolean isExpired() {
            return opacity <= 0;
        }
    }

    public enum NotificationType {
        SUCCESS, ERROR
    }

    private final VBox container = new VBox(8);
    private final List<Notification> notifications = new ArrayList<>();
    private double reservedBottom = 0;
    private double reservedRight = 0;
    private final Timeline ticker;

    public NotificationSystem() {
        container.setAlignment(Pos.BOTTOM_RIGHT);
        container.setPickOnBounds(false);
        container.setMouseTransparent(true);

        container.setFillWidth(false);

        ticker = new Timeline(new KeyFrame(Duration.millis(50), e -> tick()));
        ticker.setCycleCount(Timeline.INDEFINITE);
        ticker.play();
    }

    public Pane getView() {
        Pane overlay = new Pane();
        overlay.setMouseTransparent(true);
        overlay.setPickOnBounds(false);
        overlay.getChildren().add(container);
        overlay.widthProperty().addListener((obs, o, n) -> reposition(overlay));
        overlay.heightProperty().addListener((obs, o, n) -> reposition(overlay));
        return overlay;
    }

    private void reposition(Pane parent) {
        double w = parent.getWidth();
        double h = parent.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        container.setLayoutX(12);
        container.setLayoutY(12);
        container.setPrefWidth(Math.max(0, w - reservedRight - 24));
        container.setMaxWidth(Math.max(0, w - reservedRight - 24));
        container.setLayoutY(Math.max(12, h - reservedBottom - 12 - estimateHeight()));
    }

    private double estimateHeight() {
        double h = 0;
        for (Notification n : notifications) {
            h += n.label.prefHeight(-1) + 8;
        }
        return Math.max(40, h);
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
        refreshLabels();
    }

    private void tick() {
        Iterator<Notification> iterator = notifications.iterator();
        boolean changed = false;
        while (iterator.hasNext()) {
            Notification notif = iterator.next();
            notif.updateOpacity();
            if (notif.isExpired()) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            refreshLabels();
        }
    }

    private void refreshLabels() {
        container.getChildren().clear();
        for (int i = notifications.size() - 1; i >= 0; i--) {
            container.getChildren().add(notifications.get(i).label);
        }
        if (container.getParent() instanceof Pane pane) {
            reposition(pane);
        }
    }

    public void setReservedBottom(double reservedBottom) {
        this.reservedBottom = Math.max(0, reservedBottom);
        if (container.getParent() instanceof Pane pane) {
            reposition(pane);
        }
    }

    public void setReservedRight(double reservedRight) {
        this.reservedRight = Math.max(0, reservedRight);
        if (container.getParent() instanceof Pane pane) {
            reposition(pane);
        }
    }
}
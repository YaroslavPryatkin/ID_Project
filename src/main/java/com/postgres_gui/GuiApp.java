package com.postgres_gui;

import com.postgres_gui.config.Config;
import com.postgres_gui.config.ScreenConfig;
import com.postgres_gui.ui.AppWindow;
import com.postgres_gui.ui.layout.UILayoutSettings;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class GuiApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Image icon = loadIcon();
        if (icon != null) {
            stage.getIcons().add(icon);
        }

        Config config = loadConfig();
        AppWindow appWindow = new AppWindow(config, icon);

        Scene scene = new Scene(
                appWindow.getRoot(),
                UILayoutSettings.WINDOW_START_WIDTH,
                UILayoutSettings.WINDOW_START_HEIGHT
        );
        appWindow.bindRootSize(UILayoutSettings.WINDOW_START_WIDTH, UILayoutSettings.WINDOW_START_HEIGHT);

        stage.setTitle("PostgreSQL Function GUI");
        stage.setScene(scene);
        stage.setMinWidth(UILayoutSettings.WINDOW_MIN_WIDTH);
        stage.setMinHeight(UILayoutSettings.WINDOW_MIN_HEIGHT);
        stage.show();

        appWindow.showStartScreen();
    }

    private Image loadIcon() {
        try (InputStream stream = getClass().getResourceAsStream("/icon.png")) {
            if (stream != null) {
                return new Image(stream);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Config loadConfig() throws IOException {
        Path configPath = Path.of(Config.getConfigFilePath());
        if (!Files.exists(configPath)) {
            Config defaults = createDefaultConfig();
            defaults.saveToFile();
            return defaults;
        }
        return Config.loadFromFile();
    }

    private Config createDefaultConfig() {
        Config config = new Config();
        config.setUrl("localhost:5432");
        config.setDatabase("postgres");
        config.setUser("postgres");
        config.setPassword("postgres");
        config.addScreen(new ScreenConfig("read_functions", "#4F709C", "Reading data"));
        config.addScreen(new ScreenConfig("insert_functions", "#5F8D4E", "Inserting new data"));
        config.addScreen(new ScreenConfig("remove_functions", "#A75D5D", "Removing data"));
        return config;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

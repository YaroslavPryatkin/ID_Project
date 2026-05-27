package com.postgres_gui;

import com.postgres_gui.config.Config;
import com.postgres_gui.config.ScreenConfig;
import com.postgres_gui.ui.AppWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class GuiApp extends Application {
    private static final int STARTING_WIDTH = 900;
    private static final int STARTING_HEIGHT = 700;
    private static final int MINIMAL_WIDTH = 800;
    private static final int MINIMAL_HEIGHT = 650;


    @Override
    public void start(Stage stage) throws IOException {
        InputStream str =  getClass().getResourceAsStream("/icon.png");
        Image icon = null;
        if(str!=null) {
            icon = new Image(str);
            stage.getIcons().add(icon);
        }


        Config config = loadConfig();
        AppWindow appWindow = new AppWindow(config, icon);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: " + ScreenConfig.backgroundColor + ";");

        Canvas canvas = new Canvas();
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());
        root.getChildren().add(canvas);

        appWindow.attachCanvas(canvas);

        Scene scene = new Scene(root, STARTING_WIDTH, STARTING_HEIGHT);
        stage.setTitle("PostgreSQL Function GUI");
        stage.setScene(scene);
        stage.setMinWidth(MINIMAL_WIDTH);
        stage.setMinHeight(MINIMAL_HEIGHT);
        stage.show();

        appWindow.showStartScreen();
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

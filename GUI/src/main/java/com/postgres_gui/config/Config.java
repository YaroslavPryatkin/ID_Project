package com.postgres_gui.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Config {
    private String url = "localhost:5432";
    private String database = "postgres";
    private String user = "";
    private String password = "";
    private final List<ScreenConfig> screens;
    private static final String CONFIG_FILE_NAME = "config.conf";

    public Config() {
        this.screens = new ArrayList<>();
    }

    public static Config loadFromFile() throws IOException {
        ConfigParser parser = new ConfigParser();
        return parser.parse(getConfigFilePath());
    }

    public static String getConfigFilePath(){
        try {
            String path = Config.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedPath = java.net.URLDecoder.decode(path, "UTF-8");
            File jarDir = new File(decodedPath).getParentFile();
            return new File(jarDir, CONFIG_FILE_NAME).getAbsolutePath();
        } catch (Exception e) {
            return CONFIG_FILE_NAME;
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<ScreenConfig> getScreens() {
        return screens;
    }

    public void addScreen(ScreenConfig screen) {
        screens.add(screen);
    }

    public ScreenConfig getScreenByPrefix(String prefix) {
        return screens.stream()
                .filter(s -> s.getPrefix().equals(prefix))
                .findFirst()
                .orElse(null);
    }

    public void saveToFile() throws IOException {
        ConfigWriter writer = new ConfigWriter();
        writer.write(this, getConfigFilePath());
    }
}

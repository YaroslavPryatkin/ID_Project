package com.postgres_gui.config;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class ConfigWriter {

    public void write(Config config, String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("url: " + config.getUrl() + "\n");
            writer.write("database: " + config.getDatabase() + "\n");
            writer.write("user: " + config.getUser() + "\n");
            writer.write("password: " + config.getPassword() + "\n");

            writer.write("\n#Function screens\n");
            for (ScreenConfig screen : config.getScreens()) {
                writer.write("screen: " + screen.getPrefix() + " "
                        + screen.getColor() + " "
                        + screen.getDisplayName() + "\n");

                for (Map.Entry<String, String> mapping : screen.getFunctionNameMappings().entrySet()) {
                    writer.write("fun: " + mapping.getKey() + " " + mapping.getValue() + "\n");
                }
            }

            writer.flush();
        }
    }
}

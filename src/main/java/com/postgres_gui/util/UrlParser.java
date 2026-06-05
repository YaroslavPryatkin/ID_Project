package com.postgres_gui.util;

public final class UrlParser {
    private final String host;
    private final String port;
    private final String database;

    public UrlParser(String hostPort, String database) {
        String url = hostPort == null ? "" : hostPort.trim();
        if (url.startsWith("jdbc:postgresql://")) {
            url = url.substring("jdbc:postgresql://".length());
        }

        int slash = url.indexOf('/');
        if (slash >= 0) {
            url = url.substring(0, slash);
        }

        int colon = url.indexOf(':');
        if (colon >= 0) {
            this.host = url.substring(0, colon);
            this.port = url.substring(colon + 1);
        } else {
            this.host = url.isEmpty() ? "localhost" : url;
            this.port = "5432";
        }

        String db = database == null ? "" : database.trim();
        this.database = db.isEmpty() ? "postgres" : db;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String toJdbcUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }
}

package com.postgres_gui.database;

import com.postgres_gui.util.UrlParser;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseManager {
    private Connection connection;
    private String jdbcUrl;
    private String user;
    private String password;
    private boolean connected;

    public DatabaseManager(String hostPort, String database, String user, String password) {
        UrlParser parser = new UrlParser(hostPort, database);
        this.jdbcUrl = parser.toJdbcUrl();
        this.user = user;
        this.password = password;
    }

    public void setCredentials(String hostPort, String database, String user, String password) {
        UrlParser parser = new UrlParser(hostPort, database);
        this.jdbcUrl = parser.toJdbcUrl();
        this.user = user;
        this.password = password;
    }

    public boolean connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            connection = DriverManager.getConnection(jdbcUrl, user, password);
            connected = true;
            return true;
        } catch (SQLException e) {
            connected = false;
            return false;
        }
    }

    public boolean isConnected() {
        try {
            return connected && connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
        connected = false;
    }

    public List<DatabaseFunctionInfo> getFunctionsByPrefix(String prefix) throws SQLException {
        List<DatabaseFunctionInfo> functions = new ArrayList<>();
        String query = """
                SELECT p.proname, p.proretset, p.pronargs
                FROM pg_proc p
                JOIN pg_namespace n ON n.oid = p.pronamespace
                WHERE n.nspname = 'public' AND p.proname LIKE ?
                ORDER BY p.proname
                """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, prefix + "_%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    functions.add(new DatabaseFunctionInfo(
                            rs.getString("proname"),
                            rs.getBoolean("proretset"),
                            rs.getInt("pronargs")
                    ));
                }
            }
        }
        return functions;
    }

    public List<FunctionParameter> getFunctionParameters(String functionName) throws SQLException {
        List<FunctionParameter> parameters = new ArrayList<>();
        String query = """
                SELECT pg_get_function_arguments(p.oid) AS args
                FROM pg_proc p
                JOIN pg_namespace n ON n.oid = p.pronamespace
                WHERE n.nspname = 'public' AND p.proname = ?
                LIMIT 1
                """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, functionName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String args = rs.getString("args");
                    if (args != null && !args.isBlank()) {
                        parameters.addAll(parseArguments(args));
                    }
                }
            }
        }
        return parameters;
    }

    private List<FunctionParameter> parseArguments(String args) {
        List<FunctionParameter> result = new ArrayList<>();
        for (String part : splitArguments(args)) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            Matcher matcher = Pattern.compile("^(?:(\\w+)\\s+)?([\\w\\s\\[\\]]+)$").matcher(part);
            if (matcher.find()) {
                String name = matcher.group(1);
                String type = matcher.group(2).trim();
                if (name == null || name.isBlank()) {
                    name = "param" + (result.size() + 1);
                }
                result.add(new FunctionParameter(name, type));
            } else {
                result.add(new FunctionParameter("param" + (result.size() + 1), part));
            }
        }
        return result;
    }

    private List<String> splitArguments(String args) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : args.toCharArray()) {
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }
        return parts;
    }

    public boolean returnsSet(String functionName) throws SQLException {
        String query = """
                SELECT p.proretset
                FROM pg_proc p
                JOIN pg_namespace n ON n.oid = p.pronamespace
                WHERE n.nspname = 'public' AND p.proname = ?
                LIMIT 1
                """;
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, functionName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("proretset");
                }
            }
        }
        return false;
    }

    public Object executeFunction(String functionName, List<Object> params) throws SQLException {
        boolean returnsSet = returnsSet(functionName);
        StringBuilder query = new StringBuilder(returnsSet ? "SELECT * FROM " : "SELECT ");
        query.append(functionName).append("(");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                query.append(", ");
            }
            query.append("?");
        }
        query.append(")");
        if (!returnsSet) {
            query.append(" AS result");
        }

        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            bindParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                if (returnsSet) {
                    return convertResultSetToTable(rs);
                }
                if (rs.next()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    if (meta.getColumnCount() == 1) {
                        return rs.getObject(1);
                    }
                    return convertResultSetToTable(rs);
                }
            }
        }
        return null;
    }

    private void bindParameters(PreparedStatement stmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            int index = i + 1;
            if (param == null) {
                stmt.setNull(index, Types.NULL);
            } else {
                stmt.setObject(index, param);
            }
        }
    }

    private List<Map<String, Object>> convertResultSetToTable(ResultSet rs) throws SQLException {
        List<Map<String, Object>> data = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(metaData.getColumnLabel(i), rs.getObject(i));
            }
            data.add(row);
        }
        return data;
    }

    public Connection getConnection() {
        return connection;
    }
}

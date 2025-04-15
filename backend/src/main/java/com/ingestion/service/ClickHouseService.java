package com.ingestion.service;

import com.ingestion.model.JoinCondition;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClickHouseService {
    private static final String URL = "jdbc:clickhouse://clickhouse:8123/default";
    private static final String USER = "default";
    private static final String PASSWORD = "";

    public Connection getConnection(String jwtToken) throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", USER);
        if (jwtToken != null && !jwtToken.isEmpty()) {
            properties.setProperty("password", jwtToken);
        } else if (!PASSWORD.isEmpty()) {
            properties.setProperty("password", PASSWORD);
        }
        try {
            Connection conn = DriverManager.getConnection(URL, properties);
            if (conn == null || conn.isClosed()) {
                throw new SQLException("Failed to establish ClickHouse connection");
            }
            return conn;
        } catch (SQLException e) {
            throw new SQLException("Cannot connect to ClickHouse at " + URL + ": " + e.getMessage(), e);
        }
    }

    public List<String> getTables() throws SQLException {
        try (Connection conn = getConnection(null);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
            List<String> tables = new ArrayList<>();
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
            return tables;
        }
    }

    public List<String> getColumns(String tableName) throws SQLException {
        try (Connection conn = getConnection(null);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE TABLE " + tableName)) {
            List<String> columns = new ArrayList<>();
            while (rs.next()) {
                columns.add(rs.getString("name"));
            }
            return columns;
        }
    }

    public Map<String, List<String>> getColumnsForMultipleTables(List<String> tables) throws SQLException {
        Map<String, List<String>> result = new HashMap<>();
        for (String table : tables) {
            result.put(table, getColumns(table));
        }
        return result;
    }

    public String buildQuery(String tableName, List<String> columns, List<JoinCondition> joinConditions) {
        // Sanitize column names for ClickHouse
        List<String> sanitizedColumns = columns.stream()
                .map(col -> {
                    if (col.matches("^[a-zA-Z0-9_]+$")) {
                        return col;
                    } else {
                        return "`" + col.replace("`", "``") + "`";
                    }
                })
                .collect(Collectors.toList());
        
        String columnsStr = String.join(",", sanitizedColumns);
        StringBuilder query = new StringBuilder("SELECT " + columnsStr + " FROM " + tableName);
        for (JoinCondition jc : joinConditions) {
            String joinTable = jc.getTable().matches("^[a-zA-Z0-9_]+$") ? 
                              jc.getTable() : "`" + jc.getTable().replace("`", "``") + "`";
            String joinKey = jc.getKey().matches("^[a-zA-Z0-9_]+$") ? 
                            jc.getKey() : "`" + jc.getKey().replace("`", "``") + "`";
            query.append(" JOIN ").append(joinTable)
                 .append(" ON ").append(tableName).append(".").append(joinKey)
                 .append("=").append(joinTable).append(".").append(joinKey);
        }
        return query.toString();
    }

    public long executeIngestion(String tableName, List<String> columns, String outputPath, 
                                List<JoinCondition> joinConditions) throws SQLException {
        String query = buildQuery(tableName, columns, joinConditions);
        try (Connection conn = getConnection(null);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(outputPath)) {
                // Write original column names as CSV header
                writer.println(String.join(",", columns));
                long count = 0;
                while (rs.next()) {
                    List<String> values = new ArrayList<>();
                    for (int i = 1; i <= columns.size(); i++) {
                        String value = rs.getString(i);
                        // Escape commas and quotes in values
                        if (value != null && (value.contains(",") || value.contains("\""))) {
                            value = "\"" + value.replace("\"", "\"\"") + "\"";
                        }
                        values.add(value != null ? value : "");
                    }
                    writer.println(String.join(",", values));
                    count++;
                }
                return count;
            } catch (java.io.IOException e) {
                throw new SQLException("Failed to write to output file: " + e.getMessage(), e);
            }
        }
    }
}

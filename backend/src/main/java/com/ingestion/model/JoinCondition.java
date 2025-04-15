package com.ingestion.model;

public class JoinCondition {
    private String table;
    private String key;

    public JoinCondition(String table, String key) {
        this.table = table;
        this.key = key;
    }

    // Getters and setters
    public String getTable() { return table; }
    public void setTable(String table) { this.table = table; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
}
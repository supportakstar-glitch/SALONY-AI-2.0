package com.akstarcompany.salony.ai.model;

public class MemoryItem {
    private long id;
    private String key;
    private String value;
    private String category;
    private long timestamp;

    public MemoryItem(long id, String key, String value, String category, long timestamp) {
        this.id = id;
        this.key = key;
        this.value = value;
        this.category = category;
        this.timestamp = timestamp;
    }

    public MemoryItem(String key, String value, String category) {
        this(0, key, value, category, System.currentTimeMillis());
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

package com.example.trackmate.models;

public class Notification {
    private String id;
    private String title;
    private String message;
    private String userId;
    private String itemId;
    private long timestamp;

    public Notification() {
    }

    public Notification(String userId, String title, String message, String itemId) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.itemId = itemId;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

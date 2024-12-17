package com.example.trackmate.models;

public class ReportedItem {
    public enum Status {
        OPEN, CLOSED, RESOLVED
    }

    public enum Type {
        LOST, FOUND
    }

    private String id;
    private String userId;
    private String name;
    private String description;
    private String location;
    private String date;
    private String imageUrl;
    private Type type;
    private Status status;
    private long timestamp;
    private String receiverId;

    // Default constructor for Firebase
    public ReportedItem() {}

    // Constructor with parameters
    public ReportedItem(String name, String description, String location, 
                       String date, String imageUrl, Type type) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.date = date;
        this.imageUrl = imageUrl;
        this.type = type;
        this.status = Status.OPEN;
        this.timestamp = System.currentTimeMillis();
    }


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isLost() {
        return Type.LOST.equals(type);
    }

    public void setId(String key) {
        this.id = key;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
}